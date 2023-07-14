package work.lclpnet.pal.di;

import dagger.Component;
import work.lclpnet.kibu.plugin.hook.HookListenerModule;
import work.lclpnet.pal.cmd.PalCommand;
import work.lclpnet.pal.config.ConfigManager;

import javax.inject.Singleton;
import java.util.Set;

@Singleton
@Component(modules = {
        PalModule.class,
        CommandModule.class,
        HookModule.class
})
public interface PalComponent {

    ConfigManager configManager();

    Set<PalCommand> commands();

    Set<HookListenerModule> hooks();
}
