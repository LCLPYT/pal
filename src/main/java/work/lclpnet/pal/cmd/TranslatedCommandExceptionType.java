package work.lclpnet.pal.cmd;

import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.text.Text;
import work.lclpnet.translations.Translator;

public class TranslatedCommandExceptionType implements CommandExceptionType {

    private final String key;
    private final Translator translator;

    public TranslatedCommandExceptionType(Translator translator, String key) {
        this.translator = translator;
        this.key = key;
    }

    public CommandSyntaxException create(String locale) {
        String translated = translator.translate(locale, key);

        return new CommandSyntaxException(this, Text.literal(translated));
    }
}
