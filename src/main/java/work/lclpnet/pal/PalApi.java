package work.lclpnet.pal;

import work.lclpnet.pal.config.PalConfig;

import java.util.function.Consumer;

public interface PalApi {

    void editConfig(Consumer<PalConfig> action);

    static PalApi getInstance() {
        return PalApiImpl.getInstance();
    }
}
