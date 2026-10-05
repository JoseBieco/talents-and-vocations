package com.seunome.vanillatalents.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.seunome.vanillatalents.capability.SkillAccess;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** {@code /vt debug ...}: comandos de teste manual, registrados só fora de produção. */
public final class DebugCommands {

    private DebugCommands() {}

    public static void register() {
        if (!FMLEnvironment.production) {
            RegisterCommandsEvent.BUS.addListener(event -> register(event.getDispatcher()));
        }
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vt").then(Commands.literal("debug")
                .then(Commands.literal("convert").executes(ctx ->
                        reply(ctx, "convert -> " + TalentActions.convertXp(ctx.getSource().getPlayerOrException(), false))))
                .then(Commands.literal("convertall").executes(ctx ->
                        reply(ctx, "convertall -> " + TalentActions.convertXp(ctx.getSource().getPlayerOrException(), true))))
                .then(Commands.literal("buy").then(Commands.argument("id", StringArgumentType.word()).executes(ctx ->
                        reply(ctx, "buy -> " + TalentActions.buyNode(ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "id"))))))
                .then(Commands.literal("class").then(Commands.argument("id", StringArgumentType.word()).executes(ctx ->
                        reply(ctx, "class -> " + TalentActions.changeClass(ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "id"))))))
                .then(Commands.literal("info").executes(ctx -> reply(ctx, "ok")))));
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, String result) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String state = SkillAccess.get(player)
                .map(d -> "class=" + d.getCurrentClass() + " pt=" + d.getAvailablePoints() + " nodes=" + d.getUnlockedNodes())
                .orElse("sem capability");
        ctx.getSource().sendSuccess(() -> Component.literal(result + " | lvl=" + player.experienceLevel + " | " + state), false);
        return 1;
    }
}
