package xyz.derkades.serverselectorx;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class Futures {

	// From PlaceholderAPI, licensed under GPLv3.
	// Find the original source code and full license text here:
	// https://github.com/PlaceholderAPI/PlaceholderAPI
	public static <T> void whenCompleteOnMainThread(@NotNull Plugin plugin, @NotNull CompletableFuture<T> future, @NotNull BiConsumer<T, Throwable> consumer) {
		future.whenComplete((value, exception) -> {
			if (Bukkit.isPrimaryThread()) {
				consumer.accept(value, exception);
			} else {
				Bukkit.getScheduler().runTask(plugin, () -> {
					consumer.accept(value, exception);
				});
			}
		});
	}

}
