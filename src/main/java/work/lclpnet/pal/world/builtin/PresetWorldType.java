package work.lclpnet.pal.world.builtin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import work.lclpnet.pal.world.WorldCreationContext;
import work.lclpnet.pal.world.WorldType;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

public class PresetWorldType implements WorldType {

    private final ResourceLocation identifier;
    private final ResourceKey<Level> worldKey;

    public PresetWorldType(ResourceLocation identifier, ResourceKey<Level> worldKey) {
        this.identifier = identifier;
        this.worldKey = worldKey;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return identifier;
    }

    @Override
    public void configure(WorldCreationContext context, RuntimeWorldConfig config) {
        MinecraftServer server = context.getServer();
        ServerLevel presetWorld = server.getLevel(worldKey);

        if (presetWorld == null) {
            throw new IllegalStateException("World for key '%s' not found".formatted(worldKey.location()));
        }

        config.setDimensionType(presetWorld.dimensionTypeRegistration());
        config.setGenerator(presetWorld.getChunkSource().getGenerator());
    }
}
