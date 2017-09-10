package xyz.derkades.serverselectorx;

import java.util.HashMap;
import java.util.Map;

import org.bstats.bukkit.Metrics;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import xyz.derkades.serverselectorx.placeholders.PlaceholdersEnabled;

public class Stats {
	
	public static void initialize() {
		final Metrics metrics = new Metrics(Main.getPlugin());
		
		metrics.addCustomChart(new Metrics.SimplePie("placeholderapi", () -> {
			if (Main.PLACEHOLDER_API instanceof PlaceholdersEnabled) {
				return "yes";
			} else {
				return "no";
			}
		}));
		
		metrics.addCustomChart(new Metrics.SimplePie("number_of_selectors", () -> {
			return Main.getServerSelectorConfigurationFiles().size() + "";
		}));
		
		metrics.addCustomChart(new Metrics.AdvancedPie("selector_item", () -> {
			final Map<String, Integer> map = new HashMap<>();
			
			for (final FileConfiguration config : Main.getServerSelectorConfigurationFiles()) {
				final Material material = Material.getMaterial(config.getString("item"));
				if (material == null) continue; //Do not count invalid items
				
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
			
			for (final FileConfiguration config : Main.getServerSelectorConfigurationFiles()) {
				for (final String slot : config.getConfigurationSection("menu").getKeys(false)) {
					String action = config.getString("menu." + slot + ".action").split(":")[0].toLowerCase();
					if (map.containsKey(action)) {
						map.put(action, map.get(action) + 1);
					} else {
						map.put(action, 1);
					}
				}
			}
			
			return map;
		}));
		
		metrics.addCustomChart(new Metrics.SimplePie("ping_api", () -> {
			if (Main.getPlugin().getConfig().getBoolean("external-query", true)) {
				return "External";
			} else {
				return "Internal";
			}
		}));
		
		metrics.addCustomChart(new Metrics.SimplePie("updater", () -> {
			if (Main.getPlugin().getConfig().getBoolean("updater", true)) {
				return "Enabled";
			} else {
				return "Disabled";
			}
		}));
		
		metrics.addCustomChart(new Metrics.SimplePie("player_count_mode", () -> {
			return Main.getPlugin().getConfig().getString("item-count-mode", "absolute").toLowerCase();
		}));
		
		metrics.addCustomChart(new Metrics.SimplePie("background_pinging", () -> {
			if (Main.getPlugin().getConfig().getBoolean("background-pinging", true)) {
				return "Enabled";
			} else {
				return "Disabled";
			}
		}));
		
		metrics.addCustomChart(new Metrics.AdvancedPie("server_pinging", () -> {
			final Map<String, Integer> map = new HashMap<>();
			
			for (final FileConfiguration config : Main.getServerSelectorConfigurationFiles()) {
				for (final String slot : config.getConfigurationSection("menu").getKeys(false)) {
					String action = config.getString("menu." + slot + ".action").split(":")[0].toLowerCase();
					if (action.equalsIgnoreCase("srv")) {
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
			}
			
			return map;
		}));
		
		metrics.addCustomChart(new Metrics.SimplePie("permissions", () -> {
			if (Main.getPlugin().getConfig().getBoolean("permissions-enabled", false)) {
				return "Enabled";
			} else {
				return "Disabled";
			}
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
			
			for (final FileConfiguration config : Main.getServerSelectorConfigurationFiles()) {
				int slot = config.getInt("inv-slot", 0);
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
			
			for (final FileConfiguration config : Main.getServerSelectorConfigurationFiles()) {
				int rows = config.getInt("rows", 6);
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
