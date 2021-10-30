package xyz.derkades.serverselectorx.http;

import org.apache.commons.lang3.Validate;
import org.bukkit.configuration.file.FileConfiguration;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import xyz.derkades.serverselectorx.Main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GetFile extends HttpHandler {

	@Override
	public void service(Request request, Response response) throws Exception {
		Validate.isTrue(request.getMethod() == Method.GET, "Must use GET method");

		final FileConfiguration api = Main.getConfigurationManager().getApiConfiguration();

		if (!api.getBoolean("files-api")) {
			response.getWriter().write("Files API disabled\n");
			response.setStatus(HttpStatus.FORBIDDEN_403);
			return;
		}

		final String filePath = request.getParameter("file");

		if (filePath == null) {
			response.getWriter().write("File not specified\n");
			response.setStatus(HttpStatus.BAD_REQUEST_400);
			return;
		}

		final Path file = Paths.get(filePath);
		final String type = Files.probeContentType(file);
		response.setContentType(type != null ? type : "application/octet-stream");
		Files.copy(file, response.getOutputStream());
	}
}
