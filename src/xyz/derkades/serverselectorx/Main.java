package xyz.derkades.serverselectorx;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import xyz.derkades.derkutils.bukkit.ItemBuilder;
import xyz.derkades.derkutils.bukkit.PlaceholderUtil;
import xyz.derkades.derkutils.bukkit.PlaceholderUtil.Placeholder;
import xyz.derkades.serverselectorx.configuration.ConfigSync;
import xyz.derkades.serverselectorx.configuration.ConfigurationManager;
import xyz.derkades.serverselectorx.placeholders.PapiExpansionRegistrar;
import xyz.derkades.serverselectorx.placeholders.Server;

public class Main extends JavaPlugin {

	static {
		// Disable NBT API update checker
		MinecraftVersion.disableUpdateCheck();
	}

	private static final int FREEZE_STEP_MS = 50;
	private static final int FREEZE_MAX_MS = 5000;

	// When set to true, lag-related debug information is printed to the console. This boolean
	// is enabled using /ssx lagdebug
	static boolean LAG_DEBUG = false;

	// When set to true, debug information related to giving items on join is printed to the
	// console. This boolean is enabled using /ssx lagdebug
	static boolean ITEM_DEBUG = false;

	private static ConfigurationManager configurationManager;
	private static ConfigSync configSync;

	private static Main plugin;

	private static BukkitAudiences adventure;

	public static WebServer server;

	public static final Gson GSON = new GsonBuilder().registerTypeAdapter(Server.class, Server.SERIALIZER).create();

	private static final Map<UUID, String> HEAD_TEXTURE_CACHE = new HashMap<>();

	public static Main getPlugin(){
		return plugin;
	}

	@Override
	public void onEnable(){
		plugin = this;

		MinecraftVersion.replaceLogger(this.getLogger());

		configurationManager = new ConfigurationManager();
		try {
			configurationManager.reload();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

		this.getCommand("serverselectorx").setExecutor(new ServerSelectorXCommand());

		//Register custom selector commands
		Commands.registerCustomCommands();

		// Disable annoying jetty warnings
		if (!configurationManager.api.getBoolean("jetty-debug", false)){
			System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
			System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
			System.setProperty("org.eclipse.jetty.util.log.announce", "false");
		}

		adventure = BukkitAudiences.create(this);

		server = new WebServer();
		server.start();

		configSync = new ConfigSync();
		new Stats();
		new ItemMoveDropCancelListener();

		Bukkit.getPluginManager().registerEvents(new ItemClickListener(), this);
		Bukkit.getPluginManager().registerEvents(new ItemGiveListener(), this);
		Bukkit.getPluginManager().registerEvents(new BetaMessageJoinListener(), this);
		Bukkit.getPluginManager().registerEvents(new ActionsOnJoinListener(), this);

		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			PapiExpansionRegistrar.register();
		}
	}

	@Override
	public void onDisable() {
		if (adventure != null) {
			adventure.close();
			adventure = null;
		}

		if (server != null) {
			server.stop();

			// Freeze bukkit thread to give the server time to stop
			int safetyLimit = 0;
			final int max = FREEZE_MAX_MS / FREEZE_STEP_MS;
			while(!server.isStopped()) {

				// Don't freeze for longer than 5 seconds
				if (safetyLimit > max) {
					this.getLogger().severe("Webserver was still running after waiting for 5 seconds.");
					this.getLogger().severe("Giving up, crashing the server is worse than breaking the menu.");
					break;
				}

				safetyLimit++;

				try {
					Thread.sleep(FREEZE_STEP_MS);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}

			// Sleep a little longer to give the internal jetty stuff time to shut down
			// otherwise I still got a NoClassDefFoundError
			try {
				Thread.sleep(100);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static ConfigurationManager getConfigurationManager() {
		return configurationManager;
	}

	static ConfigSync getConfigSync() {
		return configSync;
	}

	public static BukkitAudiences adventure() {
		return adventure;
	}

    public static ItemBuilder getItemBuilderFromItemSection(final Player player, final ConfigurationSection section) {
    	final String materialString = section.getString("material");
    	final ItemBuilder builder = getItemBuilderFromMaterialString(player, materialString);

    	if (section.contains("title")) {
	    	String title = "&r&f" + section.getString("title");
	    	title = title.replace("{player}", player.getName());
	    	title = PlaceholderUtil.parsePapiPlaceholders(player, title);
	    	builder.coloredName(title);
    	} else {
    		builder.name(" ");
    	}

		if (section.contains("lore")) {
			List<String> lore = section.getStringList("lore");
			lore = lore.stream().map((s) -> "&r&f" + s).collect(Collectors.toList());
			final Placeholder playerPlaceholder = new Placeholder("{player}", player.getName());
			lore = PlaceholderUtil.parsePapiPlaceholders(player, lore, playerPlaceholder);
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

		if (section.isInt("durability")) {
			builder.damage(section.getInt("durability"));
		}

		if (section.isConfigurationSection("nbt")) {
			final NBTItem nbt = new NBTItem(builder.create());
			final ConfigurationSection nbtSection = section.getConfigurationSection("nbt");
			for (final String key : nbtSection.getKeys(false)) {
				if (nbtSection.isBoolean(key)) {
					nbt.setBoolean(key, nbtSection.getBoolean(key));
				} else if (nbtSection.isString(key)) {
					nbt.setString(key, nbtSection.getString(key));
				} else if (nbtSection.isInt(key)) {
					nbt.setInteger(key, nbtSection.getInt(key));
				} else {
					player.sendMessage("Unsupported NBT option '" + key + "'");
				}
			}
			return new ItemBuilder(nbt.getItem());
		}

		return builder;
    }

    public static ItemBuilder getItemBuilderFromMaterialString(final Player player, final String materialString) {
		ItemBuilder builder;

		if (materialString == null) {
			player.sendMessage("Material is null, either specify a material or remove the material option completely");
			return new ItemBuilder(Material.COBBLESTONE);
		} else if (materialString.startsWith("head:")) {
			final String owner = materialString.split(":")[1];
			if (owner.equals("auto")) {
				if (getConfigurationManager().misc.getBoolean("mojang-api-head-auto", false)) {
					builder = new ItemBuilder(Material.PLAYER_HEAD).skullTexture(getHeadTexture(player.getUniqueId()));
				} else {
					builder = new ItemBuilder(player);
				}
			} else {
				try {
					builder = new ItemBuilder(Material.PLAYER_HEAD).skullTexture(getHeadTexture(UUID.fromString(owner)));
				} catch (final IllegalArgumentException e) {
					// Invalid UUID, parse as texture
					builder = new ItemBuilder(Material.PLAYER_HEAD).skullTexture(owner);
				}
			}
		} else if (materialString.startsWith("hdb")) {
			final String id = materialString.substring(4);
			builder = HDBHandler.getBuilder(id);
		} else {
			Material material;
			try {
				material = Material.valueOf(materialString);
			} catch (final IllegalArgumentException e) {
				player.sendMessage("Invalid item name '" + materialString + "'");
				player.sendMessage("https://github.com/ServerSelectorX/ServerSelectorX/wiki/Item-names");
				return new ItemBuilder(Material.COBBLESTONE);
			}

			builder = new ItemBuilder(material);
		}

		return builder;
    }

    public static String getHeadTexture(final UUID uuid) {
    	if (HEAD_TEXTURE_CACHE.containsKey(uuid)) {
    		return HEAD_TEXTURE_CACHE.get(uuid);
    	}

    	// TODO async. Need to rewrite menu code first :(

    	try {
    		Main.getPlugin().getLogger().info("Getting texture value for " + uuid + " from Mojang API");
	    	final HttpURLConnection connection = (HttpURLConnection) new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString()).openConnection();
	    	try (final Reader reader = new InputStreamReader(connection.getInputStream())) {
	    		final JsonObject jsonResponse = (JsonObject) JsonParser.parseReader(reader);
	    		final String texture = jsonResponse.get("properties").getAsJsonArray().get(0).getAsJsonObject().get("value").getAsString();
	    		HEAD_TEXTURE_CACHE.put(uuid, texture);
	    		Main.getPlugin().getLogger().info("Got " + texture);
	    		return texture;
	    	}
    	} catch (final IOException | IllegalArgumentException | NullPointerException | ClassCastException | IllegalStateException | IndexOutOfBoundsException e) {
    		Main.getPlugin().getLogger().warning("Failed to get base64 texture value for " + uuid + ". Is the UUID valid? Error details: " + e.getClass().getSimpleName() + " " + e.getMessage());
    		return "";
    	}
    }

}
