package work.lclpnet.pal.di;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import work.lclpnet.kibu.plugin.hook.HookListenerModule;
import work.lclpnet.pal.event.PlateListener;

/**
 * Dagger module that configures all {@link HookListenerModule}s.
 */
@Module
public interface HookModule {

    @Binds @IntoSet
    HookListenerModule providePlateListener(PlateListener impl);
}
