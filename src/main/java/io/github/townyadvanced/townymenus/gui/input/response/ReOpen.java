package io.github.townyadvanced.townymenus.gui.input.response;

import io.github.townyadvanced.townymenus.gui.MenuInventory;
import java.util.function.Supplier;

public record ReOpen(Supplier<MenuInventory> supplier) implements InputResponse {
}
