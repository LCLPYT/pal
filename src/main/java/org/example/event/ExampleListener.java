package org.example.event;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import work.lclpnet.kibu.hook.player.PlayerConnectionHooks;
import work.lclpnet.kibu.plugin.hook.HookListenerModule;
import work.lclpnet.kibu.plugin.hook.HookRegistrar;

public class ExampleListener implements HookListenerModule {

    private final Logger logger;

    public ExampleListener(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void registerListeners(HookRegistrar registrar) {
        // register hooks here
        registrar.registerHook(PlayerConnectionHooks.JOIN_MESSAGE, this::onJoinMessage);
    }

    private Text onJoinMessage(ServerPlayerEntity player, Text joinMessage) {
        logger.info("Player joined: {}", player.getName().getString());
        return joinMessage;
    }
}
