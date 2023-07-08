package work.lclpnet.pal;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.kibu.access.PlayerLanguage;
import work.lclpnet.kibu.plugin.KibuPlugin;
import work.lclpnet.kibu.translate.TranslationService;
import work.lclpnet.pal.cmd.*;
import work.lclpnet.pal.config.ConfigManager;
import work.lclpnet.pal.event.PlateListener;
import work.lclpnet.pal.service.CommandService;
import work.lclpnet.pal.service.FormattingService;
import work.lclpnet.translations.DefaultLanguageTranslator;
import work.lclpnet.translations.Translator;
import work.lclpnet.translations.loader.translation.SPITranslationLoader;

import java.nio.file.Path;
import java.util.Optional;

public class PalPlugin extends KibuPlugin {

    public static final String ID = "pal";
    private static final Logger logger = LoggerFactory.getLogger(ID);

    @Override
    public void loadKibuPlugin() {
        final TranslationService translationService = createTranslationService();
        final CommandService commandService = new CommandService(translationService);
        final FormattingService formattingService = new FormattingService();

        new HealCommand(commandService).register(this);
        new FeedCommand(commandService).register(this);
        new FlyCommand(commandService).register(this);
        new ChestCommand(commandService).register(this);
        new DieCommand(commandService).register(this);
        new InventoryCommand(commandService).register(this);
        new PingCommand(commandService).register(this);
        new RenameCommand(commandService, formattingService).register(this);
        new SayTextCommand(formattingService).register(this);
        new SpeedCommand(commandService).register(this);

        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(ID).resolve("config.json");
        ConfigManager manager = new ConfigManager(configFile, logger);

        manager.init();

        registerHooks(new PlateListener(manager));
    }

    private TranslationService createTranslationService() {
        final SPITranslationLoader loader = new SPITranslationLoader(getClass().getClassLoader());
        final Translator translator = DefaultLanguageTranslator.create(loader).join();

        return new TranslationService(translator, player -> Optional.of(PlayerLanguage.getLanguage(player)));
    }
}