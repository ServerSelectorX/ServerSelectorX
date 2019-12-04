package xyz.derkades.serverselectorx;

import java.util.HashMap;
import java.util.Map;

import org.bstats.bukkit.Metrics;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import xyz.derkades.serverselectorx.placeholders.PapiEnabled;

public class Stats {

	public static void initialize() {
		final Metrics metrics = new Metrics(Main.getPlugin());

		metrics.addCustomChart(new Metrics.SimplePie("placeholderapi", () -> Main.PLACEHOLDER_API instanceof PapiEnabled ? "yes" : "no"));

		metrics.addCustomChart(new Metrics.SimplePie("number_of_selectors", () -> {
			return Main.getConfigurationManager().getAll().size() + "";
		}));

		metrics.addCustomChart(new Metrics.AdvancedPie("selector_item", () -> {
			final Map<String, Integer> map = new HashMap<>();

			for (final FileConfiguration config : Main.getConfigurationManager().getAll()) {
				final Material material = Material.getMaterial(config.getString("item"));
				if (material == null)
				 {
					continue; //Do not count invalid items
				}

				if (map.containsKey(material.toString())) {
					map.put(material.toString(), map.get(material.toString() + 1));
				} else {
					map.put(material.toString(), 1);
				}
			}

			return map;
		}));

		metrics.addCustomChart(new Metrics.AdvancedPie("type", () -> {
			final Map<String, Integer> map = new HashMap<>();

			for (final FileConfiguration config : Main.getConfigurationManager().getAll()) {
				for (final String slot : config.getConfigurationSection("menu").getKeys(false)) {
					final String action = config.getString("menu." + slot + ".action").split(":")[0].toLowerCase();
					if (map.containsKey(action)) {
						map.put(action, map.get(action) + 1);
					} else {
						map.put(action, 1);
					}
				}
			}

			return map;
		}));

		metrics.addCustomChart(new Metrics.AdvancedPie("server_pinging", () -> {
			final Map<String, Integer> map = new HashMap<>();

			for (final FileConfiguration config : Main.getConfigurationManager().getAll()) {
				for (final String slot : config.getConfigurationSection("menu").getKeys(false)) {
					if (config.getBoolean("menu." + slot + ".ping-server", false)) {
						if (map.containsKey("Enabled")) {
							map.put("Enabled", map.get("Enabled") + 1);
						} else {
							map.put("Enabled", 1);
						}
					} else {
						if (map.containsKey("Disabled")) {
							map.put("Disabled", map.get("Disabled") + 1);
						} else {
							map.put("Disabled", 1);
						}
					}
				}
			}

			return map;
		}));

		metrics.addCustomChart(new Metrics.SimplePie("permissions", () -> {
			return "Unavailable";
		}));

		metrics.addCustomChart(new Metrics.SimplePie("item_drop", () -> {
			if (Main.getPlugin().getConfig().getBoolean("cancel-item-drop", false)) {
				return "Cancel";
			} else {
				return "Allow";
			}
		}));

		metrics.addCustomChart(new Metrics.SimplePie("item_move", () -> {
			if (Main.getPlugin().getConfig().getBoolean("cancel-item-move", false)) {
				return "Cancel";
			} else {
				return "Allow";
			}
		}));

		metrics.addCustomChart(new Metrics.AdvancedPie("menu_item_slot", () -> {
			final Map<String, Integer> map = new HashMap<>();

			for (final FileConfiguration config : Main.getConfigurationManager().getAll()) {
				final int slot = config.getInt("inv-slot", 0);
				if (slot < 0) {
					if (map.containsKey("Auto")) {
						map.put("Auto", map.get("Auto") + 1);
					} else {
						map.put("Auto", 1);
					}
				} else {
					if (map.containsKey(slot + "")) {
						map.put(slot + "", map.get(slot + "") + 1);
					} else {
						map.put(slot + "", 1);
					}
				}
			}

			return map;
		}));

		metrics.addCustomChart(new Metrics.AdvancedPie("rows", () -> {
			final Map<String, Integer> map = new HashMap<>();

			for (final FileConfiguration config : Main.getConfigurationManager().getAll()) {
				final int rows = config.getInt("rows", 6);
				if (map.containsKey(rows + "")) {
					map.put(rows + "", map.get(rows + "") + 1);
				} else {
					map.put(rows + "", 1);
				}
			}

			return map;
		}));
	}

}
