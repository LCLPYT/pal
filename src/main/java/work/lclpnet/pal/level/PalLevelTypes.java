package work.lclpnet.pal.level;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.pal.level.builtin.PresetLevelType;
import work.lclpnet.pal.level.builtin.VoidLevelType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PalLevelTypes {

    public static final LevelType VOID = new VoidLevelType();

    private final Map<Identifier, LevelType> customTypes;

    private PalLevelTypes() {
        customTypes = new HashMap<>();

        registerLevelType(VOID);
    }

    public void registerLevelType(LevelType levelType) {
        Identifier identifier = levelType.getIdentifier();
        customTypes.put(identifier, levelType);
    }

    public void unregisterLevelType(LevelType levelType) {
        Identifier identifier = levelType.getIdentifier();
        customTypes.remove(identifier);
    }

    public Set<Identifier> getLevelTypes(MinecraftServer server) {
        Set<Identifier> identifiers = new HashSet<>();

        var serverLevelKeys = server.levelKeys();
        serverLevelKeys.forEach(key -> identifiers.add(key.identifier()));

        customTypes.forEach((identifier, _) -> identifiers.add(identifier));

        return identifiers;
    }

    public static PalLevelTypes getInstance() {
        return Holder.instance;
    }

    @Nullable
    public LevelType getLevelType(MinecraftServer server, Identifier identifier) {
        LevelType levelType = customTypes.get(identifier);

        if (levelType != null) {
            return levelType;
        }

        var levelKey = ResourceKey.create(Registries.DIMENSION, identifier);

        // check that the level key is valid
        ServerLevel level = server.getLevel(levelKey);

        if (level == null) {
            return null;
        }

        return new PresetLevelType(identifier, levelKey);
    }

    private static class Holder {
        private static final PalLevelTypes instance = new PalLevelTypes();
    }
}
