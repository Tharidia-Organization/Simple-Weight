package com.THproject.tharidia_simpleweight.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.THproject.tharidia_simpleweight.weight.WeightManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Registers the /weight command.
 * Currently provides a "testmode" toggle for OPs so they can test weight
 * restrictions on themselves without permanently dropping their permission level.
 */
public class WeightCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("weight")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("testmode")
                .executes(WeightCommand::toggleTestMode)));
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
