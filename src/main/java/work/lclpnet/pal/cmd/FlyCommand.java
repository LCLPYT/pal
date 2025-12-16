package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import work.lclpnet.kibu.cmd.type.CommandRegistrar;
import work.lclpnet.kibu.cmd.type.KibuCommand;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.pal.service.CommandService;

import javax.inject.Inject;

import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class FlyCommand implements KibuCommand {

    private final CommandService commandService;

    @Inject
    public FlyCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("fly")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(this::flySelf)
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(this::fly));
    }

    private int flySelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();

        enableFlying(player);

        return 0;
    }

    private void enableFlying(ServerPlayer player) {
        Abilities abilities = player.getAbilities();

        abilities.mayfly = !abilities.mayfly;

        if (!abilities.mayfly && abilities.flying) {
            abilities.flying = false;
        }

        player.onUpdateAbilities();

        Translations translations = commandService.getTranslations();
        Component text;

        if (abilities.mayfly) {
            text = translations.translateText(player, "pal.cmd.fly.enabled").formatted(ChatFormatting.GREEN);
        } else {
            text = translations.translateText(player, "pal.cmd.fly.disabled").formatted(ChatFormatting.RED);
        }

        player.sendSystemMessage(text);
    }

    private int fly(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(ctx, "players");

        for (ServerPlayer player : players) {
            enableFlying(player);
        }

        CommandSourceStack source = ctx.getSource();
        RootText msg;

        int count = players.size();

        if (count == 1) {
            msg = commandService.translateText(source, "pal.cmd.fly.single", styled(players.iterator().next().getScoreboardName()).formatted(ChatFormatting.YELLOW));
        } else {
            msg = commandService.translateText(source, "pal.cmd.fly.multiple", styled(count).formatted(ChatFormatting.YELLOW));
        }

        source.sendSystemMessage(msg.formatted(ChatFormatting.GREEN));

        return count;
    }
}
