package work.lclpnet.pal.cmd;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import work.lclpnet.kibu.cmd.type.CommandRegistrar;
import work.lclpnet.kibu.cmd.type.KibuCommand;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.pal.service.CommandService;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class SpeedCommand implements KibuCommand {

    private static final int TYPE_WALK = 0b01, TYPE_FLY = 0b10, TYPE_BOTH = TYPE_WALK | TYPE_FLY;

    private final CommandService commandService;

    @Inject
    public SpeedCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("speed")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("set")
                        .then(Commands.argument("speed", FloatArgumentType.floatArg(-1f, 5))
                                .executes(this::modifySpeedSelf)
                                .then(Commands.literal("walk")
                                        .executes(ctx -> modifySpeedSelf(ctx, TYPE_WALK))
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> modifySpeedOther(ctx, TYPE_WALK))))
                                .then(Commands.literal("fly")
                                        .executes(ctx -> modifySpeedSelf(ctx, TYPE_FLY))
                                        .then(Commands.argument("players", EntityArgument.players())
                                                .executes(ctx -> modifySpeedOther(ctx, TYPE_FLY))))))
                .then(Commands.literal("reset")
                        .executes(this::resetSpeedSelf)
                        .then(Commands.literal("all")
                                .executes(ctx -> resetSpeedSelf(ctx, TYPE_BOTH))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(ctx -> resetSpeedOther(ctx, TYPE_BOTH))))
                        .then(Commands.literal("walk")
                                .executes(ctx -> resetSpeedSelf(ctx, TYPE_WALK))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(ctx -> resetSpeedOther(ctx, TYPE_WALK))))
                        .then(Commands.literal("fly")
                                .executes(ctx -> resetSpeedSelf(ctx, TYPE_FLY))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(ctx -> resetSpeedOther(ctx, TYPE_FLY)))));
    }

    private int modifySpeedSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return modifySpeedSelf(ctx, getType(ctx));
    }

    private int modifySpeedSelf(CommandContext<CommandSourceStack> ctx, int type) throws CommandSyntaxException {
        float value = FloatArgumentType.getFloat(ctx, "speed");
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        return modifySpeedOf(ctx, type, List.of(player), value);
    }

    private int modifySpeedOther(CommandContext<CommandSourceStack> ctx, int type) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(ctx, "players");
        float value = FloatArgumentType.getFloat(ctx, "speed");

        return modifySpeedOf(ctx, type, players, value);
    }

    private int modifySpeedOf(CommandContext<CommandSourceStack> ctx, int type, Collection<ServerPlayer> players, float value) {
        boolean walk = (type & TYPE_WALK) == TYPE_WALK;
        boolean fly = (type & TYPE_FLY) == TYPE_FLY;

        for (ServerPlayer player : players) {
            Abilities abilities = player.getAbilities();

            if (walk) {
                abilities.setWalkingSpeed(value);
                AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);

                if (attribute != null) {
                    attribute.setBaseValue(value);
                }
            }

            if (fly) {
                abilities.setFlyingSpeed(value);
            }

            player.onUpdateAbilities();
        }

        CommandSourceStack source = ctx.getSource();

        return sendModifiedMessage(players, value, walk, source);
    }

    private int sendModifiedMessage(Collection<ServerPlayer> players, float value, boolean walk, CommandSourceStack source) {
        RootText msg;

        final int count = players.size();
        final String typeStr = walk ? "walk" : "fly";

        if (count == 1) {
            msg = commandService.translateText(source, "pal.cmd.speed.%s.set.single".formatted(typeStr),
                    styled(players.iterator().next().getScoreboardName()).formatted(ChatFormatting.YELLOW),
                    styled(value).formatted(ChatFormatting.YELLOW));
        } else {
            msg = commandService.translateText(source, "pal.cmd.speed.%s.set.multiple".formatted(typeStr),
                    styled(count).formatted(ChatFormatting.YELLOW),
                    styled(value).formatted(ChatFormatting.YELLOW));
        }

        source.sendSystemMessage(msg.formatted(ChatFormatting.GREEN));

        return count;
    }

    private int resetSpeedSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return resetSpeedSelf(ctx, getType(ctx));
    }

    private int resetSpeedSelf(CommandContext<CommandSourceStack> ctx, int type) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        return resetSpeedOf(ctx, type, List.of(player));
    }

    private int resetSpeedOther(CommandContext<CommandSourceStack> ctx, int type) throws CommandSyntaxException {
        var players = EntityArgument.getPlayers(ctx, "players");

        return resetSpeedOf(ctx, type, players);
    }

    private int resetSpeedOf(CommandContext<CommandSourceStack> ctx, int type, Collection<ServerPlayer> players) {
        boolean walk = (type & TYPE_WALK) == TYPE_WALK;
        boolean fly = (type & TYPE_FLY) == TYPE_FLY;

        for (ServerPlayer player : players) {
            Abilities abilities = player.getAbilities();

            if (walk) {
                abilities.setWalkingSpeed(0.1f);

                AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);

                if (attribute != null) {
                    attribute.setBaseValue(0.1f);
                }
            }

            if (fly) {
                abilities.setFlyingSpeed(0.05f);
            }

            player.onUpdateAbilities();
        }

        final CommandSourceStack source = ctx.getSource();

        if ((type & TYPE_BOTH) == TYPE_BOTH) {
            RootText msg;

            final int count = players.size();

            if (count == 1) {
                msg = commandService.translateText(source, "pal.cmd.speed.all.reset.single",
                        styled(players.iterator().next().getScoreboardName()).formatted(ChatFormatting.YELLOW));
            } else {
                msg = commandService.translateText(source, "pal.cmd.speed.all.reset.multiple",
                        styled(count).formatted(ChatFormatting.YELLOW));
            }

            source.sendSystemMessage(msg.formatted(ChatFormatting.GREEN));

            return count;
        }

        return sendModifiedMessage(players, walk ? 0.1f : 0.05f, walk, source);
    }

    private static int getType(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        return player.getAbilities().flying ? TYPE_FLY : TYPE_WALK;
    }
}
