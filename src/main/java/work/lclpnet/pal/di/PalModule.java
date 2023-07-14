package work.lclpnet.pal.di;

import dagger.Module;
import dagger.Provides;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import work.lclpnet.kibu.translate.TranslationService;
import work.lclpnet.pal.PalPlugin;
import work.lclpnet.pal.config.ConfigAccess;
import work.lclpnet.pal.config.ConfigManager;

import javax.inject.Named;
import java.nio.file.Path;

@Module
public class PalModule {

    private final Logger logger;
    private final TranslationService translationService;

    public PalModule(Logger logger, TranslationService translationService) {
        this.logger = logger;
        this.translationService = translationService;
    }

    @Provides
    Logger provideLogger() {
        return logger;
    }

    @Provides
    TranslationService provideTranslationService() {
        return translationService;
    }

    @Provides
    @Named("configPath")
    Path provideConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(PalPlugin.ID).resolve("config.json");
    }

    @Provides
    ConfigAccess provideConfigAccess(ConfigManager configManager) {
        return configManager;
    }
}
