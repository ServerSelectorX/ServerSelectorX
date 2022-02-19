package xyz.derkades.serverselectorx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.derkades.derkutils.bukkit.NbtItemBuilder;
import xyz.derkades.derkutils.bukkit.PlaceholderUtil;
import xyz.derkades.serverselectorx.configuration.ConfigSync;
import xyz.derkades.serverselectorx.configuration.ConfigurationManager;
import xyz.derkades.serverselectorx.placeholders.PapiExpansionRegistrar;
import xyz.derkades.serverselectorx.placeholders.Server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class Main extends JavaPlugin {

	static {
		// Disable NBT API update checker
		MinecraftVersion.disableUpdateCheck();
	}

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
		this.getCommand("serverselectorx").setTabCompleter(new ServerSelectorXCommandCompleter());

		// Register custom selector commands
		Commands.registerCustomCommands();

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

    public static void getItemBuilderFromMaterialString(final Player player, @Nullable String materialString, Consumer<NbtItemBuilder> builderConsumer) {
		if (materialString == null) {
			return;
		}

		if (materialString.charAt(0) == '!') {
			materialString = PlaceholderUtil.parsePapiPlaceholders(player, materialString.substring(1));
		}

		if (materialString.startsWith("head:")) {
			final String owner = materialString.split(":")[1];
			if (owner.equals("auto")) {
				if (getConfigurationManager().getMiscConfiguration().getBoolean("mojang-api-head-auto", false)) {
					getHeadTexture(player.getUniqueId(), texture -> {
						if (texture != null) {
							builderConsumer.accept(new NbtItemBuilder(Material.PLAYER_HEAD).skullTexture(texture));
						} else {
							builderConsumer.accept(new NbtItemBuilder(Material.PLAYER_HEAD));
						}
					});
				} else {
					builderConsumer.accept(new NbtItemBuilder(Material.PLAYER_HEAD).skullOwner(player));
				}
			} else {
				try {
					final UUID ownerUuid = UUID.fromString(owner);
					getHeadTexture(ownerUuid, texture -> {
						if (texture != null) {
							builderConsumer.accept(new NbtItemBuilder(Material.PLAYER_HEAD).skullTexture(texture));
						} else {
							builderConsumer.accept(new NbtItemBuilder(Material.PLAYER_HEAD));
						}
					});
				} catch (final IllegalArgumentException e) {
					// Invalid UUID, parse as texture
					builderConsumer.accept(new NbtItemBuilder(Material.PLAYER_HEAD).skullTexture(owner));
				}
			}
			return;
		}

		if (materialString.startsWith("hdb")) {
			final String id = materialString.substring(4);
			builderConsumer.accept(HDBHandler.getBuilder(id));
			return;
		}


		final String[] materialsToTry = materialString.split("\\|");
		Material material = null;
		for (final String materialString2 : materialsToTry) {
			try {
				material = Material.valueOf(materialString2);
				break;
			} catch (final IllegalArgumentException ignored) {
			}
		}

		if (material == null) {
			player.sendMessage("Invalid item name '" + materialString + "'");
			player.sendMessage("https://github.com/ServerSelectorX/ServerSelectorX/wiki/Item-names");
			return;
		}

		if (material != Material.AIR) {
			builderConsumer.accept(new NbtItemBuilder(material));
		}
	}

    public static void getHeadTexture(final UUID uuid, Consumer<String> textureConsumer) {
    	if (HEAD_TEXTURE_CACHE.containsKey(uuid)) {
    		textureConsumer.accept(HEAD_TEXTURE_CACHE.get(uuid));
    	}

		Main.getPlugin().getLogger().info("Getting texture value for " + uuid + " from Mojang API");
		Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
			try {

				final HttpURLConnection connection = (HttpURLConnection) new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString()).openConnection();
				try (final Reader reader = new InputStreamReader(connection.getInputStream())) {
					final JsonObject jsonResponse = (JsonObject) JsonParser.parseReader(reader);
					final String texture = jsonResponse.get("properties").getAsJsonArray().get(0).getAsJsonObject().get("value").getAsString();
					HEAD_TEXTURE_CACHE.put(uuid, texture);
					Main.getPlugin().getLogger().info("Got " + texture);
					Bukkit.getScheduler().runTask(getPlugin(), () -> textureConsumer.accept(texture));
				}
			} catch (final IOException | IllegalArgumentException | NullPointerException | ClassCastException | IllegalStateException | IndexOutOfBoundsException e) {
				Main.getPlugin().getLogger().warning("Failed to get base64 texture value for " + uuid + ". Is the UUID valid? Error details: " + e.getClass().getSimpleName() + " " + e.getMessage());
			}
		});
    }

}
