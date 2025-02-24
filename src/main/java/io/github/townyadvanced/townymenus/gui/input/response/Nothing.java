package io.github.townyadvanced.townymenus.gui.input.response;

public record Nothing() implements InputResponse {
	static final Nothing INSTANCE = new Nothing();
}
