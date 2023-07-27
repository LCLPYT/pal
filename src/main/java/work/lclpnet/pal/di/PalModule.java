package work.lclpnet.pal.di;

import dagger.Module;
import dagger.Provides;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import work.lclpnet.kibu.scheduler.api.Scheduler;
import work.lclpnet.kibu.translate.TranslationService;
import work.lclpnet.pal.PalPlugin;
import work.lclpnet.pal.config.ConfigAccess;
import work.lclpnet.pal.config.ConfigManager;
import work.lclpnet.pal.config.PalConfig;

import javax.inject.Named;
import java.nio.file.Path;

@Module
public class PalModule {

    private final Logger logger;
    private final TranslationService translationService;
    private final Scheduler scheduler;

    public PalModule(Logger logger, TranslationService translationService, Scheduler scheduler) {
        this.logger = logger;
        this.translationService = translationService;
        this.scheduler = scheduler;
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

    @Provides
    PalConfig providePalConfig(ConfigAccess configAccess) {
        return configAccess.getConfig();
    }

    @Provides
    Scheduler provideScheduler() {
        return scheduler;
    }
}
