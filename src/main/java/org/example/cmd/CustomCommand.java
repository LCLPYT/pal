package org.example.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import work.lclpnet.kibu.scheduler.Ticks;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CustomCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> create() {
        return literal("custom")
                .requires(source -> source.hasPermissionLevel(2))  // is op level 2
                .then(argument("entity", EntityArgumentType.entity())
                        .executes(CustomCommand::run));  // add glowing for 2 minutes to the specified entity
    }

    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var entity = EntityArgumentType.getEntity(context, "entity");

        if (entity instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, Ticks.minutes(2), 2));
        }

        context.getSource().sendMessage(Text.literal("Applied glowing effect to %s".formatted(entity.getName().getString())));
        return 1;
    }
}
