package org.muehleisen.hannes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class ShowServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(ShowServlet.class);

	private static final int CACHE_SEC = 3600; // one hour

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// collect shows from request, call wsr

		response.setContentType("application/rss+xml");

		Map<String, String> shows = new HashMap<String, String>();

		String feeds = request.getParameter("f");
		if (feeds == null) {
			response.sendError(400,
					"Supply show names in ?f request parameter, separate with ','!");
			return;
		}
		String[] feedArr = feeds.split(",");

		for (String f : feedArr) {
			if ("".equals(f.trim())) {
				continue;
			}
			String show = f.replace("_", " ");
			String url = "http://en.wikipedia.org/wiki/List_of_" + f
					+ "_episodes";
			shows.put(show, url);
		}

		if (shows.size() < 1) {
			response.sendError(400,
					"Supply show names in ?f request parameter, separate with ','!");
			return;
		}

		String feedUrl = request.getRequestURL().append("?")
				.append(request.getQueryString()).toString();

		File cacheFile = new File(System.getProperty("java.io.tmpdir")
				+ File.separator + feedUrl.hashCode() + ".wsrcache");

		if (!cacheFile.exists()
				|| cacheFile.lastModified() < (System.currentTimeMillis() - CACHE_SEC * 1000)) {
			WikiShowRSS.writeFeed("", shows, new FileWriter(cacheFile));
		}
		new FileInputStream(cacheFile).getChannel()
				.transferTo(0, Long.MAX_VALUE,
						Channels.newChannel(response.getOutputStream()));
		response.getOutputStream().close();
	}
}
