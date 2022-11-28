package xyz.derkades.serverselectorx;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class Heads {

	private final Map<String, HeadHandler> handlers = new HashMap<>();

	Heads(JavaPlugin plugin) {
		Logger logger = plugin.getLogger();

		try {
			handlers.put("arc-hdb", new ArcaniaxHandler());
			logger.info("Integration with Arcaniax's Head Database plugin is active");
		} catch (Exception ignored) {}

		try {
			handlers.put("silent-hdb", new ArcaniaxHandler());
			logger.info("Integration with TheSilentPro's Head Database plugin is active");
		} catch (Exception ignored) {}

		handlers.put("uuid", new UuidHandler(plugin));
		handlers.put("texture", new TextureLiteralHandler());
	}

	public CompletableFuture<@Nullable String> getHead(final String identifier) throws InvalidConfigurationException {
		final String[] split = identifier.split(":");
		if (split.length != 2) {
			throw new InvalidConfigurationException("Invalid head '" + identifier + "'. Valid syntax is 'head:<type>:<value>' or 'head:self'");
		}

		final String type = split[0];
		final String value = split[1];

		if (!handlers.containsKey(type)) {
			throw new InvalidConfigurationException("Invalid head type: " + identifier);
		}

		return handlers.get(type).getHeadTexture(value);
	}

	private interface HeadHandler {

		CompletableFuture<@Nullable String> getHeadTexture(String name);

	}

	private static class ArcaniaxHandler implements HeadHandler {

		private final Object apiInstance;
		private final Method getBase64Method;

		private ArcaniaxHandler() throws Exception {
			Class<?> apiClass = Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI");
			final Constructor<?> constructor = apiClass.getConstructor();
			this.apiInstance = constructor.newInstance();
			this.getBase64Method = apiClass.getMethod("getBase64", String.class);
		}

		@Override
		public CompletableFuture<@Nullable String> getHeadTexture(String name) {
			final CompletableFuture<String> future = new CompletableFuture<>();
			try {
				final String value = (String) Objects.requireNonNull(getBase64Method.invoke(apiInstance, name));
				future.complete(value);
			} catch (Exception e) {
				future.completeExceptionally(e);
			}
			return future;
		}

	}

	private static class SilentHandler implements HeadHandler {

		private final Method getHeadByIdMethod;
		private final Method getValueMethod;

		private SilentHandler() throws Exception {
			Class<?> apiClass = Class.forName("tsp.headdb.api.HeadAPI");
			this.getHeadByIdMethod = apiClass.getMethod("getHeadByID");
			Class<?> headClass = Class.forName("tsp.headdb.implementation.Head");
			this.getValueMethod = headClass.getMethod("getValue");
		}

		@Override
		public CompletableFuture<@Nullable String> getHeadTexture(String name) {
			final CompletableFuture<String> future = new CompletableFuture<>();
			try {
				Object headInstance = getHeadByIdMethod.invoke(null);
				final String texture = (String) getValueMethod.invoke(headInstance);
				future.complete(texture);
			} catch (final Exception e) {
				future.completeExceptionally(e);
			}
			return future;
		}

	}

	private static class UuidHandler implements HeadHandler {

		private final JavaPlugin plugin;

		private UuidHandler(JavaPlugin plugin) {
			this.plugin = plugin;
		}

		@Override
		public CompletableFuture<@Nullable String> getHeadTexture(String name) {
			final CompletableFuture<String> future = new CompletableFuture<>();
			Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
				final UUID uuid = UUID.fromString(name);
				Main.getPlugin().getLogger().info("Getting texture value for " + uuid + " from Mojang API");
				try {
					final HttpURLConnection connection = (HttpURLConnection) new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString()).openConnection();
					try (final Reader reader = new InputStreamReader(connection.getInputStream())) {
						final JsonObject jsonResponse = (JsonObject) JsonParser.parseReader(reader);
						final String texture = jsonResponse.get("properties").getAsJsonArray().get(0).getAsJsonObject().get("value").getAsString();
						future.complete(texture);
					}
				} catch (final Exception e) {
					future.completeExceptionally(e);
				}
			});
			return future;
		}
	}

	private static class TextureLiteralHandler implements HeadHandler {

		@Override
		public CompletableFuture<@Nullable String> getHeadTexture(String name) {
			return CompletableFuture.completedFuture(name);
		}

	}

}
