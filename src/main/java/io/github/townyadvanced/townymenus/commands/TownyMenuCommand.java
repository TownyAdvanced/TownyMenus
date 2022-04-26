package io.github.townyadvanced.townymenus.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.utils.Time;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class TownyMenuCommand implements CommandExecutor, TabCompleter {
    private final TownyMenus plugin;

    public TownyMenuCommand(TownyMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return null;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player))
            return true;

        Resident resident = TownyAPI.getInstance().getResident(player);
        boolean hasTown = resident != null && resident.hasTown();
        boolean hasNation = resident != null && resident.hasNation();

        MenuInventory.builder()
                .rows(3)
                .title(Component.text("Towny Menu"))
                .addItem(MenuItem.builder(Material.EMERALD)
                        .name(Component.text("Town Settings", NamedTextColor.GREEN))
                        .lore(hasTown ? Component.text("Click to view town commands!", NamedTextColor.GRAY) :
                                Component.text("✖ You are not a member of a town.", NamedTextColor.RED))
                        .slot(10)
                        .action(hasTown ? ClickAction.close() : ClickAction.EMPTY)
                        .build())
                .addItem(MenuItem.builder(Material.DIAMOND)
                        .name(Component.text("Nation Settings", NamedTextColor.AQUA))
                        .lore(hasNation ? Component.text("Click to view nation commands!", NamedTextColor.GRAY) :
                                Component.text("✖ You are not a member of a nation.", NamedTextColor.RED))
                        .slot(12)
                        .action(hasNation ? ClickAction.close() : ClickAction.EMPTY)
                        .build())
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .name(Component.text("Plot Settings", NamedTextColor.DARK_GREEN))
                        .slot(14)
                        .action(ClickAction.close())
                        .build())
                .addItem(MenuItem.builder(Material.PLAYER_HEAD)
                        .skullOwner(player.getUniqueId())
                        .name(Component.text("Resident Settings", NamedTextColor.YELLOW))
                        .slot(16)
                        .action(ClickAction.openInventory(createResidentMenu(player)))
                        .build())
                .build()
                .open(player);

        return true;
    }

    private Supplier<MenuInventory> createResidentMenu(@NotNull Player player) {
        return () -> MenuInventory.builder()
                .rows(3)
                .title(Component.text("Resident Menu"))
                .addItem(formatResidentInfo(player.getUniqueId(), 13))
                .addItem(MenuItem.builder(Material.RED_BED)
                        .name(Component.text("Spawn", NamedTextColor.GREEN))
                        .lore(player.hasPermission(PermissionNodes.TOWNY_COMMAND_RESIDENT_SPAWN.getNode())
                                ? Component.text("Click to teleport to your spawn!", NamedTextColor.GRAY)
                                : Component.text("✖ You do not have enough permissions to use this!", NamedTextColor.RED))
                        .action(ClickAction.confirmation(ClickAction.run(() -> {
                            if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_RESIDENT_SPAWN.getNode()))
                                return;

                            Resident resident = TownyAPI.getInstance().getResident(player);
                            if (resident == null)
                                return;

                            try {
                                SpawnUtil.sendToTownySpawn(player, new String[]{}, resident, Translatable.of("msg_err_cant_afford_tp").forLocale(player), false, true, SpawnType.RESIDENT);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }
                        })))
                        .slot(15)
                        .build())
                .addItem(MenuHelper.backButton()
                        .slot(26)
                        .build())
                .build();
    }

    /**
     * @param uuid The uuid of the resident
     * @return A formatted menu item
     */
    private MenuItem formatResidentInfo(@NotNull UUID uuid, int slot) {
        Resident resident = TownyAPI.getInstance().getResident(uuid);

        if (resident == null)
            return MenuItem.builder(Material.PLAYER_HEAD)
                    .skullOwner(uuid)
                    .name(Component.text("Error", NamedTextColor.DARK_RED))
                    .lore(Component.text("Unknown or invalid resident.", NamedTextColor.RED))
                    .build();

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Status: ", NamedTextColor.DARK_GREEN).append(resident.isOnline() ? Component.text("● Online", NamedTextColor.GREEN) : Component.text("● Offline", NamedTextColor.RED)));

        if (resident.hasTown()) {
            lore.add(Component.text(resident.isMayor() ? "Mayor" : "Resident" + " of ", NamedTextColor.DARK_GREEN).append(Component.text(resident.getTownOrNull().getName(), NamedTextColor.GREEN)));

            if (resident.hasNation())
                lore.add(Component.text(resident.isKing() ? "Leader" : "Member" + " of ", NamedTextColor.DARK_GREEN).append(Component.text(resident.getNationOrNull().getName(), NamedTextColor.GREEN)));
        }

        lore.add(Component.text("Registered ", NamedTextColor.DARK_GREEN).append(Component.text(Time.formatRegistered(resident.getRegistered()), NamedTextColor.GREEN)));

        if (!resident.isOnline())
            lore.add(Component.text("Last online ", NamedTextColor.DARK_GREEN).append(Component.text(Time.formatLastOnline(resident.getLastOnline()), NamedTextColor.GREEN)));

        return MenuItem.builder(Material.PLAYER_HEAD)
                .skullOwner(uuid)
                .name(Component.text(resident.getName(), resident.isOnline() ? NamedTextColor.GREEN : NamedTextColor.YELLOW))
                .lore(lore)
                .slot(slot)
                .build();
    }
}
