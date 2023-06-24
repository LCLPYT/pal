package work.lclpnet.pal.config;

import org.json.JSONObject;
import work.lclpnet.config.json.JsonConfig;
import work.lclpnet.config.json.JsonConfigFactory;

public class PalConfig implements JsonConfig {

    public boolean enablePlates = false;
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

                if (plates.has("motionY")) {
                    plateMotionY = plates.getFloat("motionY");
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
        plates.put("motionY", plateMotionY);

        world.put("plates", plates);

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
