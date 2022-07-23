package io.github.townyadvanced.townymenus.gui;

import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
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
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MenuInventory implements InventoryHolder, Iterable<ItemStack>, Supplier<MenuInventory> {
    private static final ItemStack backgroundGlass = MenuItem.builder(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build().itemStack();
    private final Inventory inventory;
    private final int size;
    private final Map<Integer, List<ClickAction>> clickActions = new HashMap<>();

    public MenuInventory(@NotNull Inventory inventory, @NotNull Component title) {
        this.inventory = Bukkit.createInventory(this, inventory.getSize(), title);
        this.inventory.setContents(inventory.getContents());
        this.size = this.inventory.getSize();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    public boolean hasActions(int slot) {
        return clickActions.containsKey(slot);
    }

    @Nullable
    public List<ClickAction> actions(int slot) {
        return clickActions.get(slot);
    }

    public void addAction(int slot, @NotNull ClickAction action) {
        clickActions.putIfAbsent(slot, new ArrayList<>());

        clickActions.get(slot).add(action);
    }

    public void addActions(Map<Integer, List<ClickAction>> actions) {
        if (!actions.isEmpty())
            clickActions.putAll(actions);
    }

    public void addItem(@NotNull MenuItem item) {
        if (item.resolveSlot(this.size) > this.inventory.getSize() - 1)
            return;

        this.inventory.setItem(item.resolveSlot(this.size), item.itemStack());

        if (!item.actions().isEmpty())
            clickActions.put(item.resolveSlot(this.size), item.actions());
    }

    public void open(@NotNull HumanEntity player) {
        Bukkit.getScheduler().runTask(TownyMenus.getPlugin(), () -> {
            player.openInventory(this.inventory);
            MenuHistory.addHistory(player.getUniqueId(), this);
        });
    }

    public void openSilent(@NotNull HumanEntity player) {
        Bukkit.getScheduler().runTask(TownyMenus.getPlugin(), () -> player.openInventory(this.inventory));
    }

    @NotNull
    @Override
    public Iterator<ItemStack> iterator() {
        return inventory.iterator();
    }

    public int size() {
        return this.size;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PaginatorBuilder paginator() {
        return new PaginatorBuilder();
    }

    @Override
    public @NotNull MenuInventory get() {
        return this;
    }

    public static class Builder {
        private final Map<Integer, ItemStack> items = new HashMap<>();
        private final Map<Integer, List<ClickAction>> actions = new HashMap<>();
        private int size = 54;
        private Component title = Component.empty();

        private Builder() {}

        public Builder addItem(@NotNull MenuItem item) {
            // TODO: resolve slots in the build method, the size can change later on
            int slot = item.resolveSlot(this.size);

            if (slot > this.size - 1)
                return this;

            items.put(slot, item.itemStack());

            if (!item.actions().isEmpty())
                actions.put(slot, item.actions());

            return this;
        }

        public Builder size(int size) {
            this.size = MenuHelper.normalizeSize(size);
            return this;
        }

        public int size() {
            return this.size;
        }

        public Builder rows(int rows) {
            this.size = Math.min(rows, 6) * 9;
            return this;
        }

        public Builder title(@NotNull Component title) {
            this.title = title;
            return this;
        }

        public MenuInventory build() {
            Inventory inventory = Bukkit.createInventory(null, size, title);

            for (int i = 0; i < size; i++)
                inventory.setItem(i, backgroundGlass);

            for (Map.Entry<Integer, ItemStack> entry : items.entrySet())
                inventory.setItem(entry.getKey(), entry.getValue());

            MenuInventory menuInventory = new MenuInventory(inventory, title);
            menuInventory.addActions(actions);

            return menuInventory;
        }
    }

    public static class PaginatorBuilder {
        private final List<MenuItem> items = new ArrayList<>();
        private final List<MenuItem> extraItems = new ArrayList<>(0);
        private Component title = Component.empty();

        public PaginatorBuilder addItem(MenuItem item) {
            this.items.add(item);
            return this;
        }

        public PaginatorBuilder addItems(Collection<MenuItem> items) {
            this.items.addAll(items);
            return this;
        }
        
        public PaginatorBuilder addExtraItem(MenuItem extraItem) {
            this.extraItems.add(extraItem);
            return this;
        }

        public PaginatorBuilder title(Component title) {
            this.title = title;
            return this;
        }

        public MenuInventory build() {
            if (this.items.isEmpty())
                this.items.add(MenuItem.builder(Material.BARRIER)
                        .name(Component.text("No entries to list.", NamedTextColor.RED))
                        .build());

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

                for (MenuItem extraItem : this.extraItems)
                    builder.addItem(extraItem);

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
                        menuInventory.addAction(menuInventory.size() - 9, ClickAction.openSilent(inventories[i - 1]));

                    if (i + 1 != inventories.length)
                        menuInventory.addAction(menuInventory.size() - 1, ClickAction.openSilent(inventories[i + 1]));
                }
            }

            return inventories[0];
        }
    }
}
