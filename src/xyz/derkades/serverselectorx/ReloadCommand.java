package xyz.derkades.serverselectorx;

import static org.bukkit.ChatColor.AQUA;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.md_5.bungee.api.ChatColor;

public class ReloadCommand implements CommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){		
		if (args.length == 1 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl"))){
			if (!sender.hasPermission("ssx.reload")) {
				sender.sendMessage(ChatColor.RED + "You need the permission 'ssx.reload' to execute this command.");
				return true;
			}
			
			Main.getConfigurationManager().reloadAll();
			
			Main.server.stop();
			Main.server.start();
			Main.getPlugin().getLogger().info("Server restarted!");
					
			sender.sendMessage(AQUA + "The configuration file has been reloaded.");
			return true;			
		} else if (args.length == 2 && args[0].equalsIgnoreCase("retrieveconfig")) {
			URL url;
			try {
				url = new URL("http://" + args[1] + "/config");
			} catch (MalformedURLException e) {
				sender.sendMessage(ChatColor.RED + "The address you entered seems to be incorrectly formatted.");
				sender.sendMessage("It must be formatted like this: 173.45.16.208:8888");
				e.printStackTrace();
				return true;
			}
			
			sender.sendMessage(ChatColor.GRAY + "Making request to " + url.toString());
			
			Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
				boolean error = false;
				String jsonOutput = null;
				try {
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					BufferedReader streamReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
					StringBuilder responseBuilder = new StringBuilder();
					String temp;
					while ((temp = streamReader.readLine()) != null)
						responseBuilder.append(temp);
					jsonOutput = responseBuilder.toString();
				} catch (IOException e) {
					e.printStackTrace();
					error = true;
				}
				
				final boolean errorFinal = error;
				final String jsonOutputFinal = jsonOutput;
				
				// Go back to bukkit main thread
				Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
					if (errorFinal) {
						sender.sendMessage(ChatColor.RED + "An error occured while making a request. Are you sure you are using the right IP and port? You can find a more detailed error message in the server log.");
					} else {
						if (jsonOutputFinal == null) {
							// it should only ever be null if error == true
							throw new AssertionError();
						}
						
						sender.sendMessage(ChatColor.GRAY + "Deleting existing configuration..");
						File pluginDirectory = Main.getPlugin().getDataFolder();
						
						File serversYml = new File(pluginDirectory, "servers.yml");
						File menuDirectory = new File(pluginDirectory, "menu");
						serversYml.delete(); menuDirectory.delete(); menuDirectory.mkdirs();
							
						sender.sendMessage(ChatColor.GRAY + "Parsing request output..");
						
						JsonParser parser = new JsonParser();
						JsonObject json = parser.parse(jsonOutputFinal).getAsJsonObject();
						
						List<String> retrievedConfigurationFiles = new ArrayList<>();
						retrievedConfigurationFiles.add("servers.yml");
						
						Map<File, String> fileContents = new HashMap<>();
						fileContents.put(serversYml, json.get("servers").getAsString());
						
						JsonObject menuFilesJson = json.get("menu").getAsJsonObject();
						for (Entry<String, JsonElement> menuJsonObjectEntryThing : menuFilesJson.entrySet()) {
						    retrievedConfigurationFiles.add("menu/" + menuJsonObjectEntryThing.getKey() + ".yml");
						    fileContents.put(new File(menuDirectory, menuJsonObjectEntryThing.getKey() + ".yml"),
						    menuJsonObjectEntryThing.getValue().getAsString());
						}
						
						sender.sendMessage(ChatColor.GRAY + "Writing to disk..");
						
						// Write to disk
						for (Entry<File, String> fileContent : fileContents.entrySet()) {
							FileConfiguration config = new YamlConfiguration();
							try {
								config.loadFromString(fileContent.getValue());
							} catch (InvalidConfigurationException e) {
								e.printStackTrace();
							}
							try {
								config.save(fileContent.getKey());
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						
						sender.sendMessage(ChatColor.GREEN + "Done! Retrieved the following config files: " + String.join(", ", retrievedConfigurationFiles));
					}
				});
			});
			return true;
		}
		
		return false;
	}

}
