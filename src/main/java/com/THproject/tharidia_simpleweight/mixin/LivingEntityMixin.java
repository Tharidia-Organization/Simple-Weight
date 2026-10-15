package com.THproject.tharidia_simpleweight.mixin;

import com.THproject.tharidia_simpleweight.event.WeightDebuffHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * Player.travel() adds the swim-up push and then calls this method, so
     * clamping here (before the entity moves) catches every source of upward
     * movement in water. A tick event cannot: it runs before aiStep() adds the
     * jump-in-water boost, so the player still rose every tick.
     */
    @Inject(method = "travel", at = @At("HEAD"))
    private void tharidia_simpleweight$limitSwimUp(Vec3 travelVector, CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            WeightDebuffHandler.limitSwimUp(player);
        }
    }
}
