package work.lclpnet.pal.world.builtin;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import work.lclpnet.pal.world.WorldCreationContext;
import work.lclpnet.pal.world.WorldType;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

public class PresetWorldType implements WorldType {

    private final Identifier identifier;
    private final RegistryKey<World> worldKey;

    public PresetWorldType(Identifier identifier, RegistryKey<World> worldKey) {
        this.identifier = identifier;
        this.worldKey = worldKey;
    }

    @Override
    public Identifier getIdentifier() {
        return identifier;
    }

    @Override
    public void configure(WorldCreationContext context, RuntimeWorldConfig config) {
        MinecraftServer server = context.getServer();
        ServerWorld presetWorld = server.getWorld(worldKey);

        if (presetWorld == null) {
            throw new IllegalStateException("World for key '%s' not found".formatted(worldKey.getValue()));
        }

        config.setDimensionType(presetWorld.getDimensionEntry());
        config.setGenerator(presetWorld.getChunkManager().getChunkGenerator());
    }
}
