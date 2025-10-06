package io.github.townyadvanced.townymenus.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.gui.slot.Slot;
import io.github.townyadvanced.townymenus.gui.slot.anchor.SlotAnchor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class MenuItem {
    public static final NamespacedKey PDC_KEY = Objects.requireNonNull(NamespacedKey.fromString("townymenus:menuitem")); // Hide the might be null message
    private Slot slot;
    private final ItemStack itemStack;
    private final List<ClickAction> actions = new ArrayList<>(0);

    public MenuItem(@NotNull ItemStack itemStack, @NotNull Slot slot) {
        this.itemStack = itemStack;
        this.slot = slot;
    }

    public void slot(int slot) {
        this.slot = SlotAnchor.ofSlot(slot);
    }

    public void slot(@NotNull Slot slot) {
        this.slot = slot;
    }

    @NotNull
    public Slot slot() {
        return this.slot;
    }

    public ItemStack itemStack() {
        return this.itemStack.clone();
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
                .slot(slot)
                .size(itemStack.getAmount());

        final ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            builder.withGlint(meta.hasEnchants());

			final Component displayName = meta.displayName();
            if (displayName != null) {
				builder.name(displayName);
			}

			final List<Component> lore = meta.lore();
            if (lore != null) {
				builder.lore(lore);
			}

            if (itemStack.getType() == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta && skullMeta.getPlayerProfile() instanceof PlayerProfile profile && profile.getId() != null) {
				builder.skullOwner(profile.getId());
			}
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
        private Slot slot = SlotAnchor.ofSlot(0);
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
            this.slot = SlotAnchor.ofSlot(slot);
            return this;
        }

        public Builder slot(Slot slot) {
            this.slot = slot;
            return this;
        }

        public Builder withGlint() {
            this.glint = true;
            return this;
        }

        public Builder withGlint(boolean glint) {
            this.glint = glint;
            return this;
        }

        public Builder lore(@NotNull Component lore) {
            if (!lore.equals(Component.empty()))
                this.lore.add(lore.decoration(TextDecoration.ITALIC, false));

            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder lore(@NotNull Supplier<Object> supplier) {
            Object object = supplier.get();

            if (object instanceof Component component)
                return this.lore(component);
            else if (object instanceof List<?> list)
                return this.lore((List<Component>) list);

            throw new IllegalArgumentException("Invalid lore class type: " + object.getClass().getName());
        }

        public Builder lore(@NotNull List<Component> lore) {
            lore.forEach(this::lore);
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
                meta.getPersistentDataContainer().set(PDC_KEY, PersistentDataType.BYTE, (byte) 1);

				meta.displayName(this.name);
				meta.lore(lore);

                if (meta instanceof SkullMeta skullMeta && this.ownerUUID != null)
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(this.ownerUUID));

                if (glint) {
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
                }

                itemStack.setItemMeta(meta);
            }

            MenuItem item = new MenuItem(itemStack, slot);
            item.addActions(this.actions);

            return item;
        }
    }
}
