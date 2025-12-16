package work.lclpnet.pal.world;

import net.minecraft.resources.Identifier;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

public interface WorldType {

    Identifier getIdentifier();

    void configure(WorldCreationContext context, RuntimeWorldConfig config);
}
