package work.lclpnet.pal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.kibu.access.PlayerLanguage;
import work.lclpnet.kibu.plugin.KibuPlugin;
import work.lclpnet.kibu.plugin.hook.HookListenerModule;
import work.lclpnet.kibu.translate.TranslationService;
import work.lclpnet.pal.cmd.PalCommand;
import work.lclpnet.pal.di.DaggerPalComponent;
import work.lclpnet.pal.di.PalComponent;
import work.lclpnet.pal.di.PalModule;
import work.lclpnet.translations.DefaultLanguageTranslator;
import work.lclpnet.translations.loader.translation.SPITranslationLoader;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class PalPlugin extends KibuPlugin {

    public static final String ID = "pal";
    private static final Logger logger = LoggerFactory.getLogger(ID);
    private PalComponent component = null;

    @Override
    public void loadKibuPlugin() {
        TranslationService translationService = createTranslationService();

        component = DaggerPalComponent.builder()
                .palModule(new PalModule(logger, translationService))
                .build();

        var translator = (DefaultLanguageTranslator) translationService.getTranslator();

        CompletableFuture.allOf(
                translator.reload(),
                component.configManager().init()
        ).exceptionally(err -> {
            // handle any errors and recover
            logger.error("Failed to initialize plugin 'pal'", err);
            return null;
        }).thenRun(this::onLoaded);
    }

    private void onLoaded() {
        for (HookListenerModule hookModule : component.hooks()) {
            registerHooks(hookModule);
        }

        for (PalCommand cmd : component.commands()) {
            cmd.register(this);
        }
    }

    private TranslationService createTranslationService() {
        ClassLoader classLoader = getClass().getClassLoader();
        SPITranslationLoader loader = new SPITranslationLoader(classLoader);
        DefaultLanguageTranslator translator = new DefaultLanguageTranslator(loader);

        return new TranslationService(translator, player -> Optional.of(PlayerLanguage.getLanguage(player)));
    }
}