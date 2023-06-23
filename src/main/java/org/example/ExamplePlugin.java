package org.example;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.MinecraftVersion;
import org.example.cmd.CustomCommand;
import org.example.event.ExampleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.kibu.plugin.KibuPlugin;
import work.lclpnet.kibu.scheduler.Ticks;
import work.lclpnet.mplugins.MPlugins;
import work.lclpnet.mplugins.ext.WorldStateListener;

public class ExamplePlugin extends KibuPlugin implements WorldStateListener {

    public static final String ID = "examplePlugin";
    private static final Logger logger = LoggerFactory.getLogger(ID);

    // if you add a custom constructor, make sure to have a default constructor (no arguments)

    @Override
    public void loadKibuPlugin() {
        // do initialization here...
        var fabric = FabricLoader.getInstance();
        var mplugins = fabric.getModContainer(MPlugins.MOD_ID).orElseThrow();

        logger.info("Running Minecraft {} with Fabric {} and mplugins {}",
                MinecraftVersion.CURRENT.getName(), FabricLoaderImpl.VERSION, mplugins.getMetadata().getVersion());

        // register hooks like this
        registerHooks(new ExampleListener(logger));

        // register commands like this
        registerCommand(CustomCommand.create());

        // access to scheduler for delayed or recurring tasks
        getScheduler().timeout(() -> logger.info("This message is logged 10 seconds delayed."), Ticks.seconds(10));
    }

    @Override
    public void onWorldReady() {
        // called when the main world is loaded
    }

    @Override
    public void onWorldUnready() {
        // called when the main world or the plugin is unloading
    }
}