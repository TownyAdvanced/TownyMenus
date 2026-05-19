package io.github.townyadvanced.townymenus.gui.input;

import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.input.impl.anvil.AnvilInputBackend;
import io.github.townyadvanced.townymenus.gui.input.impl.text.TextInputBackend;
import io.github.townyadvanced.townymenus.gui.input.response.InputResponse;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.function.Function;

public interface UserInputBackend {
	void startAwaitingInput(final Player player, final MenuInventory currentInventory, final Component title, Function<PlayerInput, List<InputResponse>> inputFunction);

	static UserInputBackend selectBackend(TownyMenus plugin) {
		boolean incompatible = false;

		try {
			new AnvilGUI.Builder();
		} catch (Throwable throwable) {
			incompatible = true;
		}

		if (!incompatible) {
			return new AnvilInputBackend(plugin);
		}

		plugin.getSLF4JLogger().warn("This version of TownyMenus ({}) does not yet support anvil input for this version ({}), chat input will be used instead.", plugin.getVersion(), plugin.getServer().getMinecraftVersion());
		return new TextInputBackend(plugin);
	}
}
