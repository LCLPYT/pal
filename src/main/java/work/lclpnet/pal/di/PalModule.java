package work.lclpnet.pal.di;

import dagger.Module;
import dagger.Provides;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import work.lclpnet.kibu.scheduler.api.Scheduler;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.pal.PalMod;
import work.lclpnet.pal.config.ConfigAccess;
import work.lclpnet.pal.config.ConfigManager;
import work.lclpnet.pal.config.PalConfig;

import javax.inject.Named;
import java.nio.file.Path;

@Module
public class PalModule {

    private final Logger logger;
    private final Translations translations;
    private final Scheduler scheduler;

    public PalModule(Logger logger, Translations translations, Scheduler scheduler) {
        this.logger = logger;
        this.translations = translations;
        this.scheduler = scheduler;
    }

    @Provides
    Logger provideLogger() {
        return logger;
    }

    @Provides
    Translations provideTranslations() {
        return translations;
    }

    @Provides
    @Named("configPath")
    Path provideConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(PalMod.ID).resolve("config.json");
    }

    @Provides
    @Named("imagesPath")
    Path provideImagesPath(@Named("configPath") Path configPath) {
        return configPath.resolveSibling("images");
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
