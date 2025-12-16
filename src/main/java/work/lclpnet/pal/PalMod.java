package work.lclpnet.pal;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.kibu.cmd.impl.CommandContainer;
import work.lclpnet.kibu.cmd.type.CommandRegistrar;
import work.lclpnet.kibu.cmd.type.KibuCommand;
import work.lclpnet.kibu.hook.HookContainer;
import work.lclpnet.kibu.hook.HookListenerModule;
import work.lclpnet.kibu.hook.HookRegistrar;
import work.lclpnet.kibu.hook.world.ServerWorldReadyCallback;
import work.lclpnet.kibu.scheduler.KibuScheduling;
import work.lclpnet.kibu.scheduler.api.Scheduler;
import work.lclpnet.kibu.translate.util.ModTranslations;
import work.lclpnet.pal.di.DaggerPalComponent;
import work.lclpnet.pal.di.PalComponent;
import work.lclpnet.pal.di.PalModule;

import java.util.concurrent.CompletableFuture;

public class PalMod implements ModInitializer {

    public static final String ID = "pal";
    private static final Logger logger = LoggerFactory.getLogger(ID);
    private PalComponent component = null;

    @Override
    public void onInitialize() {
        var loadingTranslations = ModTranslations.fromAssets(PalMod.ID, logger);

        // create and register a scheduler
        var scheduler = new Scheduler(logger);
        KibuScheduling.getRootScheduler().addChild(scheduler);

        component = DaggerPalComponent.builder()
                .palModule(new PalModule(logger, loadingTranslations.translations(), scheduler))
                .build();

        CompletableFuture.allOf(
                component.configManager().init(),
                loadingTranslations.whenLoaded()
        ).whenComplete((nil, err) -> {
            if (err != null) {
                logger.error("Failed to initialize pal", err);
                return;
            }

            onLoaded();
        });

        ServerWorldReadyCallback.HOOK.register(component.commandService()::setServer);
    }

    private void onLoaded() {
        PalApiImpl api = component.api();
        PalApiImpl.setInstance(api);

        HookRegistrar hooks = new HookContainer();

        for (HookListenerModule hookModule : component.hooks()) {
            hooks.registerHooks(hookModule);
        }

        CommandRegistrar commands = new CommandContainer();

        for (KibuCommand cmd : component.commands()) {
            cmd.register(commands);
        }
    }

    public static ResourceLocation identifier(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }
}