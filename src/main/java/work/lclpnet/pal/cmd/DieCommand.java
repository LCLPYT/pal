package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.ChatFormatting;
import work.lclpnet.kibu.cmd.type.CommandRegistrar;
import work.lclpnet.kibu.cmd.type.KibuCommand;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.pal.service.CommandService;

import javax.inject.Inject;

import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class DieCommand implements KibuCommand {

    private final CommandService commandService;

    @Inject
    public DieCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("die")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(this::dieSelf)
                .then(Commands.argument("entities", EntityArgument.entities())
                        .executes(this::die));
    }

    private int dieSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Entity entity = source.getEntityOrException();

        if (!(entity instanceof LivingEntity living)) {
            throw commandService.createRequiresLivingException(source);
        }

        dieEntity(living);

        RootText message = commandService.translateText(source, "pal.cmd.die.single", styled(living.getScoreboardName()).formatted(ChatFormatting.YELLOW));
        source.sendSystemMessage(message.formatted(ChatFormatting.GREEN));

        return 0;
    }

    private void dieEntity(LivingEntity living) {
        living.setHealth(0);
    }

    private int die(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var entities = EntityArgument.getEntities(ctx, "entities").stream()
                .filter(Entity::showVehicleHealth)
                .toList();

        if (entities.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
        }

        for (Entity entity : entities) {
            dieEntity((LivingEntity) entity);
        }

        CommandSourceStack source = ctx.getSource();
        RootText msg;

        int count = entities.size();

        if (count == 1) {
            msg = commandService.translateText(source, "pal.cmd.die.single", styled(entities.getFirst().getScoreboardName()).formatted(ChatFormatting.YELLOW));
        } else {
            msg = commandService.translateText(source, "pal.cmd.die.multiple", styled(count).formatted(ChatFormatting.YELLOW));
        }

        source.sendSystemMessage(msg.formatted(ChatFormatting.GREEN));

        return count;
    }
}
