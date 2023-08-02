package work.lclpnet.pal.cmd;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import work.lclpnet.kibu.plugin.cmd.CommandRegistrar;
import work.lclpnet.kibu.plugin.cmd.KibuCommand;
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

    private LiteralArgumentBuilder<ServerCommandSource> command() {
        return CommandManager.literal("speed")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("speed", FloatArgumentType.floatArg(-1f, 5))
                                .executes(this::modifySpeedSelf)
                                .then(CommandManager.literal("walk")
                                        .executes(ctx -> modifySpeedSelf(ctx, TYPE_WALK))
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .executes(ctx -> modifySpeedOther(ctx, TYPE_WALK))))
                                .then(CommandManager.literal("fly")
                                        .executes(ctx -> modifySpeedSelf(ctx, TYPE_FLY))
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .executes(ctx -> modifySpeedOther(ctx, TYPE_FLY))))))
                .then(CommandManager.literal("reset")
                        .executes(this::resetSpeedSelf)
                        .then(CommandManager.literal("all")
                                .executes(ctx -> resetSpeedSelf(ctx, TYPE_BOTH))
                                .then(CommandManager.argument("players", EntityArgumentType.players())
                                        .executes(ctx -> resetSpeedOther(ctx, TYPE_BOTH))))
                        .then(CommandManager.literal("walk")
                                .executes(ctx -> resetSpeedSelf(ctx, TYPE_WALK))
                                .then(CommandManager.argument("players", EntityArgumentType.players())
                                        .executes(ctx -> resetSpeedOther(ctx, TYPE_WALK))))
                        .then(CommandManager.literal("fly")
                                .executes(ctx -> resetSpeedSelf(ctx, TYPE_FLY))
                                .then(CommandManager.argument("players", EntityArgumentType.players())
                                        .executes(ctx -> resetSpeedOther(ctx, TYPE_FLY)))));
    }

    private int modifySpeedSelf(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return modifySpeedSelf(ctx, getType(ctx));
    }

    private int modifySpeedSelf(CommandContext<ServerCommandSource> ctx, int type) throws CommandSyntaxException {
        float value = FloatArgumentType.getFloat(ctx, "speed");
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();

        return modifySpeedOf(ctx, type, List.of(player), value);
    }

    private int modifySpeedOther(CommandContext<ServerCommandSource> ctx, int type) throws CommandSyntaxException {
        var players = EntityArgumentType.getPlayers(ctx, "players");
        float value = FloatArgumentType.getFloat(ctx, "speed");

        return modifySpeedOf(ctx, type, players, value);
    }

    private int modifySpeedOf(CommandContext<ServerCommandSource> ctx, int type, Collection<ServerPlayerEntity> players, float value) {
        boolean walk = (type & TYPE_WALK) == TYPE_WALK;
        boolean fly = (type & TYPE_FLY) == TYPE_FLY;

        for (ServerPlayerEntity player : players) {
            PlayerAbilities abilities = player.getAbilities();

            if (walk) {
                abilities.setWalkSpeed(value);
                EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);

                if (attribute != null) {
                    attribute.setBaseValue(value);
                }
            }

            if (fly) {
                abilities.setFlySpeed(value);
            }

            player.sendAbilitiesUpdate();
        }

        ServerCommandSource source = ctx.getSource();

        return sendModifiedMessage(players, value, walk, source);
    }

    private int sendModifiedMessage(Collection<ServerPlayerEntity> players, float value, boolean walk, ServerCommandSource source) {
        RootText msg;

        final int count = players.size();
        final String typeStr = walk ? "walk" : "fly";

        if (count == 1) {
            msg = commandService.translateText(source, "pal.cmd.speed.%s.set.single".formatted(typeStr),
                    styled(players.iterator().next().getDisplayName().getString()).formatted(Formatting.YELLOW),
                    styled(value).formatted(Formatting.YELLOW));
        } else {
            msg = commandService.translateText(source, "pal.cmd.speed.%s.set.multiple".formatted(typeStr),
                    styled(count).formatted(Formatting.YELLOW),
                    styled(value).formatted(Formatting.YELLOW));
        }

        source.sendMessage(msg.formatted(Formatting.GREEN));

        return count;
    }

    private int resetSpeedSelf(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        return resetSpeedSelf(ctx, getType(ctx));
    }

    private int resetSpeedSelf(CommandContext<ServerCommandSource> ctx, int type) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        return resetSpeedOf(ctx, type, List.of(player));
    }

    private int resetSpeedOther(CommandContext<ServerCommandSource> ctx, int type) throws CommandSyntaxException {
        var players = EntityArgumentType.getPlayers(ctx, "players");

        return resetSpeedOf(ctx, type, players);
    }

    private int resetSpeedOf(CommandContext<ServerCommandSource> ctx, int type, Collection<ServerPlayerEntity> players) {
        boolean walk = (type & TYPE_WALK) == TYPE_WALK;
        boolean fly = (type & TYPE_FLY) == TYPE_FLY;

        for (ServerPlayerEntity player : players) {
            PlayerAbilities abilities = player.getAbilities();

            if (walk) {
                abilities.setWalkSpeed(0.1f);

                EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);

                if (attribute != null) {
                    attribute.setBaseValue(0.1f);
                }
            }

            if (fly) {
                abilities.setFlySpeed(0.05f);
            }

            player.sendAbilitiesUpdate();
        }

        final ServerCommandSource source = ctx.getSource();

        if ((type & TYPE_BOTH) == TYPE_BOTH) {
            RootText msg;

            final int count = players.size();

            if (count == 1) {
                msg = commandService.translateText(source, "pal.cmd.speed.all.reset.single",
                        styled(players.iterator().next().getDisplayName().getString()).formatted(Formatting.YELLOW));
            } else {
                msg = commandService.translateText(source, "pal.cmd.speed.all.reset.multiple",
                        styled(count).formatted(Formatting.YELLOW));
            }

            source.sendMessage(msg.formatted(Formatting.GREEN));

            return count;
        }

        return sendModifiedMessage(players, walk ? 0.1f : 0.05f, walk, source);
    }

    private static int getType(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        return player.getAbilities().flying ? TYPE_FLY : TYPE_WALK;
    }
}
