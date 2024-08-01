package work.lclpnet.pal.service;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.pal.cmd.TranslatedCommandExceptionType;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CommandService {

    private final Translations translations;
    private final TranslatedCommandExceptionType
            requiresLivingException,
            unknownWorldException,
            unknownWorldTypeException,
            notUnloadableWorldException,
            reservedWorldIdException,
            persistedWorldFailedToLoadException,
            invalidMapSizesException,
            invalidIntException;
    private MinecraftServer server = null;

    @Inject
    public CommandService(Translations translations) {
        this.translations = translations;
        this.requiresLivingException = new TranslatedCommandExceptionType("pal.permissions.requires.living");
        this.unknownWorldException = new TranslatedCommandExceptionType("pal.errors.world.unknown");
        this.unknownWorldTypeException = new TranslatedCommandExceptionType("pal.errors.world_type.unknown");
        this.notUnloadableWorldException = new TranslatedCommandExceptionType("pal.errors.world.not_unloadable");
        this.reservedWorldIdException = new TranslatedCommandExceptionType("pal.errors.world_id.reserved");
        this.persistedWorldFailedToLoadException = new TranslatedCommandExceptionType("pal.errors.world_persistent.failed_to_load");
        this.invalidMapSizesException = new TranslatedCommandExceptionType("pal.errors.map_sizes_invalid");
        this.invalidIntException = new TranslatedCommandExceptionType("pal.errors.invalid_int");
    }

    @NotNull
    public CommandSyntaxException createRequiresLivingException(ServerCommandSource source) {
        return requiresLivingException.create(key -> translateText(source, key));
    }

    @NotNull
    public CommandSyntaxException createUnknownWorldException(ServerCommandSource source, Identifier id) {
        return unknownWorldException.create(key -> translateText(source, key, id));
    }

    @NotNull
    public CommandSyntaxException createUnknownWorldTypeException(ServerCommandSource source, Identifier id) {
        return unknownWorldTypeException.create(key -> translateText(source, key, id));
    }

    @NotNull
    public CommandSyntaxException createNotUnloadableWorldException(ServerCommandSource source) {
        return notUnloadableWorldException.create(key -> translateText(source, key));
    }

    @NotNull
    public CommandSyntaxException createReservedWorldIdException(ServerCommandSource source, Identifier id) {
        return reservedWorldIdException.create(key -> translateText(source, key, id, id.getNamespace()));
    }

    @NotNull
    public CommandSyntaxException createPersistedWorldFailedToLoadException(ServerCommandSource source, Identifier id) {
        return persistedWorldFailedToLoadException.create(key -> translateText(source, key, id));
    }

    @NotNull
    public CommandSyntaxException createInvalidMapSizesException(ServerCommandSource source) {
        return invalidMapSizesException.create(key -> translateText(source, key));
    }

    @NotNull
    public CommandSyntaxException createInvalidIntException(ServerCommandSource source, String input) {
        return invalidIntException.create(key -> translateText(source, input));
    }

    public Translations getTranslations() {
        return translations;
    }

    public RootText translateText(ServerCommandSource source, String key, Object... arguments) {
        ServerPlayerEntity player = source.getPlayer();

        if (player != null) {
            return translations.translateText(player, key, arguments);
        }

        return translations.translateText("en_us", key, arguments);
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
