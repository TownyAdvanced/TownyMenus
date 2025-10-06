package io.github.townyadvanced.townymenus.gui.action;

import net.kyori.adventure.sound.Sound;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class SoundAction implements ClickAction {
    private final Sound sound;

    public SoundAction(@NotNull Sound sound) {
        this.sound = sound;
    }

    @Override
    public void onClick(MenuInventory inventory, InventoryClickEvent event) {
        event.getWhoClicked().playSound(sound, Sound.Emitter.self());
    }
}
