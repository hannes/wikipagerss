package org.muehleisen.hannes;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.any23.extractor.ExtractionException;
import org.apache.log4j.Logger;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.SyndFeedOutput;

public class WikiShowRSS {
	static Logger log = Logger.getLogger(WikiShowRSS.class);

	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	private static final int CACHE_SEC = 21600; // cache for 12 hours

	public static class ICALTZD {
		public static final String PREFIX = "http://www.w3.org/2002/12/cal/icaltzd#";
		public static final String VEVENT = PREFIX + "Vevent";
		public static final String SUMMARY = PREFIX + "summary";
		public static final String DTSTART = PREFIX + "dtstart";
	}

	private static Comparator<Episode> dateDescSorter = new Comparator<Episode>() {
		@Override
		public int compare(Episode o1, Episode o2) {
			return o1.date.compareTo(o2.date) * -1;
		}
	};

	public List<Episode> getAllAndSort(Map<String, String> shows)
			throws Exception {
		List<Episode> episodes = new ArrayList<Episode>();
		for (Entry<String, String> show : shows.entrySet()) {
			episodes.addAll(get(show.getValue(), show.getKey()));
		}

		Collections.sort(episodes, dateDescSorter);
		return episodes;
	}

	static SimpleDateFormat rssDate = new SimpleDateFormat(
			"dd MMM yyyy HH:mm:ss Z");

	public static void main(String[] args) {
		Map<String, String> testShows = new HashMap<String, String>();
		testShows.put("MythBusters",
				"http://en.wikipedia.org/wiki/List_of_MythBusters_episodes");

		testShows.put("Family Guy",
				"http://en.wikipedia.org/wiki/List_of_Family_Guy_episodes");

		testShows.put("South Park",
				"http://en.wikipedia.org/wiki/List_of_South_Park_episodes");

		testShows.put("Raising Hope",
				"http://en.wikipedia.org/wiki/List_of_Raising_Hope_episodes");

		testShows
				.put("My Name is Earl",
						"http://en.wikipedia.org/wiki/List_of_My_Name_is_Earl_episodes");

		writeFeed("http://example.com/feed", testShows, new OutputStreamWriter(
				System.out));
	}

	public static void writeFeed(String link, Map<String, String> shows,
			Writer outputWriter) {

		WikiShowRSS rss = new WikiShowRSS();

		String showTitles = "";
		List<Entry<String, String>> a = new ArrayList<Entry<String, String>>(
				shows.entrySet());
		for (int i = 0; i < a.size(); i++) {
			showTitles += a.get(i).getKey();
			if (i < a.size() - 2) {
				showTitles += ", ";
			}
			if (i == a.size() - 2) {
				showTitles += " and ";
			}
		}

		try {
			SyndFeed feed = new SyndFeedImpl();
			feed.setFeedType("rss_2.0");
			feed.setLink(link);
			feed.setTitle("Recent " + showTitles + " Episodes");
			feed.setDescription("Recent Episodes for " + showTitles
					+ ",  scraped from Wikipedia pages.");
			List<SyndEntry> entries = new ArrayList<SyndEntry>();

			List<Episode> eps = rss.getAllAndSort(shows);

			for (Episode e : eps) {
				entries.add(e.toFeedEntry());
			}

			feed.setEntries(entries);

			SyndFeedOutput output = new SyndFeedOutput();
			output.output(feed, outputWriter);
			outputWriter.close();

		} catch (Exception e) {
			log.warn(e);
		}
	}

	private List<Episode> get(String url, String show) throws IOException,
			URISyntaxException, ExtractionException, RepositoryException,
			MalformedQueryException, QueryEvaluationException, ParseException {

		// setup
		Repository memRepo = new SailRepository(new MemoryStore());
		memRepo.initialize();
		RepositoryConnection repoConn = memRepo.getConnection();

		WikiPage page = new WikiPage(show, url, CACHE_SEC);
		repoConn.add(page.getTriples());

		String spargel = "SELECT ?name ?date WHERE { ?episode a <"
				+ ICALTZD.VEVENT + "> . ?episode <" + ICALTZD.SUMMARY
				+ "> ?name . ?episode <" + ICALTZD.DTSTART + "> ?date . }";

		TupleQueryResult res = repoConn.prepareTupleQuery(QueryLanguage.SPARQL,
				spargel).evaluate();

		List<Episode> episodes = new ArrayList<Episode>();

		while (res.hasNext()) {
			BindingSet bs = res.next();
			String name = bs.getBinding("name").getValue().stringValue();
			
			// filter out some ugly stuff
			name = name.replaceAll("\\[\\d+\\]", "");
			name = name.replaceAll("\"", "");
			name = name.replaceAll("\n", " / ");

			Date date = dateFormat.parse(bs.getBinding("date").getValue()
					.stringValue());

			// filter out future events (cant watch!)
			if (date.after(Calendar.getInstance().getTime())) {
				continue;
			}
			episodes.add(new Episode(show, name, date));
		}
		return episodes;
	}

}
