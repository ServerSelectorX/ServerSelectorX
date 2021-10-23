package xyz.derkades.serverselectorx;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jetty.util.log.Log;
import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.simple.SimpleLogger;
import org.slf4j.simple.SimpleLoggerConfiguration;
import xyz.derkades.derkutils.LoggerOutputStream;
import xyz.derkades.derkutils.bukkit.ItemBuilder;
import xyz.derkades.derkutils.bukkit.NbtItemBuilder;
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

	@SuppressWarnings("null")
	@NotNull
	public static Main getPlugin(){
		return plugin;
	}

	@Override
	public void onEnable() {
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

		// Register custom selector commands
		Commands.registerCustomCommands();

		try {
			Class<?> outputChoiceClass = Class.forName("org.slf4j.simple.OutputChoice");
			OutputStream out = new LoggerOutputStream(this.getLogger(), Level.INFO);
			PrintStream printStream = new PrintStream(out);
			Constructor outputChoiceConstructor = outputChoiceClass.getDeclaredConstructor(PrintStream.class);
			outputChoiceConstructor.setAccessible(true);
			Object outputChoice = outputChoiceConstructor.newInstance(printStream);
			Field configField = SimpleLogger.class.getDeclaredField("CONFIG_PARAMS");
			configField.setAccessible(true);
			SimpleLoggerConfiguration config = (SimpleLoggerConfiguration) configField.get(null);
			Field outputChoiceField = SimpleLoggerConfiguration.class.getDeclaredField("outputChoice");
			outputChoiceField.setAccessible(true);
			outputChoiceField.set(config, outputChoice);
			Field initializedField = SimpleLogger.class.getDeclaredField("INITIALIZED");
			initializedField.setAccessible(true);
			initializedField.set(null, true);
		} catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
			e.printStackTrace();
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

    public static NbtItemBuilder getItemBuilderFromItemSection(final Player player, final ConfigurationSection section) {
    	final String materialString = section.getString("material");
    	final NbtItemBuilder builder = getItemBuilderFromMaterialString(player, materialString);
    	
    	boolean useMiniMessage = section.getBoolean("minimessage", false);

    	if (section.contains("title")) {
	    	String title = section.getString("title");
	    	title = title.replace("{player}", player.getName());
	    	title = PlaceholderUtil.parsePapiPlaceholders(player, title);
	    	if (useMiniMessage) {
	    		Component c = MiniMessage.get().deserialize(title);
	    		title = LegacyComponentSerializer.legacySection().serialize(c);
	    		builder.name(title);
	    	} else {
	    		title = "&r&f" + title;
	    		builder.coloredName(title);
	    	}
    	} else {
    		builder.name(" ");
    	}

		if (section.contains("lore")) {
			List<String> lore = new ArrayList<>(section.getStringList("lore"));
			for (int i = 0; i < lore.size(); i++) {
				String line = lore.get(i);
				final Placeholder playerPlaceholder = new Placeholder("{player}", player.getName());
				line = PlaceholderUtil.parsePapiPlaceholders(player, line, playerPlaceholder);
				if (useMiniMessage) {
		    		Component c = MiniMessage.get().deserialize(line);
		    		line = LegacyComponentSerializer.legacySection().serialize(c);
				} else {
					line = "&r&f";
				}
				lore.set(i, line);
			}
			if (useMiniMessage) {
				builder.lore(lore);
			} else {
				builder.coloredLore(lore);
			}
		}

		boolean hideFlagsDefault = false;
		if (section.getBoolean("enchanted")) {
			builder.unsafeEnchant(Enchantment.DURABILITY, 1);
			hideFlagsDefault = true;
		}

		if (section.getBoolean("hide-flags", hideFlagsDefault)) {
			builder.hideFlags();
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
			return new NbtItemBuilder(nbt.getItem());
		}

		return builder;
    }

    public static NbtItemBuilder getItemBuilderFromMaterialString(final Player player, final String materialString) {
    	NbtItemBuilder builder;

		if (materialString == null) {
			player.sendMessage("Material is null, either specify a material or remove the material option completely");
			return new NbtItemBuilder(Material.COBBLESTONE);
		} else if (materialString.startsWith("head:")) {
			final String owner = materialString.split(":")[1];
			if (owner.equals("auto")) {
				if (getConfigurationManager().getMiscConfiguration().getBoolean("mojang-api-head-auto", false)) {
					final String texture = getHeadTexture(UUID.fromString(owner));
					if (texture != null) {
						builder = new NbtItemBuilder(Material.PLAYER_HEAD).skullTexture(texture);
					} else {
						builder = new NbtItemBuilder(Material.PLAYER_HEAD);
					}
				} else {
					builder = new NbtItemBuilder(Material.PLAYER_HEAD).skullOwner(player);
				}
			} else {
				try {
					final String texture = getHeadTexture(UUID.fromString(owner));
					if (texture != null) {
						builder = new NbtItemBuilder(Material.PLAYER_HEAD).skullTexture(texture);
					} else {
						builder = new NbtItemBuilder(Material.PLAYER_HEAD);
					}
				} catch (final IllegalArgumentException e) {
					// Invalid UUID, parse as texture
					builder = new NbtItemBuilder(Material.PLAYER_HEAD).skullTexture(owner);
				}
			}
		} else if (materialString.startsWith("hdb")) {
			final String id = materialString.substring(4);
			builder = HDBHandler.getBuilder(id);
		} else {
			final String[] materialsToTry = materialString.split("\\|");
			Material material = null;
			for (final String materialString2 : materialsToTry) {
				try {
					material = Material.valueOf(materialString2);
					break;
				} catch (final IllegalArgumentException e) {}
			}

			if (material == null) {
				player.sendMessage("Invalid item name '" + materialString + "'");
				player.sendMessage("https://github.com/ServerSelectorX/ServerSelectorX/wiki/Item-names");
				return new NbtItemBuilder(Material.COBBLESTONE);
			}

			builder = new NbtItemBuilder(material);
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
    		return null;
    	}
    }

}
