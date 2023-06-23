package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import work.lclpnet.kibu.plugin.cmd.CommandRegistrar;
import work.lclpnet.pal.service.CommandService;
import work.lclpnet.translations.Translator;

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

        if (!(living instanceof PlayerEntity player)) return;  // no need to send messages to non-players

        Translator translator = commandService.getTranslator();
        String msg = translator.translate(commandService.getLocaleOf(player), "pal.cmd.heal.healed_you");

        living.sendMessage(Text.literal(msg).formatted(Formatting.GREEN));
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

        Translator translator = commandService.getTranslator();
        ServerCommandSource source = ctx.getSource();
        String msg;

        int count = entities.size();

        if (count == 1) {
            msg = translator.translate(commandService.getLocaleOf(source), "pal.cmd.heal.single", entities.get(0).getDisplayName().getString());
        } else {
            msg = translator.translate(commandService.getLocaleOf(source), "pal.cmd.heal.multiple", count);
        }

        source.sendMessage(Text.literal(msg).formatted(Formatting.GREEN));

        return count;
    }
}
