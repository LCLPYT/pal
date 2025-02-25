package work.lclpnet.pal.event;

import work.lclpnet.kibu.hook.HookListenerModule;
import work.lclpnet.kibu.hook.HookRegistrar;
import work.lclpnet.kibu.hook.player.PlayerInventoryHooks;
import work.lclpnet.pal.util.TextPrompt;

import javax.inject.Inject;

public class InventoryListener implements HookListenerModule {

    @Inject
    public InventoryListener() {}

    @Override
    public void registerListeners(HookRegistrar registrar) {
        registrar.registerHook(PlayerInventoryHooks.MODIFY_INVENTORY, event -> {
            if (event.player().currentScreenHandler instanceof TextPrompt.Handler handler) {
                handler.onClick(event);
                return true;
            }

            return false;
        });
    }
}
