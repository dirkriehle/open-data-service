package org.jvalue.ods.utils;

import org.jvalue.commons.utils.Assert;
import org.jvalue.commons.utils.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;


public class HttpUtils {

	private HttpUtils () { } 

	public static String readUrl(URL url, String charsetName) throws IOException {
		Assert.assertNotNull(url, charsetName);

		BufferedReader reader = null;
		try {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");

			reader = new BufferedReader(new InputStreamReader(
					connection.getInputStream(),
					Charset.forName(charsetName)));

			StringBuilder stringBuilder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
			}
			return stringBuilder.toString();

		} finally {
			try {
				if (reader != null)  reader.close();
			} catch (IOException e) {
				Log.error("Failed to close reader", e);
			}
		}
	}

}
