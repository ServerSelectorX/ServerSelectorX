package xyz.derkades.serverselectorx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import de.tr7zw.changeme.nbtapi.NBTItem;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.ItemBuilder;
import xyz.derkades.derkutils.bukkit.menu.IconMenu;
import xyz.derkades.derkutils.bukkit.menu.MenuCloseEvent;
import xyz.derkades.derkutils.bukkit.menu.OptionClickEvent;
import xyz.derkades.serverselectorx.actions.Action;
import xyz.derkades.serverselectorx.placeholders.GlobalPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Placeholder;
import xyz.derkades.serverselectorx.placeholders.PlayerPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Server;

public class Menu extends IconMenu {

	private final FileConfiguration config;

	private final BukkitTask refreshTimer;

	public Menu(final Player player, final FileConfiguration config, final String configName) {
		super(Main.getPlugin(), Colors.parseColors(config.getString("title", UUID.randomUUID().toString())), config.getInt("rows", 6), player);

		this.config = config;

		this.refreshTimer = Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), () -> {
			final long start = System.nanoTime();
			this.addItems();
			if (Main.LAG_DEBUG) {
				final long diff = System.nanoTime() - start;
				System.out.println("(Re)loaded menu for player " + this.player.getName() + " in " + diff / 1000 + "μs. (one tick is 50ms)");
			}
		}, 0, 1*20);
	}

	private void addItems() {
		if (this.config == null) {
			this.player.sendMessage("The configuration file failed to load, probably due to a syntax error.");
			this.player.sendMessage("Take a look at the console for any YAML errors, or paste your config in http://www.yamllint.com/");
			this.player.sendMessage("Check for identation and balanced quotes. If you want to use quotation marks in strings, they must be escaped properly by putting two quotation marks (for example \"\" or '').");
			return;
		}

		itemLoop:
		for (final String key : this.config.getConfigurationSection("menu").getKeys(false)) {
			final ConfigurationSection section = this.config.getConfigurationSection("menu." + key);

			List<String> actions;
			ItemBuilder builder;

			if (section.contains("permission") && !this.player.hasPermission(section.getString("permission"))) {
				// Use no-permission section
				builder = Main.getItemBuilderFromItemSection(this.player, section.getConfigurationSection("no-permission"));
				actions = section.getConfigurationSection("no-permission").getStringList("actions");
			} else {
				// Player has permission, use other sections
				if (section.contains("connector")) {
					// Advanced server section. Use online, offline, dynamic sections.
					final String serverName = section.getString("connector");
					final Server server = Server.getServer(serverName);

					if (server != null && server.isOnline()) {
						builder = null;
						actions = null;

						if (section.contains("dynamic")) {
							for (final String dynamicKey : section.getConfigurationSection("dynamic").getKeys(false)) {
								final String placeholderKeyInConfig = dynamicKey.split(":")[0];
								final String placeholderValueInConfig = dynamicKey.split(":")[1];

								final Placeholder placeholder = server.getPlaceholder(placeholderKeyInConfig);
								if (placeholder == null) {
									Main.getPlugin().getLogger().warning("Dynamic feature contains rule with placeholder " + placeholder + " which has not been received from the server.");
									continue;
								}

								final String placeholderValueFromConnector;

								if (placeholder instanceof GlobalPlaceholder) {
									final GlobalPlaceholder global = (GlobalPlaceholder) placeholder;
									placeholderValueFromConnector = global.getValue();
								} else {
									final PlayerPlaceholder playerPlaceholder = (PlayerPlaceholder) placeholder;
									placeholderValueFromConnector = playerPlaceholder.getValue(this.player);
								}

								final ConfigurationSection dynamicSection = section.getConfigurationSection("dynamic." + dynamicKey);

								final String mode = dynamicSection.getString("mode", "equals");

								if (
										mode.equals("equals") && placeholderValueInConfig.equals(placeholderValueFromConnector) ||
										mode.equals("less") && Double.parseDouble(placeholderValueInConfig) < Double.parseDouble(placeholderValueFromConnector) ||
										mode.equals("more") && Double.parseDouble(placeholderValueInConfig) > Double.parseDouble(placeholderValueFromConnector)
										) {

									if (!dynamicSection.contains("material")) {
										this.player.sendMessage("Dynamic section '" + dynamicKey + "' is missing the material option");
										return;
									}

									if (dynamicSection.getString("material").equalsIgnoreCase("NONE")) {
										continue itemLoop;
									}

									builder = Main.getItemBuilderFromItemSection(this.player, dynamicSection);
									actions = dynamicSection.getStringList("actions");
									break;
								}
							}
						}

						if (builder == null) {
							//No dynamic rule matched, fall back to online
							if (!section.isConfigurationSection("online")) {
								this.player.sendMessage("Error for item " + key);
								this.player.sendMessage("Online section does not exist");
								return;
							}

							final ConfigurationSection onlineSection = section.getConfigurationSection("online");

							if (!onlineSection.contains("material")) {
								this.player.sendMessage("Error for item " + key);
								this.player.sendMessage("Online section does not have a material option");
							}

							if (onlineSection.getString("material").equalsIgnoreCase("NONE")) {
								continue itemLoop;
							}

							builder = Main.getItemBuilderFromItemSection(this.player, onlineSection);
							actions = onlineSection.getStringList("actions");
						}

						final Map<String, String> placeholders = new HashMap<>();

						// Add global placeholders to list (uuid = null)
						for (final Placeholder placeholder : server.getPlaceholders()) {
							final String value;
							if (placeholder instanceof GlobalPlaceholder) {
								final GlobalPlaceholder global = (GlobalPlaceholder) placeholder;
								value = global.getValue();
							} else {
								final PlayerPlaceholder playerPlaceholder = (PlayerPlaceholder) placeholder;
								value = playerPlaceholder.getValue(this.player);
							}
							placeholders.put("{" + placeholder.getKey() + "}", value);
						}

						// Now parse the collected placeholders and papi placeholders in name and lore
						builder.namePlaceholders(placeholders).lorePlaceholders(placeholders);

						// Set item amount if dynamic item count is enabled
						if (section.getBoolean("dynamic-item-count", false)) {
							final int amount = server.getOnlinePlayers();
							builder.amount(amount < 1 || amount > 64 ? 1 : amount);
						}
					} else {
						//Server is offline
						if (!section.isConfigurationSection("offline")) {
							this.player.sendMessage("Offline section does not exist");
							return;
						}

						final ConfigurationSection offlineSection = section.getConfigurationSection("offline");

						if (!offlineSection.contains("material")) {
							this.player.sendMessage("Error for item " + key);
							this.player.sendMessage("Offline section does not have a material option");
						}

						if (offlineSection.getString("material").equalsIgnoreCase("NONE")) {
							continue;
						}

						builder = Main.getItemBuilderFromItemSection(this.player, offlineSection);
						actions = offlineSection.getStringList("actions");
					}
				} else {
					// Simple section
					if (section.getString("material") == null) {
						this.player.sendMessage("Error for item " + key);
						this.player.sendMessage("Missing material option. Remember, this is not an advanced section, so don't use online/offline/dynamic sections in the config.");
						this.player.sendMessage("If you want to use these sections, add a connector option.");
						this.player.sendMessage("Read more here: https://github.com/ServerSelectorX/ServerSelectorX/wiki/Menu-items-v2");
					}

					if (section.getString("material").equalsIgnoreCase("NONE")) {
						continue itemLoop;
					}

					builder = Main.getItemBuilderFromItemSection(this.player, section);
					actions = section.getStringList("actions");
				}
			}

			builder.papi(this.player)
				.placeholder("{player}", this.player.getName())
				.placeholder("{globalOnline}", Main.getGlobalPlayerCount() + "");

			// Add actions to item as NBT
			final NBTItem nbt = new NBTItem(builder.create());
			nbt.setObject("SSXActions", actions);

			final ItemStack item = nbt.getItem();

			Main.illegalItems.setIllegal(item, true);

			final int slot = Integer.valueOf(key);

			if (slot >= this.getInventory().getSize()) {
				this.player.sendMessage("You put an item in slot " + slot + ", which is higher than the maximum number of slots in your menu.");
				this.player.sendMessage("Increase the number of rows in the config");
				return;
			}

			if (slot < 0) {
				// Slot is set to -1 (or other negative value), fill all blank slots
				for (int i = 0; i < this.getInventory().getSize(); i++) {
					if (!this.hasItem(i)) {
						this.addItem(i, item);
					}
				}
			} else {
				// Slot is set to a positive value, put item in slot.
				this.addItem(slot, item);
			}
		}
	}

	@Override
	public boolean onOptionClick(final OptionClickEvent event) {
		final Player player = event.getPlayer();

		final NBTItem nbt = new NBTItem(event.getItemStack());

		@SuppressWarnings("unchecked")
		final List<String> actions = nbt.getObject("SSXActions", List.class);

		if (actions.isEmpty()) {
			return false;
		}

		return Action.runActions(player, actions);
	}

	@Override
	public void onClose(final MenuCloseEvent event) {
		if (this.refreshTimer != null) {
			this.refreshTimer.cancel();
		}
	}

}
