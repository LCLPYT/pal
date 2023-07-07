package work.lclpnet.pal.service;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FormattingService {

    public MutableText parseText(String string, char formatChar) {
        String[] parts = string.split(Pattern.quote(String.valueOf(formatChar)));
        Style style = Style.EMPTY;

        List<MutableText> texts = new ArrayList<>();
        StringBuilder carry = new StringBuilder(parts[0]);

        for (int i = 1, partsLength = parts.length; i < partsLength; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            Formatting format = getFormatting(part.charAt(0));

            if (format == null) {
                carry.append(part);
                continue;
            }

            if (carry.length() > 0) {
                texts.add(Text.literal(carry.toString()).setStyle(style));
                carry.setLength(0);
            }

            style = style.withFormatting(format);

            texts.add(Text.literal(part.substring(1)).setStyle(style));
        }

        if (carry.length() > 0) {
            texts.add(Text.literal(carry.toString()).setStyle(style));
        }

        if (texts.isEmpty()) {
            return Text.empty();
        }

        MutableText root = texts.get(0);

        for (int i = 1, len = texts.size(); i < len; i++) {
            root.append(texts.get(i));
        }

        return root;
    }

    @Nullable
    private Formatting getFormatting(char spec) {
        return switch (spec) {
            case '0' -> Formatting.BLACK;
            case '1' -> Formatting.DARK_BLUE;
            case '2' -> Formatting.DARK_GREEN;
            case '3' -> Formatting.DARK_AQUA;
            case '4' -> Formatting.DARK_RED;
            case '5' -> Formatting.DARK_PURPLE;
            case '6' -> Formatting.GOLD;
            case '7' -> Formatting.GRAY;
            case '8' -> Formatting.DARK_GRAY;
            case '9' -> Formatting.BLUE;
            case 'a' -> Formatting.GREEN;
            case 'b' -> Formatting.AQUA;
            case 'c' -> Formatting.RED;
            case 'd' -> Formatting.LIGHT_PURPLE;
            case 'e' -> Formatting.YELLOW;
            case 'f' -> Formatting.WHITE;
            case 'k' -> Formatting.OBFUSCATED;
            case 'l' -> Formatting.BOLD;
            case 'm' -> Formatting.STRIKETHROUGH;
            case 'n' -> Formatting.UNDERLINE;
            case 'o' -> Formatting.ITALIC;
            case 'r' -> Formatting.RESET;
            default -> null;
        };
    }
}
