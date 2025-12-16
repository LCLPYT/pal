package work.lclpnet.pal.service;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Singleton
public class FormattingService {

    @Inject
    public FormattingService() {}

    public MutableComponent parseText(String string, char formatChar) {
        String[] parts = string.split(Pattern.quote(String.valueOf(formatChar)));
        Style style = Style.EMPTY;

        List<MutableComponent> texts = new ArrayList<>();
        StringBuilder carry = new StringBuilder(parts[0]);

        for (int i = 1, partsLength = parts.length; i < partsLength; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            ChatFormatting format = getFormatting(part.charAt(0));

            if (format == null) {
                carry.append(part);
                continue;
            }

            if (!carry.isEmpty()) {
                texts.add(Component.literal(carry.toString()).setStyle(style));
                carry.setLength(0);
            }

            style = style.applyFormat(format);

            texts.add(Component.literal(part.substring(1)).setStyle(style));
        }

        if (!carry.isEmpty()) {
            texts.add(Component.literal(carry.toString()).setStyle(style));
        }

        if (texts.isEmpty()) {
            return Component.empty();
        }

        MutableComponent root = texts.getFirst();

        for (int i = 1, len = texts.size(); i < len; i++) {
            root.append(texts.get(i));
        }

        return root;
    }

    @Nullable
    private ChatFormatting getFormatting(char spec) {
        return switch (spec) {
            case '0' -> ChatFormatting.BLACK;
            case '1' -> ChatFormatting.DARK_BLUE;
            case '2' -> ChatFormatting.DARK_GREEN;
            case '3' -> ChatFormatting.DARK_AQUA;
            case '4' -> ChatFormatting.DARK_RED;
            case '5' -> ChatFormatting.DARK_PURPLE;
            case '6' -> ChatFormatting.GOLD;
            case '7' -> ChatFormatting.GRAY;
            case '8' -> ChatFormatting.DARK_GRAY;
            case '9' -> ChatFormatting.BLUE;
            case 'a' -> ChatFormatting.GREEN;
            case 'b' -> ChatFormatting.AQUA;
            case 'c' -> ChatFormatting.RED;
            case 'd' -> ChatFormatting.LIGHT_PURPLE;
            case 'e' -> ChatFormatting.YELLOW;
            case 'f' -> ChatFormatting.WHITE;
            case 'k' -> ChatFormatting.OBFUSCATED;
            case 'l' -> ChatFormatting.BOLD;
            case 'm' -> ChatFormatting.STRIKETHROUGH;
            case 'n' -> ChatFormatting.UNDERLINE;
            case 'o' -> ChatFormatting.ITALIC;
            case 'r' -> ChatFormatting.RESET;
            default -> null;
        };
    }
}
