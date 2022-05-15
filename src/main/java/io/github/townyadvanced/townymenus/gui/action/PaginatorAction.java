package io.github.townyadvanced.townymenus.gui.action;

import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.anchor.HorizontalAnchor;
import io.github.townyadvanced.townymenus.gui.anchor.SlotAnchor;
import io.github.townyadvanced.townymenus.gui.anchor.VerticalAnchor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.function.Supplier;

public class PaginatorAction implements ClickAction {
    private final Supplier<List<MenuItem>> supplier;
    private final Component title;

    public PaginatorAction(Component title, List<MenuItem> items) {
        this.supplier = () -> items;
        this.title = title;
    }

    public PaginatorAction(Component title, Supplier<List<MenuItem>> supplier) {
        this.supplier = supplier;
        this.title = title;
    }

    @Override
    public void onClick(MenuInventory inventory, InventoryClickEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(TownyMenus.getPlugin(), () -> {
            List<MenuItem> items = supplier.get();
            // Each page can hold 45 items (5 rows), the bottom row is reserved for forward/back buttons.
            int pageCount = (int) Math.ceil(items.size() / 45d);
            MenuInventory[] inventories = new MenuInventory[pageCount];

            for (int i = 0; i < pageCount; i++) {
                List<MenuItem> pageItems = items.subList(i * 45, Math.min((i + 1) * 45, items.size()));

                MenuInventory.Builder builder = MenuInventory.builder()
                        .size(pageItems.size() + 9)
                        .title(title.append(Component.text(" (Page " + (i + 1) + "/" + pageCount + ")")));

                for (int j = 0; j < pageItems.size(); j++) {
                    MenuItem item = pageItems.get(j);
                    item.slot(j);
                    builder.addItem(item);
                }

                // Add a back button if we're not on the first page
                if (i != 0)
                    builder.addItem(MenuItem.builder(Material.ARROW)
                            .name(Component.text("Back", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                            .lore(Component.text("Click to go back to the previous page.", NamedTextColor.GOLD))
                            .action(ClickAction.sound(Sound.sound(Key.key(Key.MINECRAFT_NAMESPACE, "block.stone_button.click_on"), Sound.Source.PLAYER, 1.0f, 1.0f)))
                            .slot(SlotAnchor.of(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromLeft(0)))
                            .build());

                // Since we already have the inventory to go back to, clear the actions and add an openInventory one instead.
                builder.addItem(MenuHelper.backButton()
                        .clearActions()
                        .action(ClickAction.openInventory(inventory))
                        .slot(SlotAnchor.of(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromRight(4)))
                        .build());

                if (i + 1 != pageCount)
                    builder.addItem(MenuItem.builder(Material.ARROW)
                            .name(Component.text("Next", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                            .lore(Component.text("Click to go to the next page.", NamedTextColor.GOLD))
                            .action(ClickAction.sound(Sound.sound(Key.key(Key.MINECRAFT_NAMESPACE, "block.stone_button.click_on"), Sound.Source.PLAYER, 1.0f, 1.0f)))
                            .slot(SlotAnchor.of(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromRight(0)))
                            .build());

                inventories[i] = builder.build();
            }

            // Loop over the pages if there are more than 1 in order to add the appropriate click events to items.
            if (pageCount > 1) {
                for (int i = 0; i < inventories.length; i++) {
                    MenuInventory menuInventory = inventories[i];

                    if (i != 0)
                        menuInventory.addAction(menuInventory.size() - 9, ClickAction.openInventory(inventories[i - 1]));

                    if (i + 1 != inventories.length)
                        menuInventory.addAction(menuInventory.size() - 1, ClickAction.openInventory(inventories[i + 1]));
                }
            }

            Bukkit.getScheduler().runTask(TownyMenus.getPlugin(), () -> inventories[0].open(event.getWhoClicked()));
        });
    }
}
