package io.github.townyadvanced.townymenus;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.object.TranslationLoader;
import com.palmergames.bukkit.util.Version;
import io.github.townyadvanced.townymenus.commands.TownyMenuCommand;
import io.github.townyadvanced.townymenus.commands.addon.NationAddonCommand;
import io.github.townyadvanced.townymenus.commands.addon.PlotAddonCommand;
import io.github.townyadvanced.townymenus.commands.addon.ResidentAddonCommand;
import io.github.townyadvanced.townymenus.commands.addon.TownAddonCommand;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.listeners.InventoryListener;
import io.github.townyadvanced.townymenus.listeners.PlayerListener;
import io.github.townyadvanced.townymenus.settings.MenuSettings;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class TownyMenus extends JavaPlugin {

	private static final Version requiredTownyVersion = Version.fromString("0.98.3.8");
	private static TownyMenus plugin;

	@Override
	public void onEnable() {
		plugin = this;

		if (!townyVersionCheck()) {
			getLogger().severe("Towny version does not meet required minimum version: " + requiredTownyVersion);
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		} else {
			getLogger().info("Towny version " + Towny.getPlugin().getVersion() + " found.");
		}

		MenuSettings.loadConfig();

		Bukkit.getPluginManager().registerEvents(new InventoryListener(this), this);
		Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);

		TownyMenuCommand townyMenuCommand = new TownyMenuCommand(this);

		getCommand("townymenu").setExecutor(townyMenuCommand);
		TownyCommandAddonAPI.addSubCommand(CommandType.TOWNY, "menu", townyMenuCommand);

		TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "menu", new NationAddonCommand());
		TownyCommandAddonAPI.addSubCommand(CommandType.PLOT, "menu", new PlotAddonCommand());
		TownyCommandAddonAPI.addSubCommand(CommandType.RESIDENT, "menu", new ResidentAddonCommand());
		TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "menu", new TownAddonCommand());

		logger().info("Loading translations...");
		TranslationLoader loader = new TranslationLoader(getDataFolder().toPath().resolve("lang"), this, TownyMenus.class);
		loader.load();
		TownyAPI.getInstance().addTranslations(this, loader.getTranslations());
	}

	@Override
	public void onDisable() {
		// Close any open menu inventories
		for (Player player : Bukkit.getOnlinePlayers())
			if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuInventory)
				player.closeInventory();

		TownyCommandAddonAPI.removeSubCommand(CommandType.NATION, "menu");
		TownyCommandAddonAPI.removeSubCommand(CommandType.PLOT, "menu");
		TownyCommandAddonAPI.removeSubCommand(CommandType.RESIDENT, "menu");
		TownyCommandAddonAPI.removeSubCommand(CommandType.TOWN, "menu");
	}

	public static TownyMenus getPlugin() {
		return plugin;
	}

	public static Logger logger() {
		return plugin.getLogger();
	}

	public String getVersion() {
		return this.getDescription().getVersion();
	}

	private boolean townyVersionCheck() {
		return Version.fromString(Towny.getPlugin().getVersion()).compareTo(requiredTownyVersion) >= 0;
	}
}
