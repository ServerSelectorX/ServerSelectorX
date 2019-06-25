package xyz.derkades.serverselectorx;

import static org.bukkit.ChatColor.DARK_AQUA;
import static org.bukkit.ChatColor.DARK_GRAY;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.md_5.bungee.api.ChatColor;
import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.ListUtils;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.ItemBuilder;
import xyz.derkades.derkutils.caching.Cache;
import xyz.derkades.serverselectorx.placeholders.Placeholders;
import xyz.derkades.serverselectorx.placeholders.PlaceholdersDisabled;
import xyz.derkades.serverselectorx.placeholders.PlaceholdersEnabled;

public class Main extends JavaPlugin {

	public static boolean BETA = false;

	public static Placeholders PLACEHOLDER_API;

	public static final String PREFIX = DARK_GRAY + "[" + DARK_AQUA + "ServerSelectorX" + DARK_GRAY + "]";

	/** <server, <placeholder, result>> */
	public static final Map<String, Map<String, String>> PLACEHOLDERS = new HashMap<>();
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
		configurationManager.reload();

		//Register listeners
		Bukkit.getPluginManager().registerEvents(new SelectorOpenListener(), this);
		Bukkit.getPluginManager().registerEvents(new OnJoinListener(), this);
		Bukkit.getPluginManager().registerEvents(new ItemMoveDropCancelListener(), this);

		//Register messaging channels
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

		//Register command
		this.getCommand("serverselectorx").setExecutor(new ReloadCommand());

		//Start bStats
		Stats.initialize();

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

		//Periodically clean cache
		this.getServer().getScheduler().runTaskTimer(this, () -> {
			Cache.cleanCache();
		}, 30*60*20, 30*60*20);

		if (this.getDescription().getVersion().contains("beta")) {
			BETA = true;
		}

		// Disable annoying jetty warnings
		if (!configurationManager.getSSXConfig().getBoolean("jetty-debug", false)){
			System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
			System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
		}

		final int port = configurationManager.getSSXConfig().getInt("port");
		server = new WebServer(port);
		server.start();

		this.getServer().getScheduler().runTaskLater(this, () -> this.retrieveConfigs(), 5*20);
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

			for (final Map.Entry<String, FileConfiguration> menuConfigEntry : configurationManager.getAllMenus().entrySet()) {
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
								if (Cooldown.getCooldown(player.getUniqueId() + "doubleopen") > 0) { //if time left on cooldown is > 0
									return true;
								}

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
		final ConfigurationSection syncConfig = getConfigurationManager().getSSXConfig().getConfigurationSection("config-sync");

		if (!syncConfig.getBoolean("enabled", false)) {
			return;
		}

		this.getLogger().info("Config sync is enabled. Starting the configuration file retrieval process..");

		final String address = syncConfig.getString("address");
		final List<String> whitelist = syncConfig.getStringList("whitelist");

		URL url;
		try {
			url = new URL("http://" + address + "/config");
		} catch (final MalformedURLException e) {
			this.getLogger().severe("The address you entered seems to be incorrectly formatted.");
			this.getLogger().severe("It must be formatted like this: 173.45.16.208:8888");
			//e.printStackTrace();
			return;
		}

		Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {

			this.getLogger().info("Making request to " + url.toString());
			boolean error = false;
			String jsonOutput = null;
			try {
				final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				final BufferedReader streamReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
				final StringBuilder responseBuilder = new StringBuilder();
				String temp;
				while ((temp = streamReader.readLine()) != null)
					responseBuilder.append(temp);
				jsonOutput = responseBuilder.toString();
			} catch (final IOException e) {
				e.printStackTrace();
				error = true;
			}

			if (error) {
				this.getLogger().severe("An error occured while making a request. Are you sure you are using the right IP and port? You can find a more detailed error message in the server log.");
				return;
			}

			if (jsonOutput == null) {
				// it should only ever be null if error == true
				this.getLogger().severe("Json output null. Report issue to developer");
			}

			final File pluginDirectory = Main.getPlugin().getDataFolder();

			final File serversYml = new File(pluginDirectory, "servers.yml");
			final File globalYml = new File(pluginDirectory, "global.yml");
			final File menuDirectory = new File(pluginDirectory, "menu");

			final JsonParser parser = new JsonParser();
			final JsonObject json = parser.parse(jsonOutput).getAsJsonObject();

			this.getLogger().info("Writing to disk..");

			try {
				if (whitelist.contains("servers")) {
					serversYml.delete();
					final FileConfiguration config = new YamlConfiguration();
					config.loadFromString(json.get("servers").getAsString());
					config.save(serversYml);
				}

				if (whitelist.contains("global")) {
					globalYml.delete();
					final FileConfiguration config = new YamlConfiguration();
					config.loadFromString(json.get("global").getAsString());
					config.save(globalYml);
				}

				if (whitelist.contains("menu:all")) {
					Arrays.asList(menuDirectory.listFiles()).forEach(File::delete);

					final JsonObject menuFilesJson = json.get("menu").getAsJsonObject();
					for (final Entry<String, JsonElement> menuJson : menuFilesJson.entrySet()) {
						final FileConfiguration config = new YamlConfiguration();
						config.loadFromString(menuJson.getValue().getAsString());
						config.save(new File(menuDirectory, menuJson.getKey() + ".yml"));
					}
				} else {
					for (final String string : whitelist) {
						if (!string.startsWith("menu:")) {
							continue;
						}

						final String menuName = string.substring(5);
						if (!json.get("menu").getAsJsonObject().has(menuName)) {
							this.getLogger().warning("Skipped menu file with name '" + menuName + "', it was not sent by the server.");
							continue;
						}

						final JsonElement menuJson = json.get("menu").getAsJsonObject().get(menuName);
						final FileConfiguration config = new YamlConfiguration();
						config.loadFromString(menuJson.getAsString());
						config.save(new File(menuDirectory, menuName + ".yml"));
					}
				}
			} catch (final InvalidConfigurationException e) {
				final RuntimeException e2 = new RuntimeException("The configuration received from the server is invalid");
				e2.initCause(e);
				throw e2;
			} catch (final IOException e) {
				e.printStackTrace();
			}

			this.getLogger().info("Done! The plugin will now reload.");
			Main.getConfigurationManager().reload();
			server.stop();
			server.start();
		});
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
		final FileConfiguration config = configurationManager.getMenuByName(configName);

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
		if (Cooldown.getCooldown("servertp" + player.getName() + server) > 0) {
			return;
		}

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
		} else {
			//If the server has not sent a message at all it is offline
			return false;
		}
	}

	public static int getGlobalPlayerCount() {
		int online = 0;
		for (final Map<String, String> serverPlaceholders : Main.PLACEHOLDERS.values()) {
			online += Integer.parseInt(serverPlaceholders.get("online"));
		}
		return online;
	}

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

    public static ItemStack getHotbarItemStackFromMenuConfig(final Player player, final FileConfiguration menuConfig, final String configName) {
    	final ItemBuilder builder;

		final String materialString = menuConfig.getString("item.material");

		if (materialString.startsWith("head:")) {
			final String owner = materialString.split(":")[1];
			if (owner.equals("auto")) {
				builder = new ItemBuilder(player);
			} else {
				player.sendMessage("Custom player heads are not implemented in this version. You can only use 'head:auto'.");
				return null;
			}
		} else {
			final Material material = Material.getMaterial(materialString);

			if (material == null) {
				Main.error("Invalid item name for menu with name " + configName, player);
			}

			builder = new ItemBuilder(material);
		}

		builder.name(Main.PLACEHOLDER_API.parsePlaceholders(player,
				menuConfig.getString("item.title", "error")
						.replace("{player}", player.getName())));

		if (menuConfig.contains("item.lore")) {
			List<String> lore = menuConfig.getStringList("item.lore");
			lore = Main.PLACEHOLDER_API.parsePlaceholders(player, lore);
			lore = ListUtils.replaceInStringList(lore,
						new Object[] {"{player}"},
						new Object[] {player.getName()});
				builder.coloredLore(lore);
		}

		return builder.create();
    }

}
