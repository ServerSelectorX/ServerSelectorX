package xyz.derkades.serverselectorx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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

	private final HotbarItemManager hotbarItemManager = new HotbarItemManager(this);
	public HotbarItemManager getHotbarItemManager() { return this.hotbarItemManager; }

	private final Heads heads = new Heads(this);

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

		PluginCommand command = Objects.requireNonNull(this.getCommand("serverselectorx"),
				"Command missing from plugin.yml");
		command.setExecutor(new ServerSelectorXCommand());
		command.setTabCompleter(new ServerSelectorXCommandCompleter());

		// Register custom selector commands
		Commands.registerCustomCommands();

		adventure = BukkitAudiences.create(this);

		server = new WebServer();
		server.start();

		configSync = new ConfigSync();
		new Stats();
		new ItemMoveDropCancelListener();
		this.hotbarItemManager.enable();

		Bukkit.getPluginManager().registerEvents(new ItemClickListener(), this);
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

    public static void getItemBuilderFromMaterialString(final Player player, @Nullable String materialString, Consumer<NbtItemBuilder> builderConsumer) throws InvalidConfigurationException {
		if (materialString == null || materialString.isEmpty()) {
			return;
		}

		if (materialString.charAt(0) == '!') {
			materialString = PlaceholderUtil.parsePapiPlaceholders(player, materialString.substring(1));
		}

		if (materialString.startsWith("head:")) {
			String headValue = materialString.split(":")[1];
			if (headValue.equals("self") || headValue.equals("auto")) {
				if (getConfigurationManager().getMiscConfiguration().getBoolean("mojang-api-head-auto", false)) {
					// Bypass head system, just return player's own head. No need to get a texture, because the server caches it for online players
					builderConsumer.accept(new NbtItemBuilder(Material.SKULL_ITEM).damage(3).skullOwner(player));
					return;
				} else {
					headValue = "uuid:" + player.getUniqueId();
				}
			}

			CompletableFuture<@Nullable String> headTextureFuture = plugin.heads.getHead(headValue);

			Futures.whenCompleteOnMainThread(plugin, headTextureFuture, (headTexture, exception) -> {
				if (exception != null) {
					exception.printStackTrace();
				}

				if (headTexture != null) {
					builderConsumer.accept(new NbtItemBuilder(Material.SKULL_ITEM).damage(3).skullTexture(headTexture));
				} else {
					builderConsumer.accept(new NbtItemBuilder(Material.SKULL_ITEM).damage(3));
				}
			});
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

}
