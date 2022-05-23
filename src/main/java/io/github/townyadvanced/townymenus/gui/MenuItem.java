package io.github.townyadvanced.townymenus.gui;

import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.gui.anchor.SlotAnchor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MenuItem {
    private SlotAnchor slot;
    private final ItemStack itemStack;
    private final List<ClickAction> actions = new ArrayList<>(0);

    public MenuItem(ItemStack itemStack, SlotAnchor slot) {
        this.itemStack = itemStack;
        this.slot = slot;
    }

    public int resolveSlot(int size) {
        return this.slot.resolveSlot(size);
    }

    public void slot(int slot) {
        this.slot = SlotAnchor.ofExact(slot);
    }

    public void slot(SlotAnchor slot) {
        this.slot = slot;
    }

    public ItemStack itemStack() {
        return this.itemStack;
    }

    @NotNull
    public List<ClickAction> actions() {
        return this.actions;
    }

    public void addAction(@NotNull ClickAction action) {
        this.actions.add(action);
    }

    public void addActions(@NotNull List<ClickAction> actions) {
        this.actions.addAll(actions);
    }

    public static Builder builder(@NotNull Material type) {
        return new Builder(type);
    }

    public Builder builder() {
        Builder builder = builder(itemStack.getType())
                .name(itemStack.displayName())
                .slot(slot)
                .size(itemStack.getAmount());

        final ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            if (meta.hasEnchants())
                builder.withGlint();

            if (meta.hasLore())
                builder.lore(meta.lore());

            if (itemStack.getType() == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta && skullMeta.hasOwner() && skullMeta.getPlayerProfile().isComplete())
                builder.skullOwner(skullMeta.getPlayerProfile().getId());
        }

        for (ClickAction clickAction : this.actions)
            builder.action(clickAction);

        return builder;
    }

    public static class Builder {
        private final Material type;
        private Component name = Component.empty();
        private final List<Component> lore = new ArrayList<>(0);
        private int size = 1;
        private SlotAnchor slot = SlotAnchor.ofExact(0);
        private final List<ClickAction> actions = new ArrayList<>(0);
        private boolean glint;
        private UUID ownerUUID;

        private Builder(Material type) {
            this.type = type;
        }

        public Builder name(@NotNull Component name) {
            this.name = name.decoration(TextDecoration.ITALIC, false);
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder action(@NotNull ClickAction action) {
            this.actions.add(action);
            return this;
        }

        public Builder clearActions() {
            this.actions.clear();
            return this;
        }

        public Builder slot(int slot) {
            this.slot = SlotAnchor.ofExact(slot);
            return this;
        }

        public Builder slot(SlotAnchor slot) {
            this.slot = slot;
            return this;
        }

        public Builder withGlint() {
            this.glint = true;
            return this;
        }

        public Builder lore(@NotNull Component lore) {
            if (!lore.equals(Component.empty()))
                this.lore.add(lore.decoration(TextDecoration.ITALIC, false));

            return this;
        }

        public Builder lore(@NotNull List<Component> lore) {
            this.lore.addAll(lore.stream().map(component -> component.decoration(TextDecoration.ITALIC, false)).toList());
            return this;
        }

        public Builder skullOwner(@NotNull UUID ownerUUID) {
            this.ownerUUID = ownerUUID;
            return this;
        }

        public MenuItem build() {
            ItemStack itemStack = new ItemStack(type, size);

            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {

                if (!name.equals(Component.empty()))
                    meta.displayName(name);

                if (!lore.isEmpty())
                    meta.lore(lore);

                if (meta instanceof SkullMeta skullMeta && this.ownerUUID != null)
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(this.ownerUUID));

                itemStack.setItemMeta(meta);
            }

            if (glint) {
                itemStack.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            }

            MenuItem item = new MenuItem(itemStack, slot);
            item.addActions(this.actions);

            return item;
        }
    }
}
