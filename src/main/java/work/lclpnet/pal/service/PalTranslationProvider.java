package work.lclpnet.pal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.pal.PalPlugin;
import work.lclpnet.translations.loader.TranslationProvider;
import work.lclpnet.translations.loader.language.LanguageLoader;
import work.lclpnet.translations.loader.language.UrlLanguageLoader;

import java.net.URL;
import java.util.List;

public class PalTranslationProvider implements TranslationProvider {

    private static final Logger logger = LoggerFactory.getLogger(PalPlugin.ID);

    @Override
    public LanguageLoader create() {
        URL[] urls = UrlLanguageLoader.getResourceLocations(this);
        List<String> resourceDirectories = List.of("lang/");

        return new UrlLanguageLoader(urls, resourceDirectories, logger);
    }
}
