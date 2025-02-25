package work.lclpnet.pal.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.kibu.hook.player.PlayerInventoryHooks;
import work.lclpnet.pal.mixin.ScreenHandlerAccessor;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class TextPrompt {

    private final Text title;
    private final String initial;
    private final Predicate<String> validator;

    public TextPrompt(Text title) {
        this(title, input -> true);
    }

    public TextPrompt(Text title, Predicate<String> validator) {
        this(title, "", validator);
    }

    public TextPrompt(Text title, String initial, Predicate<String> validator) {
        this.title = title;
        this.initial = initial;
        this.validator = validator;
    }

    public CompletableFuture<Optional<String>> open(ServerPlayerEntity player) {
        var future = new CompletableFuture<Optional<String>>();

        ScreenHandlerFactory factory = (syncId, inv, p) -> createMenu(syncId, inv, future);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(factory, title));

        return future;
    }

    private TextInputHandler createMenu(int syncId, PlayerInventory inventory, CompletableFuture<Optional<String>> future) {
        var handler = new TextInputHandler(syncId, inventory, future);
        handler.setInitial(initial);

        return handler;
    }

    public interface Handler {
        void onClick(PlayerInventoryHooks.ClickEvent event);
    }

    private class TextInputHandler extends AnvilScreenHandler implements Handler {

        private final ItemStack EMPTY_INEQUALITY = new ItemStack(Items.POISONOUS_POTATO);  // ¯\_(ツ)_/¯
        private final CompletableFuture<Optional<String>> future;
        private @Nullable String value = null;
        private boolean changed = false;

        protected TextInputHandler(int syncId, PlayerInventory inventory, CompletableFuture<Optional<String>> future) {
            super(syncId, inventory);
            this.future = future;
        }

        @Override
        public void sendContentUpdates() {
            if (changed) {
                changed = false;

                // some stack that doesn't equal the new output stack so that the cache will miss
                ItemStack invalidateStack;

                if (value != null) {
                    output.setStack(0, textStack(value));
                    invalidateStack = ItemStack.EMPTY;
                } else {
                    output.setStack(0, ItemStack.EMPTY);
                    invalidateStack = EMPTY_INEQUALITY;
                }

                // set no level cost
                setProperty(0, 0);

                // invalidate tracked data
                ((ScreenHandlerAccessor) this).getTrackedPropertyValues().set(0, 1);
                setPreviousTrackedSlotMutable(2, invalidateStack);
            }

            super.sendContentUpdates();
        }

        @Override
        public boolean setNewItemName(String newItemName) {
            value = validate(newItemName);
            changed = true;

            return super.setNewItemName(newItemName);
        }

        public void setInitial(String value) {
            ItemStack stack = textStack(value);

            input.setStack(0, stack);

            if (validate(value) != null) {
                output.setStack(0, stack);
            }

            this.value = validate(value);
        }

        public @Nullable String validate(String str) {
            String sanitized = StringHelper.stripInvalidChars(str);

            if (sanitized.length() > 50 || !validator.test(sanitized)) {
                return null;
            }

            return sanitized;
        }

        private @NotNull ItemStack textStack(String value) {
            ItemStack stack = new ItemStack(Items.PAPER);
            stack.set(DataComponentTypes.ITEM_NAME, Text.literal(value));
            return stack;
        }

        @Override
        public void onClick(PlayerInventoryHooks.ClickEvent event) {
            if (event.slot() != 2 || value == null) return;

            future.complete(Optional.of(value));
            event.player().closeHandledScreen();
        }

        @Override
        public void onClosed(PlayerEntity player) {
            super.onClosed(player);

            future.complete(Optional.empty());
        }
    }
}
