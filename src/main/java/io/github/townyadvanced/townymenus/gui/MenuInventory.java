package io.github.townyadvanced.townymenus.gui;

import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.action.BackAction;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MenuInventory implements InventoryHolder, Iterable<ItemStack>, Supplier<MenuInventory> {
    private final Inventory inventory;
    private final Map<Integer, List<ClickAction>> clickActions = new HashMap<>();

    public MenuInventory(@NotNull Inventory inventory, @NotNull Component title) {
        this.inventory = Bukkit.createInventory(this, inventory.getSize(), title);
        this.inventory.setContents(inventory.getContents());
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
        if (item.slot() > this.inventory.getSize() - 1)
            return;

        this.inventory.setItem(item.slot(), item.itemStack());

        if (!item.actions().isEmpty())
            clickActions.put(item.slot(), item.actions());
    }

    public void open(@NotNull HumanEntity player) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuInventory inventory) {
            for (List<ClickAction> actions : clickActions.values()) {
                for (int i = 0; i < actions.size(); i++) {
                    ClickAction action = actions.get(i);
                    if (action instanceof BackAction)
                        actions.set(i, ClickAction.openInventory(inventory));
                }
            }
        }

        Bukkit.getScheduler().runTask(TownyMenus.getPlugin(), () -> player.openInventory(this.inventory));
    }

    @NotNull
    @Override
    public Iterator<ItemStack> iterator() {
        return inventory.iterator();
    }

    public int size() {
        return this.inventory.getSize();
    }

    public static Builder builder() {
        return new Builder();
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
            if (item.slot() > size - 1)
                return this;

            items.put(item.slot(), item.itemStack());

            if (!item.actions().isEmpty())
                actions.put(item.slot(), item.actions());

            return this;
        }

        public Builder size(int size) {
            this.size = (int) Math.min(Math.ceil(size / 9d) * 9, 54);
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

            for (Map.Entry<Integer, ItemStack> entry : items.entrySet())
                inventory.setItem(entry.getKey(), entry.getValue());

            MenuInventory menuInventory = new MenuInventory(inventory, title);
            menuInventory.addActions(actions);

            return menuInventory;
        }
    }
}
