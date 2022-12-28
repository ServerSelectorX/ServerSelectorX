package xyz.derkades.serverselectorx;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.ItemBuilder;
import xyz.derkades.derkutils.bukkit.PlaceholderUtil;
import xyz.derkades.derkutils.bukkit.menu.IconMenu;
import xyz.derkades.derkutils.bukkit.menu.MenuCloseEvent;
import xyz.derkades.derkutils.bukkit.menu.OptionClickEvent;
import xyz.derkades.serverselectorx.utils.ServerPinger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectorMenu extends IconMenu {

	private final FileConfiguration config;

	public SelectorMenu(final Player player, final FileConfiguration config) {
		super(Main.getPlugin(),
				Colors.parseColors(config == null ? "error" : config.getString("title")),
				config == null ? 1 : config.getInt("rows"),
				player);

		this.config = config;

		if (config == null) {
			player.sendMessage("The config file for this menu isn't loaded, check the file for YAML syntax errors.");
			return;
		}

		if (!config.isConfigurationSection("menu")) {
			player.sendMessage("The config file for this menu doesn't contain a menu section.");
			player.sendMessage("If it is there, maybe the config failed to load entirely. Please check for earlier YAML syntax errors in the console.");
			return;
		}

		fillMenu(player);
	}

	private void fillMenu(final Player player) {
		for (final String key : config.getConfigurationSection("menu").getKeys(false)) {
			final ConfigurationSection section = config.getConfigurationSection("menu." + key);

			final Map<String, String> placeholders = new HashMap<>();
			placeholders.put("{player}", player.getName());
			final ConfigurationSection chosenSection = chooseSection(section, placeholders);

			final String materialString = chosenSection.getString("item");
			final String name = chosenSection.getString("name", " ");
			final List<String> lore = chosenSection.getStringList("lore");
			int amount = chosenSection.getInt("amount", 1);
			int data = chosenSection.getInt("data", 0);

			if (materialString == null) {
				player.sendMessage("Missing item option for item " + key);
				continue;
			}

			if (materialString.equals("AIR") || materialString.equals("NONE")) {
				continue;
			}

			if (placeholders.containsKey("{online}")) {
				if (section.getBoolean("change-item-count", false)) {
					final int online = Integer.parseInt(placeholders.get("{online}"));
					amount = Math.max(Math.min(online, 64), 1);
				}
			}

			final ItemBuilder builder = Main.getItemFromMaterialString(player, materialString);

			builder.amount(amount)
					.coloredName(name)
					.coloredLore(lore)
					.placeholders(placeholders)
					.papi(player)
					.hideFlags();

			if (data != 0) {
				builder.damage(data);
			}

			if (section.getBoolean("enchanted")) {
				builder.unsafeEnchant(Enchantment.DURABILITY, 1);
			}

			final ItemStack item = builder.create();

			final int slot = Integer.parseInt(key);
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

	private ConfigurationSection chooseSection(ConfigurationSection slotSection, Map<String, String> placeholders) {
		if (!slotSection.getBoolean("ping-server")) {
			// Server pinging is turned off, get item info from 'offline' section
			return slotSection.getConfigurationSection("offline");
		}

		final String ip = slotSection.getString("ip");
		final int port = slotSection.getInt("port");
		final String serverId = ip + ":" + port;

		final ServerPinger pinger = PingServersBackground.SERVER_INFO.get(serverId);

		// If server is online
		if (pinger == null || !pinger.isOnline()) {
			// Server is offline (or hasn't been pinged yet), use offline section
			return slotSection.getConfigurationSection("offline");
		}

		final String motd = pinger.getMotd();

		placeholders.put("{online}", String.valueOf(pinger.getOnlinePlayers()));
		placeholders.put("{max}", String.valueOf(pinger.getMaximumPlayers()));
		placeholders.put("{motd}", motd);

		if (slotSection.isConfigurationSection("dynamic")) {
			final ConfigurationSection dyn = slotSection.getConfigurationSection("dynamic");
			if (dyn.isConfigurationSection(motd)) {
				return dyn.getConfigurationSection(motd);
			} else {
				return slotSection.getConfigurationSection("online");
			}
		} else {
			return slotSection.getConfigurationSection("online");
		}
	}

	@Override
	public boolean onOptionClick(final OptionClickEvent event) {
		if (this.config == null) {
			Main.getPlugin().getLogger().warning("Ignoring menu click, config isn't loaded");
			return false;
		}

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

		if (action.startsWith("url:")) { // Send url message
			final String url = action.substring(4);
			final String message = Colors.parseColors(this.config.getString("url-message", "&3&lClick here"));

			player.spigot().sendMessage(
					new ComponentBuilder(message)
					.event(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
					.create()
					);
			return true;
		} else if (action.startsWith("cmd:")) { // Execute command
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
		} else if (action.startsWith("sel:")) { // Open selector
			final String configName = action.substring(4);
			final FileConfiguration config = Main.getConfigurationManager().getByName(configName);
			if (config == null){
				player.sendMessage(ChatColor.RED + "This server selector does not exist.");
				return true;
			} else {
				new SelectorMenu(player, config);
				return false;
			}
		} else if (action.startsWith("world:")) { // Teleport to world
			final String worldName = action.substring(6);
			final World world = Bukkit.getWorld(worldName);
			if (world == null){
				player.sendMessage(ChatColor.RED + "A world with the name " + worldName + " does not exist.");
				return true;
			} else {
				player.teleport(world.getSpawnLocation());
				return true;
			}
		} else if (action.startsWith("srv:")) { // Teleport to server
			final String serverName = action.substring(4);
			Main.teleportPlayerToServer(player, serverName);
			return true;
		} else if (action.startsWith("msg:")) { // Send message
			final String message = action.substring(4);
			player.sendMessage(PlaceholderUtil.parsePapiPlaceholders(player, message));
			return true;
		} else if (action.equals("close")) {
			return true; // Return true = close
		} else if (action.equals("none")) {
			return false;
		} else {
			player.sendMessage("Invalid action '" + action + "'");
			player.sendMessage("Action list: https://github.com/ServerSelectorX/ServerSelectorX/wiki/Actions");
			return true;
		}
	}

	@Override
	public void onClose(final MenuCloseEvent event) {
		if (event.getOfflinePlayer() instanceof Player) {
			Cooldown.addCooldown("ssx-global-open" + event.getPlayer().getName(), 300);
		}
	}

	static boolean checkPermission(Player player, FileConfiguration config) {
		if (config.getBoolean("require-permission")) {
			String permissionNode = config.getString("permission-node", null);
			String noPermissionMessage = config.getString("no-permission-message", null);
			if (permissionNode == null) {
				player.sendMessage("require-permission is enabled but permission-node is not configured");
				return false;
			}
			if (noPermissionMessage == null) {
				player.sendMessage("require-permission is enabled but no-permission-message is not configured");
				return false;
			}
			if (!player.hasPermission(permissionNode)) {
				player.sendMessage(Colors.parseColors(noPermissionMessage));
				return false;
			}
		}
		return true;
	}

}
