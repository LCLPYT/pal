package work.lclpnet.pal.di;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import work.lclpnet.pal.cmd.*;

/**
 * Dagger module that configures all {@link PalCommand}s.
 */
@Module
public interface CommandModule {

    @Binds @IntoSet
    PalCommand provideHealCommand(HealCommand cmd);

    @Binds @IntoSet
    PalCommand provideFeedCommand(FeedCommand cmd);

    @Binds @IntoSet
    PalCommand provideFlyCommand(FlyCommand cmd);

    @Binds @IntoSet
    PalCommand provideChestCommand(ChestCommand cmd);

    @Binds @IntoSet
    PalCommand provideDieCommand(DieCommand cmd);

    @Binds @IntoSet
    PalCommand provideInventoryCommand(InventoryCommand cmd);

    @Binds @IntoSet
    PalCommand providePingCommand(PingCommand cmd);

    @Binds @IntoSet
    PalCommand provideRenameCommand(RenameCommand cmd);

    @Binds @IntoSet
    PalCommand provideSayTextCommand(SayTextCommand cmd);

    @Binds @IntoSet
    PalCommand provideSpeedCommand(SpeedCommand cmd);
}
