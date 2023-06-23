package work.lclpnet.pal.service;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import work.lclpnet.kibu.access.PlayerLanguage;
import work.lclpnet.pal.cmd.TranslatedCommandExceptionType;
import work.lclpnet.translations.Translator;

import javax.annotation.Nonnull;

public class CommandService {

    private final Translator translator;
    private final TranslatedCommandExceptionType requiresLivingException;

    public CommandService(Translator translator) {
        this.translator = translator;
        this.requiresLivingException = new TranslatedCommandExceptionType(translator, "pal.permissions.requires.living");
    }

    @Nonnull
    public CommandSyntaxException createRequiresLivingException(ServerCommandSource source) {
        return requiresLivingException.create(getLocaleOf(source));
    }

    @Nonnull
    public String getLocaleOf(ServerCommandSource source) {
        Entity entity = source.getEntity();

        if (entity != null) {
            return getLocaleOf(entity);
        }

        return "en_us";
    }

    @Nonnull
    public String getLocaleOf(Entity entity) {
        if (entity instanceof ServerPlayerEntity serverPlayer) {
            return PlayerLanguage.getLanguage(serverPlayer);
        }

        return "en_us";
    }

    public Translator getTranslator() {
        return translator;
    }
}
