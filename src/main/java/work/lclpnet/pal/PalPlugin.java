package work.lclpnet.pal;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.kibu.plugin.KibuPlugin;
import work.lclpnet.pal.cmd.HealCommand;
import work.lclpnet.pal.config.ConfigManager;
import work.lclpnet.pal.event.PlateListener;
import work.lclpnet.pal.service.CommandService;
import work.lclpnet.translations.DefaultLanguageTranslator;
import work.lclpnet.translations.Translator;
import work.lclpnet.translations.loader.translation.SPITranslationLoader;

import java.nio.file.Path;

public class PalPlugin extends KibuPlugin {

    public static final String ID = "pal";
    private static final Logger logger = LoggerFactory.getLogger(ID);

    @Override
    public void loadKibuPlugin() {
        final SPITranslationLoader loader = new SPITranslationLoader(getClass().getClassLoader());
        final Translator translator = DefaultLanguageTranslator.create(loader).join();

        final CommandService commandService = new CommandService(translator);

        new HealCommand(commandService).register(this);

        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(ID).resolve("config.json");
        ConfigManager manager = new ConfigManager(configFile, logger);

        manager.init();

        registerHooks(new PlateListener(manager));
    }
}