package org.muehleisen.hannes;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;

public class Episode {
	protected String name;
	protected Date date;
	protected String show;

	public Episode(String show, String name, Date date) {
		this.name = name;
		this.show = show;
		this.date = date;
	}

	@Override
	public String toString() {
		return name + " from " + show + " at " + date;
	}

	public SyndEntry toFeedEntry() throws UnsupportedEncodingException {
		String google = "http://www.google.com/search?q="
				+ URLEncoder.encode(name + " " + show, "UTF-8");

		String isohunt = "http://isohunt.com/torrents/?ihq="
				+ URLEncoder.encode(name + " " + show, "UTF-8");

		String text = show + " episode: " + name;

		SyndEntry entry = new SyndEntryImpl();
		entry.setTitle(text);
		entry.setLink(google);
		entry.setPublishedDate(date);

		SyndContent description = new SyndContentImpl();
		description.setType("text/html");
		description.setValue("<p>" + text + " (<a href='" + google
				+ "'>Google</a>, <a href='" + isohunt
				+ "'>isoHunt</a>)</p>");

		entry.setDescription(description);
		return entry;
	}
}