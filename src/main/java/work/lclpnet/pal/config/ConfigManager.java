package work.lclpnet.pal.config;

import org.slf4j.Logger;
import work.lclpnet.config.json.ConfigHandler;
import work.lclpnet.config.json.FileConfigSerializer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ConfigManager implements ConfigAccess {

    private final ConfigHandler<PalConfig> handler;

    @Inject
    public ConfigManager(@Named("configPath") Path configPath, Logger logger) {
        var serializer = new FileConfigSerializer<>(PalConfig.FACTORY, logger);

        handler = new ConfigHandler<>(configPath, serializer, logger);
    }

    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(handler::loadConfig);
    }

    @Override
    public PalConfig getConfig() {
        return handler.getConfig();
    }
}
