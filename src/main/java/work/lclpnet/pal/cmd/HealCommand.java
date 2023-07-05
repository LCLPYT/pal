package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import work.lclpnet.kibu.plugin.cmd.CommandRegistrar;
import work.lclpnet.kibu.translate.TranslationService;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.pal.service.CommandService;

public class HealCommand {

    private final CommandService commandService;

    public HealCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<ServerCommandSource> command() {
        return CommandManager.literal("heal")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(this::healSelf)
                .then(CommandManager.argument("entities", EntityArgumentType.entities())
                        .executes(this::heal));
    }

    private int healSelf(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        Entity entity = source.getEntityOrThrow();

        if (!(entity instanceof LivingEntity living)) {
            throw commandService.createRequiresLivingException(source);
        }

        healLiving(living);

        return 0;
    }

    private void healLiving(LivingEntity living) {
        living.setHealth(living.getMaxHealth());

        if (!(living instanceof ServerPlayerEntity player)) return;  // no need to send messages to non-players

        TranslationService translationService = commandService.getTranslationService();
        player.sendMessage(translationService.translateText(player, "pal.cmd.heal.healed_you").formatted(Formatting.GREEN));
    }

    private int heal(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var entities = EntityArgumentType.getEntities(ctx, "entities").stream()
                .filter(Entity::isLiving)
                .toList();

        if (entities.isEmpty()) {
            throw EntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
        }

        for (Entity entity : entities) {
            healLiving((LivingEntity) entity);
        }

        ServerCommandSource source = ctx.getSource();
        RootText msg;

        int count = entities.size();

        if (count == 1) {
            msg = commandService.translateText(source, "pal.cmd.heal.single", entities.get(0).getDisplayName().getString());
        } else {
            msg = commandService.translateText(source, "pal.cmd.heal.multiple", count);
        }

        source.sendMessage(msg.formatted(Formatting.GREEN));

        return count;
    }
}
