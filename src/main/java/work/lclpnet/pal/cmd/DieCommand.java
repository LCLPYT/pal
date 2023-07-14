package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Formatting;
import work.lclpnet.kibu.plugin.cmd.CommandRegistrar;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.pal.service.CommandService;

import javax.inject.Inject;

import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class DieCommand implements PalCommand {

    private final CommandService commandService;

    @Inject
    public DieCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<ServerCommandSource> command() {
        return CommandManager.literal("die")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(this::dieSelf)
                .then(CommandManager.argument("entities", EntityArgumentType.entities())
                        .executes(this::die));
    }

    private int dieSelf(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        Entity entity = source.getEntityOrThrow();

        if (!(entity instanceof LivingEntity living)) {
            throw commandService.createRequiresLivingException(source);
        }

        dieEntity(living);

        RootText message = commandService.translateText(source, "pal.cmd.die.single", styled(living.getDisplayName().getString()).formatted(Formatting.YELLOW));
        source.sendMessage(message.formatted(Formatting.GREEN));

        return 0;
    }

    private void dieEntity(LivingEntity living) {
        living.setHealth(0);
    }

    private int die(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var entities = EntityArgumentType.getEntities(ctx, "entities").stream()
                .filter(Entity::isLiving)
                .toList();

        if (entities.isEmpty()) {
            throw EntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
        }

        for (Entity entity : entities) {
            dieEntity((LivingEntity) entity);
        }

        ServerCommandSource source = ctx.getSource();
        RootText msg;

        int count = entities.size();

        if (count == 1) {
            msg = commandService.translateText(source, "pal.cmd.die.single", styled(entities.get(0).getDisplayName().getString()).formatted(Formatting.YELLOW));
        } else {
            msg = commandService.translateText(source, "pal.cmd.die.multiple", styled(count).formatted(Formatting.YELLOW));
        }

        source.sendMessage(msg.formatted(Formatting.GREEN));

        return count;
    }
}
