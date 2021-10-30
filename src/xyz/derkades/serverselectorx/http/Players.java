package xyz.derkades.serverselectorx.http;

import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import xyz.derkades.serverselectorx.Main;

public class Players extends HttpHandler {

	@Override
	public void service(Request request, Response response) throws Exception {
		Validate.isTrue(request.getMethod() == Method.GET, "Must use GET method");

		response.setContentType("text/json");
		final JsonWriter writer = Main.GSON.newJsonWriter(response.getWriter());
		writer.beginObject();
		for (final Player player : Bukkit.getOnlinePlayers()) {
			writer.name(player.getUniqueId().toString());
			writer.value(player.getName());
		}
		writer.endObject();
	}

}
