package io.github.townyadvanced.townymenus;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TranslationLoader;
import com.palmergames.bukkit.util.Version;
import io.github.townyadvanced.townymenus.commands.TownyMenuCommand;
import io.github.townyadvanced.townymenus.listeners.InventoryListener;
import io.github.townyadvanced.townymenus.settings.MenuSettings;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public class TownyMenus extends JavaPlugin {

	private static final Version requiredTownyVersion = Version.fromString("0.98.1.0");
	private static TownyMenus plugin;

	@Override
	public void onEnable() {
		plugin = this;

		if (!townyVersionCheck()) {
			getLogger().severe("Towny version does not meet required minimum version: " + requiredTownyVersion);
			plugin.setEnabled(false);
			return;
		} else {
			getLogger().info("Towny version " + Towny.getPlugin().getVersion() + " found.");
		}

		MenuSettings.loadConfig();

		Bukkit.getPluginManager().registerEvents(new InventoryListener(this), this);
		Bukkit.getPluginCommand("townymenu").setExecutor(new TownyMenuCommand(this));

		logger().info("Loading translations...");
		TranslationLoader loader = new TranslationLoader(getDataFolder().toPath().resolve("lang"), this, TownyMenus.class);
		loader.load();
		TownyAPI.getInstance().addTranslations(this, loader.getTranslations());
	}

	public static TownyMenus getPlugin() {
		return plugin;
	}

	public static Logger logger() {
		return plugin.getSLF4JLogger();
	}

	public String getVersion() {
		return this.getDescription().getVersion();
	}

	private boolean townyVersionCheck() {
		return Version.fromString(Towny.getPlugin().getVersion()).compareTo(requiredTownyVersion) >= 0;
	}
}
