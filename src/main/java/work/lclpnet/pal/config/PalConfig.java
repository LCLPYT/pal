package work.lclpnet.pal.config;

import org.json.JSONObject;
import work.lclpnet.config.json.JsonConfig;
import work.lclpnet.config.json.JsonConfigFactory;

public class PalConfig implements JsonConfig {

    public boolean enablePlates = false, enablePads = false, enableElevators = false, enableTeleporters = false;
    public boolean padLegacyAmount = false;
    public float plateMotionY = 1f, plateStrength = 2f;

    public PalConfig() {}

    public PalConfig(JSONObject json) {
        if (json.has("world")) {
            JSONObject world = json.getJSONObject("world");

            if (world.has("plates")) {
                JSONObject plates = world.getJSONObject("plates");

                if (plates.has("enabled")) {
                    enablePlates = plates.getBoolean("enabled");
                }

                if (plates.has("strength")) {
                    plateStrength = plates.getFloat("strength");
                }

                if (plates.has("motion_y")) {
                    plateMotionY = plates.getFloat("motion_y");
                }
            }

            if (world.has("pads")) {
                JSONObject pads = world.getJSONObject("pads");

                if (pads.has("enabled")) {
                    enablePads = pads.getBoolean("enabled");
                }

                if (pads.has("legacy_amount")) {
                    padLegacyAmount = pads.getBoolean("legacy_amount");
                }
            }

            if (world.has("elevators")) {
                JSONObject elevators = world.getJSONObject("elevators");

                if (elevators.has("enabled")) {
                    enableElevators = elevators.getBoolean("enabled");
                }
            }

            if (world.has("teleporters")) {
                JSONObject teleporters = world.getJSONObject("teleporters");

                if (teleporters.has("enabled")) {
                    enableTeleporters = teleporters.getBoolean("enabled");
                }
            }
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();

        JSONObject world = new JSONObject();

        JSONObject plates = new JSONObject();
        plates.put("enabled", enablePlates);
        plates.put("strength", plateStrength);
        plates.put("motion_y", plateMotionY);

        world.put("plates", plates);

        JSONObject pads = new JSONObject();
        pads.put("enabled", enablePads);
        pads.put("legacy_amount", padLegacyAmount);

        world.put("pads", pads);

        JSONObject elevators = new JSONObject();
        elevators.put("enabled", enableElevators);

        world.put("elevators", elevators);

        JSONObject teleporters = new JSONObject();
        teleporters.put("enabled", enableTeleporters);

        world.put("teleporters", teleporters);

        json.put("world", world);

        return json;
    }

    public static final JsonConfigFactory<PalConfig> FACTORY = new JsonConfigFactory<>() {
        @Override
        public PalConfig createDefaultConfig() {
            return new PalConfig();
        }

        @Override
        public PalConfig createConfig(JSONObject json) {
            return new PalConfig(json);
        }
    };
}
