package xyz.derkades.serverselectorx.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.bukkit.configuration.file.FileConfiguration;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import xyz.derkades.serverselectorx.Main;

public class GetFile extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		final FileConfiguration api = Main.getConfigurationManager().api;

		if (!api.getBoolean("files-api")) {
			response.getWriter().println("Files API disabled");
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		
		response.setStatus(HttpServletResponse.SC_OK);

		final String fileName = request.getParameter("file");
		final File file = new File(fileName);
		try (InputStream input = new FileInputStream(file)) {
			IOUtils.copy(input, response.getOutputStream());
		}
	}

}
