package work.lclpnet.pal.world.builtin;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import work.lclpnet.pal.PalPlugin;
import work.lclpnet.pal.world.WorldCreationContext;
import work.lclpnet.pal.world.WorldType;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;

public class VoidWorldType implements WorldType {

    @Override
    public Identifier getIdentifier() {
        return PalPlugin.identifier("void");
    }

    @Override
    public void configure(WorldCreationContext context, RuntimeWorldConfig config) {
        MinecraftServer server = context.getServer();

        Registry<Biome> biomeRegistry = server.getRegistryManager().get(RegistryKeys.BIOME);
        RegistryEntry.Reference<Biome> biomeReference = biomeRegistry.getEntry(BiomeKeys.THE_VOID).orElseThrow();

        ChunkGenerator generator = new VoidChunkGenerator(biomeReference);
        config.setGenerator(generator);
        config.setFlat(true);

        ServerWorld overworld = server.getOverworld();
        config.setDimensionType(overworld.getDimensionEntry());
    }
}
