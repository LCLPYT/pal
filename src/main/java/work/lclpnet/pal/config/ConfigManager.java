package work.lclpnet.pal.config;

import org.slf4j.Logger;
import work.lclpnet.config.json.ConfigHandler;
import work.lclpnet.config.json.FileConfigSerializer;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;

public class ConfigManager implements ConfigAccess {

    private final ConfigHandler<PalConfig> handler;

    public ConfigManager(Path configPath, Logger logger) {
        var serializer = new FileConfigSerializer<>(PalConfig.FACTORY, logger);

        handler = new ConfigHandler<>(configPath, serializer, logger);
    }

    public void init() {
        handler.loadConfig();
    }

    @Override
    public PalConfig getConfig() {
        return handler.getConfig();
    }
}
