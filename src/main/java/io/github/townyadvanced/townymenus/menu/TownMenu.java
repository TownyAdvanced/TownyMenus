package io.github.townyadvanced.townymenus.menu;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeCache;
import com.palmergames.bukkit.towny.object.TownBlockTypeCache.CacheType;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.TransactionType;
import com.palmergames.bukkit.towny.object.economy.BankTransaction;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import io.github.townyadvanced.townymenus.gui.MenuHelper;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import io.github.townyadvanced.townymenus.gui.anchor.HorizontalAnchor;
import io.github.townyadvanced.townymenus.gui.anchor.SlotAnchor;
import io.github.townyadvanced.townymenus.gui.anchor.VerticalAnchor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TownMenu {
    public static MenuInventory createTownMenu(@NotNull Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);
        Town town = resident != null ? resident.getTownOrNull() : null;

        return MenuInventory.builder()
                .rows(6)
                .title(Component.text("Town Menu - " + (town != null ? town.getName() : "No Town")))
                .addItem(MenuItem.builder(Material.WRITABLE_BOOK)
                        .name(Component.text("Transaction History", NamedTextColor.GREEN))
                        .slot(0)
                        .lore(() -> {
                            if (town == null)
                                return Component.text("You are not part of a town.", NamedTextColor.GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_BANKHISTORY.getNode()))
                                return Component.text("You do not have permission to view the town's transaction history.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to view the town's transaction history.", NamedTextColor.GRAY);
                        })
                        .action(town != null && player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_BANKHISTORY.getNode())
                            ? ClickAction.openInventory(() -> createBankHistoryMenu(town)) : ClickAction.NONE)
                        .build())
                .addItem(MenuItem.builder(Material.GRASS_BLOCK)
                        .name(Component.text("Town Plots", NamedTextColor.GREEN))
                        .slot(1)
                        .lore(() -> {
                            if (town == null)
                                return Component.text("You are not part of a town.", NamedTextColor.GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_PLOTS.getNode()))
                                return Component.text("You do not have permission to view the town's plots.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to view the town's plots.", NamedTextColor.GRAY);
                        })
                        .action(town == null || !player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_PLOTS.getNode()) ? ClickAction.NONE : ClickAction.openInventory(() -> {
                            if (!TownyUniverse.getInstance().hasTown(town.getUUID()))
                                return MenuInventory.paginator().title(Component.text("Town Plots")).build();

                            List<MenuItem> plotItems = new ArrayList<>();
                            TownBlockTypeCache cache = town.getTownBlockTypeCache();

                            for (TownBlockType type : TownBlockTypeHandler.getTypes().values()) {
                                int residentOwned = cache.getNumTownBlocks(type, CacheType.RESIDENTOWNED);

                                plotItems.add(MenuItem.builder(Material.GRASS_BLOCK)
                                        .name(Component.text(type.getFormattedName(), NamedTextColor.GREEN))
                                        .lore(Component.text("Resident Owned: ", NamedTextColor.DARK_GREEN).append(Component.text(residentOwned, NamedTextColor.GREEN)))
                                        .lore(Component.text("For Sale: ", NamedTextColor.DARK_GREEN).append(Component.text(cache.getNumTownBlocks(type, CacheType.FORSALE), NamedTextColor.GREEN)))
                                        .lore(Component.text("Total: ", NamedTextColor.DARK_GREEN).append(Component.text(cache.getNumTownBlocks(type, CacheType.ALL), NamedTextColor.GREEN)))
                                        .lore(!TownyEconomyHandler.isActive() ? Component.empty() : Component.text("Daily Revenue: ", NamedTextColor.DARK_GREEN).append(Component.text(TownyEconomyHandler.getFormattedBalance(residentOwned * type.getTax(town)), NamedTextColor.GREEN)))
                                        .build());
                            }

                            return MenuInventory.paginator()
                                    .addItems(plotItems)
                                    .addExtraItem(MenuItem.builder(Material.OAK_SIGN)
                                            .name(Component.text("Town Plots", NamedTextColor.DARK_GREEN))
                                            .slot(SlotAnchor.of(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromLeft(1)))
                                            .lore(Component.text("Town Size: ", NamedTextColor.DARK_GREEN).append(Component.text(town.getTownBlocks().size() + " / " + town.getMaxTownBlocksAsAString(), NamedTextColor.GREEN))
                                                    .append(town.hasUnlimitedClaims()
                                                            ? Component.empty()
                                                            : TownySettings.isSellingBonusBlocks(town) ?
                                                            Component.text(" [Bought: " + town.getPurchasedBlocks() + "/" + TownySettings.getMaxPurchasedBlocks(town) + "]", NamedTextColor.AQUA) :
                                                            Component.empty()
                                                                    .append(town.getBonusBlocks() > 0 ?
                                                                            Component.text(" [Bonus: " + town.getBonusBlocks() + "]", NamedTextColor.AQUA) :
                                                                            Component.empty())
                                                                    .append(TownySettings.getNationBonusBlocks(town) > 0 ?
                                                                            Component.text(" [NationBonus: " + TownySettings.getNationBonusBlocks(town) + "]", NamedTextColor.AQUA) :
                                                                            Component.empty())))
                                            .lore(Component.text("Town Owned Land: ", NamedTextColor.DARK_GREEN).append(Component.text(town.getTownBlocks().size() - cache.getNumberOfResidentOwnedTownBlocks(), NamedTextColor.GREEN)))
                                            .build())
                                    .title(Component.text("Town Plots"))
                                    .build();
                        }))
                        .build())
                .addItem(MenuItem.builder(Material.RED_BED)
                        .slot(2)
                        .name(Component.text("Town Spawn", NamedTextColor.GREEN))
                        .lore(() -> {
                            if (town == null)
                                return Component.text("You are not part of a town.", NamedTextColor.GRAY);
                            else if (!player.hasPermission("towny.town.spawn.town"))
                                return Component.text("You do not have permission to use this.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to teleport to your town's spawn.", NamedTextColor.GRAY);
                        })
                        .action(town == null || !player.hasPermission("towny.town.spawn.town") ? ClickAction.NONE : ClickAction.confirmation(() -> Component.text("Click to confirm using /town spawn.", NamedTextColor.GRAY), ClickAction.run(() -> {
                            if (!player.hasPermission("towny.town.spawn.town"))
                                return;

                            try {
                                TownCommand.townSpawn(player, new String[]{}, false, true);
                            } catch (TownyException e) {
                                TownyMessaging.sendErrorMsg(player, e.getMessage(player));
                            }

                            player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                        })))
                        .build())
                .addItem(MenuHelper.backButton().build())
                .build();
    }

    private static MenuInventory createBankHistoryMenu(Town town) {
        if (town == null || !TownyEconomyHandler.isActive() || !TownyUniverse.getInstance().hasTown(town.getUUID()))
            return MenuInventory.paginator().title(Component.text("Transaction History")).build();

        List<MenuItem> transactionItems = new ArrayList<>();
        List<BankTransaction> transactions = new ArrayList<>(town.getAccount().getAuditor().getTransactions());

        for (int i = 0; i < transactions.size(); i++) {
            BankTransaction transaction = transactions.get(i);
            boolean added = transaction.getType() == TransactionType.ADD || transaction.getType() == TransactionType.DEPOSIT;

            transactionItems.add(MenuItem.builder(added ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK)
                    .name(Component.text("Transaction #" + (i + 1) + " - " + transaction.getTime(), added ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .lore(Component.text("Amount: ", NamedTextColor.DARK_GREEN).append(Component.text(TownyEconomyHandler.getFormattedBalance(transaction.getAmount()), NamedTextColor.GREEN)))
                    .lore(Component.text("New balance: ", NamedTextColor.DARK_GREEN).append(Component.text(TownyEconomyHandler.getFormattedBalance(transaction.getBalance()), NamedTextColor.GREEN)))
                    .lore(Component.text("Reason: ", NamedTextColor.DARK_GREEN).append(Component.text(transaction.getReason(), NamedTextColor.GREEN)))
                    .build());
        }

        Collections.reverse(transactionItems);

        return MenuInventory.paginator().addItems(transactionItems).title(Component.text("Transaction History")).build();
    }
}
