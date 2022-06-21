package io.github.townyadvanced.townymenus.menu;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Transaction;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class TownMenu {
    public static Supplier<MenuInventory> createTownMenu(@NotNull Player player) {
        Resident resident = TownyAPI.getInstance().getResident(player);
        Town town = resident != null ? resident.getTownOrNull() : null;

        return () -> MenuInventory.builder()
                .rows(6)
                .title(Component.text("Town Menu - " + (town != null ? town.getName() : "No Town")))
                .addItem(MenuItem.builder(Material.WRITABLE_BOOK)
                        .name(Component.text("Transaction History", NamedTextColor.GREEN))
                        .lore(() -> {
                            if (town == null)
                                return Component.text("You are not part of a town.", NamedTextColor.GRAY);
                            else if (!TownyEconomyHandler.isActive())
                                return Component.text("Economy has not been turned on.", NamedTextColor.GRAY);
                            else if (!player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_BANKHISTORY.getNode()))
                                return Component.text("You do not have permission to view the town's transaction history.", NamedTextColor.GRAY);
                            else
                                return Component.text("Click to view the town's transaction history.", NamedTextColor.GRAY);
                        })
                        .action(town != null && TownyEconomyHandler.isActive() && player.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_BANKHISTORY.getNode())
                            ? ClickAction.openInventory(() -> createBankHistoryMenu(player, town)) : ClickAction.NONE)
                        .build())
                .addItem(MenuHelper.backButton().slot(SlotAnchor.of(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromRight(0))).build())
                .build();
    }

    private static MenuInventory createBankHistoryMenu(Player player, Town town) {
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

        return MenuInventory.paginator()
                .addItems(transactionItems)
                .title(Component.text("Transaction History"))
                .build();
    }
}
