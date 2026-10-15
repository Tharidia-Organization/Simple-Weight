package com.THproject.tharidia_simpleweight.weight;

import com.THproject.tharidia_simpleweight.TharidiaSimpleWeight;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Weight of things stored inside other things: item containers (backpacks,
 * shulker boxes, bundles, Supplementaries quivers), Simply Tents packed tents
 * and Carry On carried blocks/entities. Optional mods are reached via
 * reflection, so none of them is a hard dependency.
 */
public final class ContainerWeights {
    // Guards against pathological nesting (and recursive containers)
    private static final int MAX_DEPTH = 8;

    private ContainerWeights() {}

    /**
     * Weight of everything stored inside a stack (not the stack itself).
     */
    public static double contentsWeight(ItemStack stack, HolderLookup.Provider registries, int depth) {
        if (stack.isEmpty() || depth >= MAX_DEPTH) {
            return 0.0;
        }

        // Items exposing the NeoForge item handler capability (Sophisticated
        // Backpacks, L2 Backpack, shulker boxes...). If present it is the
        // authoritative view of the contents, so components are not read too.
        IItemHandler handler = stack.getCapability(Capabilities.ItemHandler.ITEM);
        if (handler != null) {
            double weight = 0.0;
            for (int i = 0; i < handler.getSlots(); i++) {
                weight += nestedStackWeight(handler.getStackInSlot(i), registries, depth);
            }
            return weight;
        }

        double weight = 0.0;

        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null) {
            for (ItemStack contained : container.nonEmptyItems()) {
                weight += nestedStackWeight(contained, registries, depth);
            }
        }

        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            for (ItemStack contained : bundle.items()) {
                weight += nestedStackWeight(contained, registries, depth);
            }
        }

        // Supplementaries quiver (and its other selectable containers)
        if (SupplementariesCompat.isAvailable()) {
            for (TypedDataComponent<?> component : stack.getComponents()) {
                List<ItemStack> contents = SupplementariesCompat.contents(component.value());
                if (contents != null) {
                    for (ItemStack contained : contents) {
                        weight += nestedStackWeight(contained, registries, depth);
                    }
                }
            }
        }

        // Simply Tents: a packed tent stores its interior blocks and entities in custom data
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && (customData.contains("InteriorData") || customData.contains("EntityData"))) {
            weight += packedTentWeight(customData.copyTag(), registries, depth);
        }

        return weight;
    }

    private static double nestedStackWeight(ItemStack stack, HolderLookup.Provider registries, int depth) {
        if (stack.isEmpty()) {
            return 0.0;
        }
        return WeightRegistry.getItemWeight(stack.getItem()) * stack.getCount()
            + contentsWeight(stack, registries, depth + 1);
    }

    private static double packedTentWeight(CompoundTag tag, HolderLookup.Provider registries, int depth) {
        double weight = 0.0;
        for (Tag entry : tag.getList("InteriorData", Tag.TAG_COMPOUND)) {
            CompoundTag block = (CompoundTag) entry;
            weight += blockStateWeight(block.getCompound("State"));
            weight += blockEntityContentsWeight(block.getCompound("NBT"), registries, depth);
        }
        for (Tag entry : tag.getList("EntityData", Tag.TAG_COMPOUND)) {
            weight += entityWeight((CompoundTag) entry);
        }
        return weight;
    }

    /**
     * Weight of the item form of a block saved with NbtUtils.writeBlockState ({Name: "ns:id", ...}).
     */
    private static double blockStateWeight(CompoundTag stateTag) {
        ResourceLocation blockId = ResourceLocation.tryParse(stateTag.getString("Name"));
        if (blockId == null) {
            return 0.0;
        }
        Item item = BuiltInRegistries.BLOCK.get(blockId).asItem();
        return item == Items.AIR ? 0.0 : WeightRegistry.getItemWeight(item);
    }

    /**
     * Weight of the items saved in a block entity tag (chests, barrels, furnaces... all use "Items").
     */
    private static double blockEntityContentsWeight(CompoundTag blockEntityTag, HolderLookup.Provider registries, int depth) {
        double weight = 0.0;
        ListTag items = blockEntityTag.getList("Items", Tag.TAG_COMPOUND);
        for (Tag itemTag : items) {
            weight += nestedStackWeight(ItemStack.parseOptional(registries, (CompoundTag) itemTag), registries, depth);
        }
        return weight;
    }

    /**
     * Weight of a saved entity ({id: "ns:type", ...}) from the entity_weights config.
     */
    private static double entityWeight(CompoundTag entityTag) {
        ResourceLocation typeId = ResourceLocation.tryParse(entityTag.getString("id"));
        return typeId == null ? 0.0 : WeightRegistry.getEntityWeight(typeId);
    }

    /**
     * Weight of the block or entity the player is carrying with Carry On.
     */
    public static double carryOnWeight(Player player, HolderLookup.Provider registries) {
        CompoundTag carried = CarryOnCompat.carriedData(player);
        if (carried == null) {
            return 0.0;
        }
        return switch (carried.getString("type")) {
            case "BLOCK" -> blockStateWeight(carried.getCompound("block"))
                + blockEntityContentsWeight(carried.getCompound("tile"), registries, 0);
            case "ENTITY" -> entityWeight(carried.getCompound("entity"));
            case "PLAYER" -> WeightRegistry.getEntityWeight(ResourceLocation.withDefaultNamespace("player"));
            default -> 0.0;
        };
    }

    /**
     * Supplementaries stores quiver contents in a SelectableContainerContent data component.
     */
    private static final class SupplementariesCompat {
        private static final Class<?> CONTENT_CLASS;
        private static final Method GET_CONTENT;

        static {
            Class<?> contentClass = null;
            Method getContent = null;
            try {
                contentClass = Class.forName("net.mehvahdjukaar.supplementaries.common.items.components.SelectableContainerContent");
                getContent = contentClass.getMethod("getContentUnsafe");
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Supplementaries not installed
                contentClass = null;
            }
            CONTENT_CLASS = contentClass;
            GET_CONTENT = getContent;
        }

        static boolean isAvailable() {
            return CONTENT_CLASS != null;
        }

        @SuppressWarnings("unchecked")
        static List<ItemStack> contents(Object componentValue) {
            if (!CONTENT_CLASS.isInstance(componentValue)) {
                return null;
            }
            try {
                return (List<ItemStack>) GET_CONTENT.invoke(componentValue);
            } catch (ReflectiveOperationException | ClassCastException e) {
                TharidiaSimpleWeight.LOGGER.debug("Could not read Supplementaries container contents", e);
                return null;
            }
        }
    }

    /**
     * Carry On keeps what the player carries in CarryOnData (type + block/tile/entity NBT).
     */
    private static final class CarryOnCompat {
        private static final Method GET_CARRY_DATA;
        private static final Method IS_CARRYING;
        private static final Method GET_NBT;
        private static boolean errorLogged = false;

        static {
            Method getCarryData = null;
            Method isCarrying = null;
            Method getNbt = null;
            try {
                Class<?> manager = Class.forName("tschipp.carryon.common.carry.CarryOnDataManager");
                Class<?> data = Class.forName("tschipp.carryon.common.carry.CarryOnData");
                getCarryData = manager.getMethod("getCarryData", Player.class);
                isCarrying = data.getMethod("isCarrying");
                getNbt = data.getMethod("getNbt");
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Carry On not installed
                getCarryData = null;
            }
            GET_CARRY_DATA = getCarryData;
            IS_CARRYING = isCarrying;
            GET_NBT = getNbt;
        }

        static CompoundTag carriedData(Player player) {
            if (GET_CARRY_DATA == null) {
                return null;
            }
            try {
                Object data = GET_CARRY_DATA.invoke(null, player);
                if (data == null || !(Boolean) IS_CARRYING.invoke(data)) {
                    return null;
                }
                return (CompoundTag) GET_NBT.invoke(data);
            } catch (ReflectiveOperationException | RuntimeException e) {
                if (!errorLogged) {
                    errorLogged = true;
                    TharidiaSimpleWeight.LOGGER.error("Error reading Carry On data, carried weight will be ignored", e);
                }
                return null;
            }
        }
    }
}
