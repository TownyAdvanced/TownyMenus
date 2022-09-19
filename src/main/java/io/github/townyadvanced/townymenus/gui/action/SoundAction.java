package io.github.townyadvanced.townymenus.gui.action;

import com.palmergames.adventure.sound.Sound;
import com.palmergames.bukkit.towny.Towny;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class SoundAction implements ClickAction {
    private final Sound sound;

    public SoundAction(@NotNull Sound sound) {
        this.sound = sound;
    }

    @Override
    public void onClick(MenuInventory inventory, InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player)
            Towny.getAdventure().player(player).playSound(sound, Sound.Emitter.self());
    }
}
