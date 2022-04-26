package io.github.townyadvanced.townymenus.settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.palmergames.bukkit.config.CommentedConfiguration;
import io.github.townyadvanced.townymenus.TownyMenus;

public class MenuSettings {
	private static CommentedConfiguration config, newConfig;

	public static void loadConfig() {
		final Path path = TownyMenus.getPlugin().getDataFolder().toPath().resolve("config.yml");

		if (!Files.exists(path)) {
			try {
				Files.createDirectories(path.getParent());
				Files.createFile(path);
			} catch (IOException e) {
				TownyMenus.logger().error("Failed to create config.yml!", e);
			}
		}

		// read the config.yml into memory
		config = new CommentedConfiguration(path);
		if (!config.load())
			TownyMenus.logger().error("Failed to load config.yml!");

		setDefaults(path);
		config.save();
	}

	public static void addComment(String root, String... comments) {
		newConfig.addComment(root.toLowerCase(), comments);
	}

	private static void setNewProperty(String root, Object value) {
		if (value == null)
			value = "";

		newConfig.set(root.toLowerCase(), value.toString());
	}

	private static void setProperty(String root, Object value) {
		config.set(root.toLowerCase(), value.toString());
	}
	
	/**
	 * Builds a new config reading old config data.
	 */
	private static void setDefaults(Path configPath) {

		newConfig = new CommentedConfiguration(configPath);
		newConfig.load();

		for (ConfigNodes root : ConfigNodes.values()) {
			if (root.getComments().length > 0)
				addComment(root.getRoot(), root.getComments());

			if (root == ConfigNodes.VERSION)
				setNewProperty(root.getRoot(), TownyMenus.getPlugin().getVersion());
			else
				setNewProperty(root.getRoot(), (config.get(root.getRoot().toLowerCase()) != null) ? config.get(root.getRoot().toLowerCase()) : root.getDefault());
		}

		config = newConfig;
		newConfig = null;
	}
	
	public static String getString(String root, String def) {
		String data = config.getString(root.toLowerCase(), def);

		if (data == null) {
			TownyMenus.logger().error("Failed to read " + root.toLowerCase() + " from config.yml");
			return "";
		}

		return data;
	}
	
	public static boolean getBoolean(ConfigNodes node) {
		return Boolean.parseBoolean(config.getString(node.getRoot().toLowerCase(), node.getDefault()));
	}

	public static double getDouble(ConfigNodes node) {
		try {
			return Double.parseDouble(config.getString(node.getRoot().toLowerCase(), node.getDefault()).trim());
		} catch (NumberFormatException e) {
			TownyMenus.logger().error("Failed to read " + node.getRoot().toLowerCase() + " from config.yml");
			return 0.0;
		}
	}

	public static int getInt(ConfigNodes node) {
		try {
			return Integer.parseInt(config.getString(node.getRoot().toLowerCase(), node.getDefault()).trim());
		} catch (NumberFormatException e) {
			TownyMenus.logger().error("Failed to read " + node.getRoot().toLowerCase() + " from config.yml");
			return 0;
		}
	}

	public static String getString(ConfigNodes node) {
		return config.getString(node.getRoot().toLowerCase(), node.getDefault());
	}
}
