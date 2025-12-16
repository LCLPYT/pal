package work.lclpnet.pal.world.builtin;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import work.lclpnet.pal.PalMod;
import work.lclpnet.pal.world.WorldCreationContext;
import work.lclpnet.pal.world.WorldType;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;

public class VoidWorldType implements WorldType {

    @Override
    public Identifier getIdentifier() {
        return PalMod.identifier("void");
    }

    @Override
    public void configure(WorldCreationContext context, RuntimeWorldConfig config) {
        MinecraftServer server = context.getServer();

        Registry<Biome> biomeRegistry = server.registryAccess().lookupOrThrow(Registries.BIOME);
        Holder.Reference<Biome> biomeReference = biomeRegistry.get(Biomes.THE_VOID).orElseThrow();

        ChunkGenerator generator = new VoidChunkGenerator(biomeReference);
        config.setGenerator(generator);
        config.setFlat(true);

        ServerLevel overworld = server.overworld();
        config.setDimensionType(overworld.dimensionTypeRegistration());
    }
}
