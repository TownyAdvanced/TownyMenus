package io.github.townyadvanced.townymenus.gui.input.response;

public record OpenPreviousMenu() implements InputResponse {
	static final OpenPreviousMenu INSTANCE = new OpenPreviousMenu();
}
