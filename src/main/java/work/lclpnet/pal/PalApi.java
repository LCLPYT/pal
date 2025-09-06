package work.lclpnet.pal;

import work.lclpnet.pal.config.PalConfig;
import work.lclpnet.pal.util.ContraptionService;

import java.util.function.Consumer;

public interface PalApi {

    void editConfig(Consumer<PalConfig> action);

    ContraptionService getContraptionService();

    static PalApi getInstance() {
        return PalApiImpl.getInstance();
    }
}
