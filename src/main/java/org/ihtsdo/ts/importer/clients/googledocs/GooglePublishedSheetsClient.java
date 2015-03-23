package org.ihtsdo.ts.importer.clients.googledocs;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GooglePublishedSheetsClient {

	private String googleSpreadsheetPublishedUrl;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public GooglePublishedSheetsClient(String googleSpreadsheetPublishedUrl) {
		this.googleSpreadsheetPublishedUrl = googleSpreadsheetPublishedUrl;
	}

	public List<String> getColumnValues() throws GooglePublishedSheetsClientException {
		return getColumnValues(0);
	}

	public List<String> getColumnValues(int zeroBasedColumnIndex) throws GooglePublishedSheetsClientException {
		if (this.googleSpreadsheetPublishedUrl != null) {
			// Select table cells using css selector
			try {
				Document document = Jsoup.connect(this.googleSpreadsheetPublishedUrl).get();
				Elements elements = document.select("td:nth-child(" + zeroBasedColumnIndex + 2 + ")");
				List<String> values = new ArrayList<>();
				for (Element element : elements) {
					values.add(element.text());
				}
				return values;
			} catch (IOException e) {
				throw new GooglePublishedSheetsClientException("Failed retrieve published Google spreadsheet.", e);
			}
		} else {
			logger.warn("No Google spreadsheet URL is configured.");
			return null;
		}
	}

}
