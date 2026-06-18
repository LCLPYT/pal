package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.food.FoodData;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import work.lclpnet.kibu.cmd.type.CommandRegistrar;
import work.lclpnet.kibu.cmd.type.KibuCommand;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.pal.service.CommandService;

import javax.inject.Inject;

import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class FeedCommand implements KibuCommand {

    private final CommandService commandService;

    @Inject
    public FeedCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("feed")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(this::feedSelf)
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(this::feed));
    }

    private int feedSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();

        feedPlayer(player);

        return 0;
    }

    private void feedPlayer(ServerPlayer player) {
        FoodData hungerManager = player.getFoodData();

        hungerManager.setFoodLevel(20);
        hungerManager.setSaturation(5f);

        Translations translations = commandService.getTranslations();
        player.sendSystemMessage(translations.translateText(player, "pal.cmd.feed.fed_you").withStyle(ChatFormatting.GREEN));
    }

    private int feed(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(ctx, "players");

        for (ServerPlayer player : players) {
            feedPlayer(player);
        }

        CommandSourceStack source = ctx.getSource();
        RootText msg;

        int count = players.size();

        if (count == 1) {
            msg = commandService.translateText(source, "pal.cmd.feed.single", styled(players.iterator().next().getScoreboardName()).formatted(ChatFormatting.YELLOW));
        } else {
            msg = commandService.translateText(source, "pal.cmd.feed.multiple", styled(count).formatted(ChatFormatting.YELLOW));
        }

        source.sendSystemMessage(msg.withStyle(ChatFormatting.GREEN));

        return count;
    }
}
