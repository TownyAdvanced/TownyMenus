package io.github.townyadvanced.townymenus.gui.input.response;

import io.github.townyadvanced.townymenus.gui.MenuInventory;
import java.util.function.Supplier;

/**
 * Different actions for responding to user input.
 */
public interface InputResponse {
    public static InputResponse reOpen(Supplier<MenuInventory> supplier) {
        return new ReOpen(supplier);
    }

	@Deprecated // replace with errorMessage that uses messaging instead
    public static InputResponse text(String text) {
        return new ReplaceText(text);
    }

    public static InputResponse doNothing() {
        return Nothing.INSTANCE;
    }

    public static InputResponse finish() {
        return Finish.INSTANCE;
    }
}
