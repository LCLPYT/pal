package work.lclpnet.pal.level;

import net.minecraft.resources.Identifier;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;

public interface LevelType {

    Identifier getIdentifier();

    void configure(LevelCreationContext context, RuntimeLevelConfig config);
}
