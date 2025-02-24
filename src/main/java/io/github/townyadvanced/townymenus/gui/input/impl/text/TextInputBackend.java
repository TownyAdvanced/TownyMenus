package io.github.townyadvanced.townymenus.gui.input.impl.text;

import com.palmergames.bukkit.towny.object.Translatable;
import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuHistory;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.input.response.Finish;
import io.github.townyadvanced.townymenus.gui.input.response.InputResponse;
import io.github.townyadvanced.townymenus.gui.input.PlayerInput;
import io.github.townyadvanced.townymenus.gui.input.UserInputBackend;
import io.github.townyadvanced.townymenus.gui.input.response.Nothing;
import io.github.townyadvanced.townymenus.gui.input.response.ReOpen;
import io.github.townyadvanced.townymenus.gui.input.response.ReplaceText;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class TextInputBackend implements UserInputBackend, Listener {
	private static final Duration INPUT_TIMEOUT = Duration.ofSeconds(60);
	private static final Collection<String> CANCEL_PHRASES = Set.of("q", "quit", "cancel", "stop");

	private final TownyMenus plugin;
	private final Map<UUID, TextInputSession> sessions = new ConcurrentHashMap<>();

	public TextInputBackend(TownyMenus plugin) {
		this.plugin = plugin;
	}

	@Override
	public void startAwaitingInput(final Player player, final MenuInventory currentInventory, final Component title, final Function<PlayerInput, List<InputResponse>> inputFunction) {
		final TextInputSession session = new TextInputSession(currentInventory, inputFunction);
		final UUID uuid = player.getUniqueId();

		cancelSession(uuid);
		sessions.put(uuid, session);

		player.closeInventory();

		player.sendRichMessage(Translatable.of("townymenus:plugin-prefix").append(Translatable.of("chat-input-header")).forLocale(player));
		player.sendMessage(title);
		player.sendRichMessage(Translatable.of("townymenus:plugin-prefix").append(Translatable.of("chat-input-timeout-warning", String.valueOf(INPUT_TIMEOUT.getSeconds()))).forLocale(player));

		session.timeoutTask(plugin.getServer().getAsyncScheduler().runDelayed(plugin, task -> {
			cancelSession(uuid);

			final Player p = plugin.getServer().getPlayer(uuid);
			if (p != null) {
				p.sendRichMessage(Translatable.of("townymenus:plugin-prefix").append(Translatable.of("chat-input-timed-out")).forLocale(p));
			}
		}, INPUT_TIMEOUT.getSeconds(), TimeUnit.SECONDS));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void listenForInput(AsyncChatEvent event) {
		final Player player = event.getPlayer();

		final TextInputSession session = sessions.get(player.getUniqueId());
		if (session == null)
			return;

		event.setCancelled(true);

		final String plain = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
		if (CANCEL_PHRASES.contains(plain)) {
			cancelSession(player.getUniqueId());
			player.sendRichMessage(Translatable.of("townymenus:plugin-prefix").append(Translatable.of("chat-input-cancelled")).forLocale(player));
			return;
		}

		final List<InputResponse> responses = session.inputFunction().apply(new PlayerInput(plain));

		for (final InputResponse response : responses) {
			switch (response) {
				case Finish finish -> cancelSession(player.getUniqueId());
				case Nothing nothing -> {}
				case ReOpen reOpen -> {
					MenuHistory.reOpen(player, reOpen.supplier());
					cancelSession(player.getUniqueId());
				}
				case ReplaceText replaceText -> {} // Doesn't work with chat
				default -> throw new IllegalArgumentException("Unimplemented input response type " + response.getClass().toString());
			}
		}
	}

	@EventHandler
	public void invalidateSession(PlayerQuitEvent event) {
		cancelSession(event.getPlayer().getUniqueId());
	}

	private void cancelSession(UUID uuid) {
		final TextInputSession session = sessions.remove(uuid);
		if (session != null && session.timeoutTask() != null) {
			session.timeoutTask().cancel();
		}
	}
}
