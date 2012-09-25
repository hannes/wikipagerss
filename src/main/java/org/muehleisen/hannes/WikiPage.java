package org.muehleisen.hannes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.any23.Any23;
import org.apache.any23.extractor.ExtractorFactory;
import org.apache.any23.extractor.ExtractorGroup;
import org.apache.any23.extractor.html.HCalendarExtractor;
import org.apache.any23.http.DefaultHTTPClient;
import org.apache.any23.http.DefaultHTTPClientConfiguration;
import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.HTTPDocumentSource;
import org.apache.any23.writer.NTriplesWriter;
import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.rio.ntriples.NTriplesParser;

public class WikiPage {
	private String show;
	private String url;
	private int cacheSecs;

	private static Logger log = Logger.getLogger(WikiPage.class);

	private File cacheFile;

	private static ExtractorGroup extractorGroup;
	static {
		List<ExtractorFactory<?>> factories = new ArrayList<ExtractorFactory<?>>();
		factories.add(HCalendarExtractor.factory);
		extractorGroup = new ExtractorGroup(factories);
	}

	Any23 extractor = new Any23(extractorGroup);

	private static DefaultHTTPClient httpClient = new DefaultHTTPClient();
	static {
		httpClient.init(DefaultHTTPClientConfiguration.singleton());
	}

	public WikiPage(String show, String url, int cacheSecs) {
		this.show = show;
		this.url = url;
		this.cacheSecs = cacheSecs;
		this.cacheFile = new File(System.getProperty("java.io.tmpdir")
				+ File.separator + url.hashCode() + ".wsrtriplecache");
	}

	private Collection<Statement> getTriples() {
		if (isCacheInvalid()) {
			log.debug("Refreshing cache for " + show);
			retrieve();
		} else {
			log.debug("Using cached triples for " + show + " from " + cacheFile);
		}
		return loadTriples();

	}

	public Collection<Episode> getEpisodes() {

	}

	private boolean isCacheInvalid() {
		return !cacheFile.exists()
				|| cacheFile.lastModified() < (System.currentTimeMillis() - cacheSecs * 1000);

	}

	private Collection<Statement> loadTriples() {
		NTriplesParser p = new NTriplesParser();
		StatementCollector sc = new StatementCollector();
		p.setRDFHandler(sc);
		try {
			p.parse(new FileInputStream(cacheFile), "p:base#");
		} catch (Exception e) {
			WikiShowRSS.log.warn("Failed to load triples from " + cacheFile, e);
		}
		return sc.getStatements();
	}

	private boolean retrieve() {
		log.debug("Loading and parsing data from " + url + " to " + cacheFile);
		try {
			// get content
			DocumentSource source = new HTTPDocumentSource(httpClient, this.url);

			// prepare writer, nt to cache file
			NTriplesWriter writer = new NTriplesWriter(new FileOutputStream(
					cacheFile));

			// run extractor
			extractor.extract(source, writer);
			writer.close();

			return true;
		} catch (Exception e) {
			WikiShowRSS.log.warn("Unable to retrieve data from " + this.url
					+ " to " + this.cacheFile, e);
		}
		return false;
	}
}