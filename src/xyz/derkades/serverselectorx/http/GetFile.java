package xyz.derkades.serverselectorx.http;

import com.google.common.base.Preconditions;
import org.bukkit.configuration.file.FileConfiguration;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import xyz.derkades.serverselectorx.Main;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GetFile extends HttpHandler {

	@Override
	public void service(Request request, Response response) throws Exception {
		Preconditions.checkArgument(request.getMethod() == Method.GET, "Must use GET method");
		request.getParameters().setEncoding(StandardCharsets.UTF_8);

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

		final Path file = Path.of(filePath);
		final String type = Files.probeContentType(file);
		response.setContentType(type != null ? type : "application/octet-stream");
		Files.copy(file, response.getOutputStream());
	}
}
