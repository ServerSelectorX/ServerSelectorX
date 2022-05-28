package xyz.derkades.serverselectorx;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class Stats extends Metrics {

	public Stats() {
		super(Main.getPlugin(), 1061);

		this.addCustomChart(new SimplePie("placeholderapi", () -> {
			if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
				return "yes";
			} else {
				return "no";
			}
		}));

		this.addCustomChart(new SimplePie("number_of_selectors", () -> Main.getConfigurationManager().listMenuConfigurations().size() + ""));

		// TODO update for conditionals
//		this.addCustomChart(new AdvancedPie("selector_item", () -> {
//			final Map<String, Integer> map = new HashMap<>();
//
//			for (final String itemName : Main.getConfigurationManager().listItemConfigurations()) {
//				final FileConfiguration item = Main.getConfigurationManager().getItemConfiguration(itemName);
//				final Material material = Material.getMaterial(item.getString("item.material"));
//				if (material == null) {
//					continue; // Do not count invalid items
//				}
//
//				if (map.containsKey(material.toString())) {
//					map.put(material.toString(), map.get(material.toString() + 1));
//				} else {
//					map.put(material.toString(), 1);
//				}
//			}
//
//			return map;
//		}));

//		this.addCustomChart(new Metrics.AdvancedPie("type", () -> {
//			final Map<String, Integer> map = new HashMap<>();
//
//			for (final Map.Entry<String, FileConfiguration> menuConfigEntry : menuConfigs.entrySet()) {
//				final FileConfiguration config = menuConfigEntry.getValue();
//
//				for (final String slot : config.getConfigurationSection("menu").getKeys(false)) {
//					final String action = config.getString("menu." + slot + ".action").split(":")[0].toLowerCase();
//					if (map.containsKey(action)) {
//						map.put(action, map.get(action) + 1);
//					} else {
//						map.put(action, 1);
//					}
//				}
//			}
//
//			return map;
//		}));

		this.addCustomChart(new SimplePie("ping_api", () -> "Premium"));

		this.addCustomChart(new SimplePie("updater", () -> "Unavailable"));

		this.addCustomChart(new SimplePie("player_count_mode", () ->
				Main.getPlugin().getConfig().getString("item-count-mode", "absolute").toLowerCase()));

		this.addCustomChart(new SimplePie("item_drop", () -> {
			if (Main.getPlugin().getConfig().getBoolean("cancel-item-drop", false)) {
				return "Cancel";
			} else {
				return "Allow";
			}
		}));

		this.addCustomChart(new SimplePie("item_move", () -> {
			if (Main.getPlugin().getConfig().getBoolean("cancel-item-move", false)) {
				return "Cancel";
			} else {
				return "Allow";
			}
		}));

		this.addCustomChart(new AdvancedPie("menu_item_slot", () -> {
			final Map<String, Integer> map = new HashMap<>();

			for (final String itemName : Main.getConfigurationManager().listItemConfigurations()) {
				final FileConfiguration item = Main.getConfigurationManager().getItemConfiguration(itemName);
				if (!item.getBoolean("give.join")) {
					if (map.containsKey("Disabled")) {
						map.put("Disabled", map.get("Disabled") + 1);
					} else {
						map.put("Disabled", 1);
					}
				} else {
					final int slot = item.getInt("give.inv-slot", 0);
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
			}

			return map;
		}));

		this.addCustomChart(new AdvancedPie("rows", () -> {
			final Map<String, Integer> map = new HashMap<>();

			for (final String menuName : Main.getConfigurationManager().listMenuConfigurations()) {
				final FileConfiguration menu = Main.getConfigurationManager().getMenuConfiguration(menuName);
				final int rows = menu.getInt("rows", 1);
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
