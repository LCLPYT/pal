package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import work.lclpnet.kibu.plugin.cmd.CommandRegistrar;
import work.lclpnet.kibu.translate.TranslationService;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.pal.service.CommandService;

import javax.inject.Inject;

import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class HealCommand implements PalCommand {

    private final CommandService commandService;

    @Inject
    public HealCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
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

        if (!(living instanceof ServerPlayerEntity player)) return;

        HungerManager hungerManager = player.getHungerManager();

        hungerManager.setFoodLevel(20);
        hungerManager.setSaturationLevel(5f);

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
            msg = commandService.translateText(source, "pal.cmd.heal.single", styled(entities.get(0).getDisplayName().getString()).formatted(Formatting.YELLOW));
        } else {
            msg = commandService.translateText(source, "pal.cmd.heal.multiple", styled(count).formatted(Formatting.YELLOW));
        }

        source.sendMessage(msg.formatted(Formatting.GREEN));

        return count;
    }
}
