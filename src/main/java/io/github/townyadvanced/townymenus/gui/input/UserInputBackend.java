package io.github.townyadvanced.townymenus.gui.input;

import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.input.impl.anvil.AnvilInputBackend;
import io.github.townyadvanced.townymenus.gui.input.impl.text.TextInputBackend;
import io.github.townyadvanced.townymenus.gui.input.response.InputResponse;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import net.wesjd.anvilgui.version.VersionMatcher;
import org.bukkit.entity.Player;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface UserInputBackend {
	void startAwaitingInput(final Player player, final MenuInventory currentInventory, final Component title, Function<PlayerInput, List<InputResponse>> inputFunction);

	static UserInputBackend selectBackend(TownyMenus plugin) {
		try {

			// Use reflection to access the versions map in anvilgui to check if it contains the current mc version
			final Field versionMapField = VersionMatcher.class.getDeclaredField("VERSION_TO_REVISION");
			versionMapField.setAccessible(true);
			final Map<?, ?> versionMap = (Map<?, ?>) versionMapField.get(null);

			if (versionMap.containsKey(plugin.getServer().getMinecraftVersion())) {
				boolean stillNotCompatible = false;

				try {
					new AnvilGUI.Builder(); // this throws if spigot mappings are not available
				} catch (Throwable throwable) {
					stillNotCompatible = true;
				}

				if (!stillNotCompatible) {
					return new AnvilInputBackend(plugin);
				}
			}
		} catch (Throwable throwable) {
			plugin.getSLF4JLogger().warn("Failed to check AnvilGUI minecraft version compatability", throwable);
		}

		plugin.getSLF4JLogger().warn("This version of TownyMenus ({}) does not yet support anvil input for this version ({}), chat input will be used instead.", plugin.getVersion(), plugin.getServer().getMinecraftVersion());
		return new TextInputBackend(plugin);
	}
}
