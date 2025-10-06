package io.github.townyadvanced.townymenus.gui.input.impl.anvil;

import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.input.UserInputBackend;
import io.github.townyadvanced.townymenus.gui.input.response.Finish;
import io.github.townyadvanced.townymenus.gui.input.response.InputResponse;
import io.github.townyadvanced.townymenus.gui.input.PlayerInput;
import io.github.townyadvanced.townymenus.gui.input.response.Nothing;
import io.github.townyadvanced.townymenus.gui.input.response.OpenPreviousMenu;
import io.github.townyadvanced.townymenus.gui.input.response.ReOpen;
import io.github.townyadvanced.townymenus.gui.input.response.ErrorMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class AnvilInputBackend implements UserInputBackend {
	private final TownyMenus plugin;

	public AnvilInputBackend(final TownyMenus plugin) {
		this.plugin = plugin;
	}

	@Override
	public void startAwaitingInput(final Player player, final MenuInventory currentInventory, final Component title, final Function<PlayerInput, List<InputResponse>> inputFunction) {
		new AnvilGUI.Builder()
			.jsonTitle(GsonComponentSerializer.gson().serialize(title))
			.onClose(snapshot -> plugin.getScheduler().run(snapshot.getPlayer(), () -> currentInventory.openSilent(snapshot.getPlayer()))) // Re-open previous inventory if closed
			.onClick((slot, snapshot) -> {
				if (slot != AnvilGUI.Slot.OUTPUT)
					return Collections.emptyList();

				return toAnvil(inputFunction.apply(new PlayerInput(snapshot.getText())));
			})
			.itemLeft(MenuItem.builder(Material.PAPER).name(Component.empty()).build().itemStack())
			.plugin(plugin)
			.mainThreadExecutor(run -> player.getScheduler().run(plugin, task -> run.run(), null))
			.open(player);
	}

	private List<AnvilGUI.ResponseAction> toAnvil(final List<InputResponse> responses) {
		final List<AnvilGUI.ResponseAction> anvilResponses = new ArrayList<>();

		for (final InputResponse response : responses) {
			final AnvilGUI.ResponseAction action = switch (response) {
				case Finish ignored -> AnvilGUI.ResponseAction.close();
				case Nothing ignored -> null;
				case ReOpen reOpen -> (anvilGUI, player) -> MenuHistory.reOpen(player, reOpen.supplier());
				case ErrorMessage errorMessage -> (anvilGUI, player) -> player.sendMessage(errorMessage.error());
				case OpenPreviousMenu ignored -> (anvilGUI, player) -> MenuHistory.last(player);
				default -> throw new IllegalArgumentException("Unimplemented input response type " + response.getClass());
			};

			if (action != null) {
				anvilResponses.add(action);
			}
		}

		return anvilResponses;
	}
}
