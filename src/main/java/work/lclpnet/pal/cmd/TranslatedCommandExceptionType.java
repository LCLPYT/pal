package work.lclpnet.pal.cmd;

import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.text.Text;

import java.util.function.Function;

public class TranslatedCommandExceptionType implements CommandExceptionType {

    private final String key;

    public TranslatedCommandExceptionType(String key) {
        this.key = key;
    }

    public CommandSyntaxException create(Function<String, Text> textFunction) {
        return new CommandSyntaxException(this, textFunction.apply(key));
    }
}
