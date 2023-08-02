package work.lclpnet.pal.di;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import work.lclpnet.kibu.plugin.cmd.KibuCommand;
import work.lclpnet.pal.cmd.*;

/**
 * Dagger module that configures all {@link KibuCommand}s.
 */
@Module
public interface CommandModule {

    @Binds @IntoSet
    KibuCommand provideHealCommand(HealCommand cmd);

    @Binds @IntoSet
    KibuCommand provideFeedCommand(FeedCommand cmd);

    @Binds @IntoSet
    KibuCommand provideFlyCommand(FlyCommand cmd);

    @Binds @IntoSet
    KibuCommand provideChestCommand(ChestCommand cmd);

    @Binds @IntoSet
    KibuCommand provideDieCommand(DieCommand cmd);

    @Binds @IntoSet
    KibuCommand provideInventoryCommand(InventoryCommand cmd);

    @Binds @IntoSet
    KibuCommand providePingCommand(PingCommand cmd);

    @Binds @IntoSet
    KibuCommand provideRenameCommand(RenameCommand cmd);

    @Binds @IntoSet
    KibuCommand provideSayTextCommand(SayTextCommand cmd);

    @Binds @IntoSet
    KibuCommand provideSpeedCommand(SpeedCommand cmd);

    @Binds @IntoSet
    KibuCommand provideWorldCommand(WorldCommand cmd);
}
