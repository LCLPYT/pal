package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

public class HealCommand implements KibuCommand {

    private final CommandService commandService;

    @Inject
    public HealCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("heal")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(this::healSelf)
                .then(Commands.argument("entities", EntityArgument.entities())
                        .executes(this::heal));
    }

    private int healSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Entity entity = source.getEntityOrException();

        if (!(entity instanceof LivingEntity living)) {
            throw commandService.createRequiresLivingException(source);
        }

        healLiving(living);

        return 0;
    }

    private void healLiving(LivingEntity living) {
        living.setHealth(living.getMaxHealth());

        if (!(living instanceof ServerPlayer player)) return;

        FoodData hungerManager = player.getFoodData();

        hungerManager.setFoodLevel(20);
        hungerManager.setSaturation(5f);

        Translations translations = commandService.getTranslations();
        player.sendSystemMessage(translations.translateText(player, "pal.cmd.heal.healed_you").withStyle(ChatFormatting.GREEN));
    }

    private int heal(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var entities = EntityArgument.getEntities(ctx, "entities").stream()
                .filter(Entity::showVehicleHealth)
                .toList();

        if (entities.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
        }

        for (Entity entity : entities) {
            healLiving((LivingEntity) entity);
        }

        CommandSourceStack source = ctx.getSource();
        RootText msg;

        int count = entities.size();

        if (count == 1) {
            msg = commandService.translateText(source, "pal.cmd.heal.single", styled(entities.getFirst().getScoreboardName()).formatted(ChatFormatting.YELLOW));
        } else {
            msg = commandService.translateText(source, "pal.cmd.heal.multiple", styled(count).formatted(ChatFormatting.YELLOW));
        }

        source.sendSystemMessage(msg.withStyle(ChatFormatting.GREEN));

        return count;
    }
}
