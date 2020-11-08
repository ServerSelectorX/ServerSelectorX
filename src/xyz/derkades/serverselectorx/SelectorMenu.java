package xyz.derkades.serverselectorx;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import xyz.derkades.derkutils.ListUtils;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.ItemBuilder;
import xyz.derkades.derkutils.bukkit.PlaceholderUtil;
import xyz.derkades.derkutils.bukkit.menu.IconMenu;
import xyz.derkades.derkutils.bukkit.menu.OptionClickEvent;
import xyz.derkades.serverselectorx.utils.ServerPinger;

public class SelectorMenu extends IconMenu {

	private final FileConfiguration config;

	public SelectorMenu(final Player player, final FileConfiguration config) {
		super(Main.getPlugin(), Colors.parseColors(config.getString("title")), config.getInt("rows"), player);

		this.config = config;

		for (final String key : config.getConfigurationSection("menu").getKeys(false)) {
			final ConfigurationSection section = config.getConfigurationSection("menu." + key);

			String materialString;
			String name;
			List<String> lore;
			int amount = 1;

			if (!section.getBoolean("ping-server")) {
				// Server pinging is turned off, get item info from 'offline' section
				materialString = section.getString("offline.item");
				name = section.getString("offline.name", " ");
				lore = section.getStringList("offline.lore");
			} else {
				final String ip = section.getString("ip");
				final int port = section.getInt("port");
				final String serverId = ip + port;
				
				final ServerPinger pinger = PingServersBackground.SERVER_INFO.get(serverId);

				// If server is online
				if (pinger != null && pinger.isOnline()) {
					final int online = pinger.getOnlinePlayers();
					final int max = pinger.getMaximumPlayers();
					final String motd = pinger.getMotd();

					// Server is online, use online section
					final ConfigurationSection onlineSection = section.getConfigurationSection("online");
					materialString = onlineSection.getString("item");
					name = onlineSection.getString("name");
					lore = onlineSection.getStringList("lore");

					if (section.getBoolean("change-item-count", false)) {
						amount = online;
					}

					if (amount > 64 || amount < 1) {
						amount = 1;
					}

					// Replace placeholders in lore and name
					name = name.replace("{online}", online + "").replace("{max}", max + "").replace("{motd}", motd).replace("{player}", player.getName());
					lore = ListUtils.replaceInStringList(lore,
							new Object[] { "{online}", "{max}", "{motd}", "{player}" },
							new Object[] { online, max, motd, player.getName() });
				} else {
					// Server is offline, use offline section
					final ConfigurationSection offlineSection = section.getConfigurationSection("offline");

					materialString = offlineSection.getString("item");
					name = offlineSection.getString("name", " ");
					lore = offlineSection.getStringList("lore");
				}
			}
			
			if (materialString == null) {
				player.sendMessage("Missing item option for item " + key);
				return;
			}
			
			materialString = materialString.toUpperCase();
			
			if (materialString.equals("AIR")) {
				return;
			}

			ItemBuilder builder;

			if (materialString.startsWith("head:")) {
				final String owner = materialString.split(":")[1];
				if (owner.equals("auto")) {
					builder = new ItemBuilder(player);
				} else {
					try {
						final UUID uuid = UUID.fromString(owner);
						builder = new ItemBuilder(Bukkit.getOfflinePlayer(uuid));
					} catch (final IllegalArgumentException e) {
						// Invalid UUID
						player.sendMessage("Invalid player head UUID for item " + key);
						return;
					}
				}
			} else {
				try {
					final Material material = Material.valueOf(materialString);
					builder = new ItemBuilder(material);
				} catch (final IllegalArgumentException e) {
					player.sendMessage("Invalid item name " + materialString);
					player.sendMessage("https://github.com/ServerSelectorX/ServerSelectorX/wiki/Item-names");
					return;
				}
			}

			builder.amount(amount);
			builder.name(PlaceholderUtil.parsePapiPlaceholders(player, name));
			builder.lore(PlaceholderUtil.parsePapiPlaceholders(player, lore));

			final ItemStack item = builder.create();

			final int slot = Integer.valueOf(key);
			if (slot < 0) {
				for (int i = 0; i < this.getInventory().getSize(); i++) {
					if (!this.hasItem(i)) {
						this.addItem(i, item);
					}
				}
			} else {
				this.addItem(slot, item);
			}
		}
	}

	@Override
	public boolean onOptionClick(final OptionClickEvent event) {
		final int slot = event.getPosition();
		final Player player = event.getPlayer();

		String action = this.config.getString("menu." + slot + ".action");

		if (action == null) {
			// If the action is null (so 'slot' is not found in the config) it is probably a wildcard
			action = this.config.getString("menu.-1.action");

			if (action == null) { //If it is still null it must be missing
				action = "msg:Action missing";
			}
		}

		if (action.startsWith("url:")){ // Send url message
			final String url = action.substring(4);
			final String message = Colors.parseColors(this.config.getString("url-message", "&3&lClick here"));

			player.spigot().sendMessage(
					new ComponentBuilder(message)
					.event(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
					.create()
					);
			return true;
		} else if (action.startsWith("cmd:")){ // Execute command
			final String command = action.substring(4);
			// delay required by some commands that open menus
			Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
				Bukkit.dispatchCommand(player, PlaceholderUtil.parsePapiPlaceholders(player, command));
			}, 2);
			return true;
		} else if (action.startsWith("consolecmd:")) {
			final String command = action.substring(11).replace("{player}", player.getName());
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PlaceholderUtil.parsePapiPlaceholders(player, command));
			return true;
		} else if (action.startsWith("sel:")){ // Open selector
			final String configName = action.substring(4);
			final FileConfiguration config = Main.getConfigurationManager().getByName(configName);
			if (config == null){
				player.sendMessage(ChatColor.RED + "This server selector does not exist.");
				return true;
			} else {
				new SelectorMenu(player, config);
				return false;
			}
		} else if (action.startsWith("world:")){ // Teleport to world
			final String worldName = action.substring(6);
			final World world = Bukkit.getWorld(worldName);
			if (world == null){
				player.sendMessage(ChatColor.RED + "A world with the name " + worldName + " does not exist.");
				return true;
			} else {
				player.teleport(world.getSpawnLocation());
				return true;
			}
		} else if (action.startsWith("srv:")){ // Teleport to server
			final String serverName = action.substring(4);
			Main.teleportPlayerToServer(player, serverName);
			return true;
		} else if (action.startsWith("msg:")){ // Send message
			final String message = action.substring(4);
			player.sendMessage(PlaceholderUtil.parsePapiPlaceholders(player, message));
			return true;
		} else if (action.equals("close")) {
			return true; // Return true = close
		} else {
			return false; // Return false = stay open
		}

	}

}
