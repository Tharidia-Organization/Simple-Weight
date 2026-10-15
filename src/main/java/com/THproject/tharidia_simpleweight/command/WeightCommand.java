package com.THproject.tharidia_simpleweight.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.THproject.tharidia_simpleweight.weight.WeightData;
import com.THproject.tharidia_simpleweight.weight.WeightManager;
import com.THproject.tharidia_simpleweight.weight.WeightRegistry;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Registers the /weight command:
 * - "testmode" toggles weight restrictions on the OP running it, so they can
 *   test weights without permanently dropping their permission level;
 * - "check [player]" shows a player's carried weight and status.
 */
public class WeightCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("weight")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("testmode")
                .executes(WeightCommand::toggleTestMode))
            .then(Commands.literal("check")
                .executes(context -> check(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> check(context.getSource(), EntityArgument.getPlayer(context, "player"))))));
    }

    private static int check(CommandSourceStack source, ServerPlayer player) {
        // Ignores the OP bypass so admins can inspect anyone (including themselves)
        double weight = WeightManager.calculateCarriedWeight(player);
        WeightData.WeightStatus status = WeightRegistry.getWeightStatus(weight);
        String text = player.getGameProfile().getName() + " carries "
            + WeightRegistry.getDisplayConfig().format(weight) + " (" + status.name().toLowerCase(java.util.Locale.ROOT) + ")"
            + (WeightManager.isMaster(player) ? " - exempt as OP" : "");
        source.sendSuccess(() -> Component.literal(text), false);
        return (int) Math.round(weight);
    }

    private static int toggleTestMode(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        boolean enabled = WeightManager.toggleTestMode(player);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            new com.THproject.tharidia_simpleweight.network.TestModeSyncPayload(enabled));
        if (enabled) {
            source.sendSuccess(() -> Component.literal(
                "Weight test mode ENABLED - you are no longer exempt from weight restrictions."), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                "Weight test mode DISABLED - you are exempt from weight restrictions again."), false);
        }
        return 1;
    }
}
