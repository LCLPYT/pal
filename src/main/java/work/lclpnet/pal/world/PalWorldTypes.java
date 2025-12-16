package work.lclpnet.pal.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.pal.world.builtin.PresetWorldType;
import work.lclpnet.pal.world.builtin.VoidWorldType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PalWorldTypes {

    public static final WorldType VOID = new VoidWorldType();

    private final Map<ResourceLocation, WorldType> customTypes;

    private PalWorldTypes() {
        customTypes = new HashMap<>();

        registerWorldType(VOID);
    }

    public void registerWorldType(WorldType worldType) {
        ResourceLocation identifier = worldType.getIdentifier();
        customTypes.put(identifier, worldType);
    }

    public void unregisterWorldType(WorldType worldType) {
        ResourceLocation identifier = worldType.getIdentifier();
        customTypes.remove(identifier);
    }

    public Set<ResourceLocation> getWorldTypes(MinecraftServer server) {
        Set<ResourceLocation> identifiers = new HashSet<>();

        var serverWorldKeys = server.levelKeys();
        serverWorldKeys.forEach(key -> identifiers.add(key.location()));

        customTypes.forEach((identifier, worldType) -> identifiers.add(identifier));

        return identifiers;
    }

    public static PalWorldTypes getInstance() {
        return Holder.instance;
    }

    @Nullable
    public WorldType getWorldType(MinecraftServer server, ResourceLocation identifier) {
        WorldType worldType = customTypes.get(identifier);

        if (worldType != null) {
            return worldType;
        }

        var worldKey = ResourceKey.create(Registries.DIMENSION, identifier);

        // check that the world key is valid
        ServerLevel world = server.getLevel(worldKey);

        if (world == null) {
            return null;
        }

        return new PresetWorldType(identifier, worldKey);
    }

    private static class Holder {
        private static final PalWorldTypes instance = new PalWorldTypes();
    }
}
