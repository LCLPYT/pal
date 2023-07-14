package work.lclpnet.pal.service;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import work.lclpnet.kibu.translate.TranslationService;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.pal.cmd.TranslatedCommandExceptionType;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CommandService {

    private final TranslationService translationService;
    private final TranslatedCommandExceptionType requiresLivingException;

    @Inject
    public CommandService(TranslationService translationService) {
        this.translationService = translationService;
        this.requiresLivingException = new TranslatedCommandExceptionType("pal.permissions.requires.living");
    }

    @Nonnull
    public CommandSyntaxException createRequiresLivingException(ServerCommandSource source) {
        return requiresLivingException.create(key -> translateText(source, key));
    }

    public TranslationService getTranslationService() {
        return translationService;
    }

    public RootText translateText(ServerCommandSource source, String key, Object... arguments) {
        ServerPlayerEntity player = source.getPlayer();

        if (player != null) {
            return translationService.translateText(player, key, arguments);
        }

        return translationService.translateText("en_us", key, arguments);
    }
}
