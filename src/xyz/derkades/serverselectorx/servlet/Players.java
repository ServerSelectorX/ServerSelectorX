package xyz.derkades.serverselectorx.servlet;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.gson.stream.JsonWriter;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.derkades.serverselectorx.Main;

public class Players extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		response.setContentType("text/json");
		final JsonWriter writer = Main.GSON.newJsonWriter(response.getWriter());
		writer.beginObject();
		for (final Player player : Bukkit.getOnlinePlayers()) {
			writer.name(player.getUniqueId().toString());
			writer.value(player.getName());
		}
		writer.endObject();
		response.setStatus(HttpServletResponse.SC_OK);
	}

}
