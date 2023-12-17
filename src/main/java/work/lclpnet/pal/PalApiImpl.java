package work.lclpnet.pal;

import work.lclpnet.pal.config.ConfigManager;
import work.lclpnet.pal.config.PalConfig;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.Consumer;

@Singleton
public class PalApiImpl implements PalApi {

    private static volatile PalApiImpl instance = null;
    private final ConfigManager configManager;

    @Inject
    public PalApiImpl(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void editConfig(Consumer<PalConfig> action) {
        action.accept(configManager.getConfig());
        configManager.save();  // async
    }

    static PalApiImpl getInstance() {
        if (instance == null) throw new IllegalStateException("Pal not initialized");
        return instance;
    }

    static void setInstance(PalApiImpl instance) {
        PalApiImpl.instance = instance;
    }
}
