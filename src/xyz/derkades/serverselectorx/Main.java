package xyz.derkades.serverselectorx;

import static org.bukkit.ChatColor.DARK_AQUA;
import static org.bukkit.ChatColor.DARK_GRAY;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;
import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.ListUtils;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.ItemBuilder;
import xyz.derkades.serverselectorx.effects.Effects;
import xyz.derkades.serverselectorx.placeholders.Placeholders;
import xyz.derkades.serverselectorx.placeholders.PlaceholdersDisabled;
import xyz.derkades.serverselectorx.placeholders.PlaceholdersEnabled;

public class Main extends JavaPlugin {

	public static boolean BETA = false;

	public static Placeholders PLACEHOLDER_API;

	public static final String PREFIX = DARK_GRAY + "[" + DARK_AQUA + "ServerSelectorX" + DARK_GRAY + "]";

	/* <serverName, <player uuid (null for global placeholders), <placeholder, result>>> */
	public static final Map<String, Map<UUID, Map<String, String>>> PLACEHOLDERS = new HashMap<>();
	public static final Map<String, Long> LAST_INFO_TIME = new HashMap<>();

	private static ConfigurationManager configurationManager;

	private static Main plugin;

	public static WebServer server;

	public static Main getPlugin(){
		return plugin;
	}

	@Override
	public void onEnable(){
		plugin = this;

		configurationManager = new ConfigurationManager();

		Bukkit.getPluginManager().registerEvents(new OnJoinListener(), this);

		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

		this.getCommand("serverselectorx").setExecutor(new ReloadCommand());

		//Register custom selector commands
		this.registerCommands();

		//Check if PlaceHolderAPI is installed
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")){
			Main.PLACEHOLDER_API = new PlaceholdersEnabled();
			//getLogger().log(Level.INFO, "PlaceholderAPI is found. Placeholders will work!");
		} else {
			Main.PLACEHOLDER_API = new PlaceholdersDisabled();
			//getLogger().log(Level.INFO, "PlaceholderAPI is not installed. The plugin will still work.");
		}

		if (this.getDescription().getVersion().contains("beta")) {
			BETA = true;
		}

		// Disable annoying jetty warnings
		if (!configurationManager.getSSXConfig().getBoolean("jetty-debug", false)){
			//System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
			System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
		}

		final int port = configurationManager.getSSXConfig().getInt("port");
		server = new WebServer(port);
		server.start();

		this.getServer().getScheduler().runTaskLater(this, () -> this.retrieveConfigs(), 5*20);

		new Effects();
		new ConfigSync();
		new Stats();
		new ItemMoveDropCancelListener();
		Bukkit.getPluginManager().registerEvents(new ItemOpenListener(), this);
	}

	@Override
	public void onDisable() {
		if (server != null) {
			server.stop();
			if (configurationManager.getGlobalConfig().getBoolean("freeze-bukkit-thread", true)) {
				// Freeze bukkit thread to give the server time to stop
				try {
					Thread.sleep(3000);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Registers all custom commands by going through all menu files and adding commands
	 */
	private void registerCommands(){
		try {
			final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

			bukkitCommandMap.setAccessible(true);
			final CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

			for (final Map.Entry<String, FileConfiguration> menuConfigEntry : configurationManager.getMenus().entrySet()) {
				final String configName = menuConfigEntry.getKey();
				final FileConfiguration config = menuConfigEntry.getValue();

				if (!config.contains("commands")) {
					continue;
				}

				final List<String> commandNames = config.getStringList("commands");

				for (final String commandName : commandNames) {
					commandMap.register("ssx-custom", new Command(commandName){

						@Override
						public boolean execute(final CommandSender sender, final String label, final String[] args) {
							if (sender instanceof Player){
								final Player player = (Player) sender;
								//Small cooldown to prevent weird bugs
								if (Cooldown.getCooldown(player.getUniqueId() + "doubleopen") > 0)
									return true;

								Cooldown.addCooldown(player.getUniqueId() + "doubleopen", 1000); //Add cooldown for 1 second

								Main.openSelector(player, configName);
							}
							return true;
						}

					});
				}

			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	void retrieveConfigs() {
			}

	public static ConfigurationManager getConfigurationManager() {
		return configurationManager;
	}

	/**
	 * Only used by open listener and commands
	 *
	 * Checks for cooldown, checks for permissions, plays sound
	 */
	public static void openSelector(final Player player, final String configName) {
		final FileConfiguration config = configurationManager.getMenus().get(configName);

		if (config == null) {
			Main.error("A menu with this name does not exist", player);
			return;
		}

		// Check for permissions

		final boolean permissionEnabled = config.getBoolean("permission.open", false);
		final String permission = "ssx.open." + configName;

		if (permissionEnabled && !player.hasPermission(permission)) {
			if (config.contains("permission.message")) {
				final String message = config.getString("permission.message", "");
				if (!message.equals("")) {
					player.sendMessage(Colors.parseColors(message));
				}
			}
			return;
		}

		// Play sound

		final String soundString = Main.getPlugin().getConfig().getString("selector-open-sound");
		if (soundString != null && !soundString.equals("NONE")){
			try {
				final Sound sound = Sound.valueOf(soundString);
				player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
			} catch (final IllegalArgumentException e){
				Main.getPlugin().getLogger().log(Level.WARNING, "A sound with the name " + soundString + " could not be found. Make sure that it is the right name for your server version.");
			}
		}

		// Open menu

		new SelectorMenu(player, config, configName).open();
	}

	public static void teleportPlayerToServer(final Player player, final String server){
		if (Cooldown.getCooldown("servertp" + player.getName() + server) > 0)
			return;

		Cooldown.addCooldown("servertp" + player.getName() + server, 1000);

		// Send message if enabled
		final FileConfiguration globalConfig = Main.getConfigurationManager().getGlobalConfig();
		if (globalConfig.getBoolean("server-teleport-message-enabled", false)){
			if (globalConfig.getBoolean("chat-clear", false)){
				for (int i = 0; i < 150; i++) {
					player.sendMessage("");
				}
			}

			final String message = Colors.parseColors(globalConfig.getString("server-teleport-message", "error"));
			player.sendMessage(message.replace("{server}", server));
		}

		// Send message to bungeecord
		try (
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos)
			){

	        dos.writeUTF("Connect");
	        dos.writeUTF(server);
	        player.sendPluginMessage(getPlugin(), "BungeeCord", baos.toByteArray());
		} catch (final IOException e){
			e.printStackTrace();
		}
	}

	public static boolean isOnline(final String serverName) {
		if (Main.LAST_INFO_TIME.containsKey(serverName)) {
			// If the server has not sent a message for 7 seconds (usually the server sends a message every 5 seconds)

			final long timeSinceLastPing = System.currentTimeMillis() - Main.LAST_INFO_TIME.get(serverName);

			final long timeout = configurationManager.getGlobalConfig().getLong("server-offline-timeout", 6000);

			return timeSinceLastPing < timeout;
		} else
			//If the server has not sent a message at all it is offline
			return false;
	}

	public static int getGlobalPlayerCount() {
		int online = 0;
		for (final Map<UUID, Map<String, String>> serverPlaceholders : Main.PLACEHOLDERS.values()) {
			online += Integer.parseInt(serverPlaceholders.get(null).get("online"));
		}
		return online;
	}

	@Deprecated
	public static ItemStack addHideFlags(final ItemStack item) {
		try {
			final String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];

			final Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
			final Class<?> nmsItemStackClass = Class.forName("net.minecraft.server." + version + ".ItemStack");
			final Class<?> nbtTagCompoundClass = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");

			final Object nmsItemStack = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);

			Object nbtTagCompound = nmsItemStackClass.getMethod("getTag").invoke(nmsItemStack);
			if (nbtTagCompound == null) {
				nbtTagCompound = nbtTagCompoundClass.getConstructor().newInstance();
			}

			nbtTagCompoundClass.getMethod("setInt", String.class, int.class).invoke(nbtTagCompound, "HideFlags", 63);

			nmsItemStackClass.getMethod("setTag", nbtTagCompoundClass).invoke(nmsItemStack, nbtTagCompound);

			return (ItemStack) craftItemStackClass.getMethod("asBukkitCopy", nmsItemStackClass).invoke(null, nmsItemStack);
		} catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException |
				NoSuchMethodException | SecurityException | InstantiationException e) {
			e.printStackTrace();
			return item;
		}
	}

    public static void error(final String message, final Player... players) {
    	for (final Player player : players) {
    		player.sendMessage(ChatColor.RED + message);
    	}
		Main.getPlugin().getLogger().severe(message);
    }

    public static ItemBuilder getItemBuilderFromItemSection(final Player player, final ConfigurationSection section) {
    	final String materialString = section.getString("material");
    	final ItemBuilder builder = getItemBuilderFromMaterialString(player, materialString);

		builder.name(Main.PLACEHOLDER_API.parsePlaceholders(player,
				section.getString("title", " ")
						.replace("{player}", player.getName())));

		if (section.contains("lore")) {
			List<String> lore = section.getStringList("lore");
			lore = Main.PLACEHOLDER_API.parsePlaceholders(player, lore);
			lore = ListUtils.replaceInStringList(lore,
						new Object[] {"{player}"},
						new Object[] {player.getName()});
				builder.coloredLore(lore);
		}

		if (section.getBoolean("enchanted")) {
			builder.unsafeEnchant(Enchantment.DURABILITY, 1);
		}

		if (section.getBoolean("hide-flags")) {
			builder.hideFlags(63);
		}

		if (section.isInt("amount")) {
			builder.amount(section.getInt("amount"));
		}

		return builder;
    }

    public static ItemBuilder getItemBuilderFromMaterialString(final Player player, final String materialString) {
		final ItemBuilder builder;

		if (materialString.startsWith("head:")) {
			final String owner = materialString.split(":")[1];
			if (owner.equals("auto")) {
				builder = new ItemBuilder(player);
			} else {
				final OfflinePlayer ownerPlayer;
				try {
					ownerPlayer = Bukkit.getOfflinePlayer(UUID.fromString(owner));
				} catch (final IllegalArgumentException e) {
					player.sendMessage("Invalid player uuid (for head item): " + owner);
					e.printStackTrace();
					return new ItemBuilder(Material.COBBLESTONE);
				}

				if (ownerPlayer == null) {
					player.sendMessage("A player with the uuid " + ownerPlayer + " does not exist");
					return new ItemBuilder(Material.COBBLESTONE);
				}

				builder = new ItemBuilder(ownerPlayer);
			}
		} else {
			Material material;
			try {
				material = Material.valueOf(materialString);
			} catch (final IllegalArgumentException e) {
				player.sendMessage("Invalid item name '" + materialString + "'");
				return new ItemBuilder(Material.COBBLESTONE);
			}

			builder = new ItemBuilder(material);
		}

		return builder;
    }

}
