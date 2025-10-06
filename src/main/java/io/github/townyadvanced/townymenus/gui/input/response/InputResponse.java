package io.github.townyadvanced.townymenus.gui.input.response;

import com.palmergames.bukkit.towny.utils.TownyComponents;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.ApiStatus;
import java.util.function.Supplier;

/**
 * Different actions for responding to user input.
 */
public interface InputResponse {
    static InputResponse reOpen(Supplier<MenuInventory> supplier) {
        return new ReOpen(supplier);
    }

	@ApiStatus.Internal
	@Deprecated
	@SuppressWarnings("UnstableApiUsage")
	static InputResponse errorMessage(String errorText) {
        return errorMessage(TownyComponents.miniMessage(errorText)); // TODO: remove when getting a TownyException as a component is possible
    }

	static InputResponse errorMessage(Component error) {
		return new ErrorMessage(error.colorIfAbsent(NamedTextColor.DARK_RED));
	}

	/**
	 * @deprecated use {@link #errorMessage(String)} instead.
	 */
	@Deprecated(since = "0.0.14")
	static InputResponse replaceText(String text) {
		return errorMessage(text);
	}

    static InputResponse doNothing() {
        return Nothing.INSTANCE;
    }

    static InputResponse finish() {
        return Finish.INSTANCE;
    }

	static InputResponse openPreviousMenu() {
		return OpenPreviousMenu.INSTANCE;
	}
}
