package xyz.derkades.serverselectorx;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.serverselectorx.placeholders.Papi;
import xyz.derkades.serverselectorx.placeholders.PapiDisabled;
import xyz.derkades.serverselectorx.placeholders.PapiEnabled;

public class Main extends JavaPlugin {

	public static Papi PLACEHOLDER_API;

//	public static Map<String, Map<String, Object>> SERVER_PLACEHOLDERS = new HashMap<>();

	private static ConfigurationManager configurationManager;

	private static JavaPlugin plugin;

	public static JavaPlugin getPlugin(){
		return plugin;
	}
	
	public static BukkitTask pingTask;

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

		PingServersBackground.generatePingJobs();
		pingTask = Bukkit.getScheduler().runTaskLaterAsynchronously(this, new PingServersBackground(), 40);

		//Register command
		this.getCommand("serverselectorx").setExecutor(new ReloadCommand());

		//Start bStats
		Stats.initialize();

		//Register custom selector commands
		this.registerCommands();

		//Check if PlaceHolderAPI is installed
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")){
			Main.PLACEHOLDER_API = new PapiEnabled();
		} else {
			Main.PLACEHOLDER_API = new PapiDisabled();
		}

		this.getLogger().info("Thank you for using ServerSelectorX. If you enjoy using this plugin, please consider buying the premium version. It has more features and placeholders update instantly. https://github.com/ServerSelectorX/ServerSelectorX/wiki/Premium");
	}
	
	@Override
	public void onDisable() {
		if (pingTask != null) {
			// Unfortunately not possible to wait until ping task is cancelled,
			// isCancelled() is missing in 1.8
			pingTask.cancel();

			try {
				Thread.sleep(200);
			} catch (final InterruptedException e) {
				e.printStackTrace();
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

			for (final Entry<String, FileConfiguration> config : configurationManager.files.entrySet()){
				final String commandName = config.getValue().getString("command");

				if (commandName == null || commandName.equalsIgnoreCase("none")) {
					continue;
				}
				
				final String configName = config.getKey();

				commandMap.register("ssx-custom", new Command(commandName){

					@Override
					public boolean execute(final CommandSender sender, final String label, final String[] args) {
						if (sender instanceof Player){
							final Player player = (Player) sender;
							final FileConfiguration config = configurationManager.getByName(configName);
							if (config == null) {
								player.sendMessage("The menu '" + configName + "' has disappeared? Restart to remove this command.");
							} else {
								new SelectorMenu(player, config);
							}
						}
						return true;
					}

				});

			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static ConfigurationManager getConfigurationManager() {
		return configurationManager;
	}

	public static void teleportPlayerToServer(final Player player, final String server){
		final String message = Main.getPlugin().getConfig().getString("server-teleport-message");
		if (message != null) {
			player.sendMessage(Colors.parseColors(message.replace("{x}", server)));
		}

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

}
