package io.github.townyadvanced.townymenus.gui.input.impl.text;

import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.input.PlayerInput;
import io.github.townyadvanced.townymenus.gui.input.response.InputResponse;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.function.Function;

public class TextInputSession {
	private final MenuInventory currentInventory;
	private final Function<PlayerInput, List<InputResponse>> inputFunction;
	private ScheduledTask timeoutTask;

	public TextInputSession(final MenuInventory currentInventory, final Function<PlayerInput, List<InputResponse>> inputFunction) {
		this.currentInventory = currentInventory;
		this.inputFunction = inputFunction;
	}

	public MenuInventory currentInventory() {
		return this.currentInventory;
	}

	public Function<PlayerInput, List<InputResponse>> inputFunction() {
		return this.inputFunction;
	}

	@Nullable
	public ScheduledTask timeoutTask() {
		return this.timeoutTask;
	}

	public void timeoutTask(@Nullable ScheduledTask task) {
		this.timeoutTask = task;
	}
}
