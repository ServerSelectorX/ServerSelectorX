package xyz.derkades.serverselectorx.http;

import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonWriter;
import org.bukkit.configuration.file.FileConfiguration;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import xyz.derkades.serverselectorx.Main;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;

public class ListFiles extends HttpHandler {

	private static final int DIR_LIST_FILE_LIMIT = 1000;

	@Override
	public void service(Request request, Response response) throws Exception {
		Preconditions.checkArgument(request.getMethod() == Method.GET, "Must use GET method");

		final FileConfiguration api = Main.getConfigurationManager().getApiConfiguration();

		if (!api.getBoolean("files-api")) {
			response.getWriter().write("Files API disabled\n");
			response.setStatus(HttpStatus.FORBIDDEN_403);
			return;
		}

		final Logger logger = Main.getPlugin().getLogger();
		final String dirName = request.getParameter("dir");

		final File dir = new File(Main.getPlugin().getDataFolder(), dirName);
		if (!dir.isDirectory()) {
			logger.warning("Received bad request from " + request.getRemoteAddr());
			logger.warning("Requested to list files in " + dirName + ", but it is not a directory.");
			response.setStatus(HttpStatus.BAD_REQUEST_400);
			return;
		}

		final Deque<File> directories = new ArrayDeque<>();
		directories.push(dir);

		response.setContentType("text/json");
		final JsonWriter writer = Main.GSON.newJsonWriter(response.getWriter());
		writer.beginArray();

		int i = 0;
		while (!directories.isEmpty()) {
			i++;
			if (i > DIR_LIST_FILE_LIMIT) {
				logger.warning("Listing too many files! Received a request to list files in directory " + dir
						+ ", but there are a lot of files in this directory, apparently. To avoid crashing "
						+ "the server from running out of memory, SSX will only list "
						+ DIR_LIST_FILE_LIMIT + " in a directory.");
				logger.warning("If you're seeing this you're either doing something dumb with paths in the "
						+ "config sync configuration file, or your SSX API is not secured properly and "
						+ "someone else is having a look at the files on your server.");
				break;
			}

			final File file = directories.pop();
			if (file.isFile()) {
				writer.value(file.getPath());
			}

			if (file.isDirectory()) {
				for (final File f : file.listFiles()) {
					directories.push(f);
				}
			}
		}

		writer.endArray();
	}

}
