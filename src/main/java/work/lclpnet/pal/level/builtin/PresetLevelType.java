package work.lclpnet.pal.level.builtin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import work.lclpnet.pal.level.LevelCreationContext;
import work.lclpnet.pal.level.LevelType;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;

public class PresetLevelType implements LevelType {

    private final Identifier identifier;
    private final ResourceKey<Level> worldKey;

    public PresetLevelType(Identifier identifier, ResourceKey<Level> worldKey) {
        this.identifier = identifier;
        this.worldKey = worldKey;
    }

    @Override
    public Identifier getIdentifier() {
        return identifier;
    }

    @Override
    public void configure(LevelCreationContext context, RuntimeLevelConfig config) {
        MinecraftServer server = context.getServer();
        ServerLevel presetWorld = server.getLevel(worldKey);

        if (presetWorld == null) {
            throw new IllegalStateException("World for key '%s' not found".formatted(worldKey.identifier()));
        }

        config.setDimensionType(presetWorld.dimensionTypeRegistration());
        config.setGenerator(presetWorld.getChunkSource().getGenerator());
    }
}
