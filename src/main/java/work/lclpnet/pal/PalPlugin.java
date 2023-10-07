package work.lclpnet.pal;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.kibu.plugin.cmd.KibuCommand;
import work.lclpnet.kibu.plugin.ext.KibuPlugin;
import work.lclpnet.kibu.plugin.ext.TranslatedPlugin;
import work.lclpnet.kibu.plugin.hook.HookListenerModule;
import work.lclpnet.kibu.plugin.hook.TranslationsLoadedCallback;
import work.lclpnet.kibu.translate.TranslationService;
import work.lclpnet.mplugins.ext.WorldStateListener;
import work.lclpnet.pal.di.DaggerPalComponent;
import work.lclpnet.pal.di.PalComponent;
import work.lclpnet.pal.di.PalModule;
import work.lclpnet.translations.loader.translation.SPITranslationLoader;
import work.lclpnet.translations.loader.translation.TranslationLoader;

import java.util.concurrent.CompletableFuture;

public class PalPlugin extends KibuPlugin implements TranslatedPlugin, WorldStateListener {

    public static final String ID = "pal";
    private static final Logger logger = LoggerFactory.getLogger(ID);
    private TranslationService translationService = null;
    private PalComponent component = null;

    @Override
    public void loadKibuPlugin() {
        component = DaggerPalComponent.builder()
                .palModule(new PalModule(logger, translationService, getScheduler()))
                .build();

        final CompletableFuture<Void> translationsFuture = new CompletableFuture<>();

        registerHook(TranslationsLoadedCallback.HOOK, service -> {
            if (translationsFuture.isDone() || !service.getTranslator().hasTranslation("en_us", "pal.permissions.requires.living")) return;

            translationsFuture.complete(null);
        });

        CompletableFuture.allOf(
                translationsFuture,
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

        for (KibuCommand cmd : component.commands()) {
            cmd.register(this);
        }
    }

    @Override
    public void injectTranslationService(TranslationService translationService) {
        this.translationService = translationService;
    }

    @Override
    public TranslationLoader createTranslationLoader() {
        ClassLoader classLoader = getClass().getClassLoader();
        return new SPITranslationLoader(classLoader);
    }

    @Override
    public void onWorldReady() {
        MinecraftServer server = getEnvironment().getServer();
        component.commandService().setServer(server);
    }

    @Override
    public void onWorldUnready() {

    }

    public static Identifier identifier(String path) {
        return new Identifier(ID, path);
    }
}