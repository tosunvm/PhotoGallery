package com.vmware.android.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.net.Uri;
import android.util.Log;

public class FlickrFetchr {
	private static final String TAG = "FlickrFetchr";
	private static final String ENDPOINT = "https://api.flickr.com/services/rest/";
    private static final String API_KEY = "cc7bcb648f013d58d9b4929c71e592f8";
    private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String PARAM_EXTRAS = "extras";

    private static final String PAGE = "page";
    private static final String EXTRA_SMALL_URL = "url_s";
	
	byte[] getUrlBytes(String urlSpec) throws IOException {
		URL url = new URL(urlSpec);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			// Gives File not found exception for:
			// http://api.flickr.com/services/rest/?method=flickr.photos.getRecent&api_key=cc7bcb648f013d58d9b4929c71e592f8&extras=url_s
			// It should be an https url.
			InputStream in = connection.getInputStream();

			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return null;
			}

			int bytesRead = 0;
			byte[] buffer = new byte[1024];
			while ((bytesRead = in.read(buffer)) > 0) {
				out.write(buffer, 0, bytesRead);
			}
			out.close();
			return out.toByteArray();
		} finally {
			connection.disconnect();
		}
	}

	public String getUrl(String urlSpec) throws IOException {
		return new String(getUrlBytes(urlSpec));
	}
	public void fetchItems(Integer page) {
		// ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
		try {
			String url = Uri.parse(ENDPOINT).buildUpon()
					.appendQueryParameter("method", METHOD_GET_RECENT)
					.appendQueryParameter("api_key", API_KEY)
					.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
					.appendQueryParameter(PAGE, page.toString())
					.build().toString();
			Log.i(TAG, "URL called: " + url);
			String xmlString = getUrl(url);
			longInfo("Received xml: " + xmlString);
			
			//XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			//XmlPullParser parser = factory.newPullParser();
			//parser.setInput(new StringReader(xmlString));

			//parseItems(items, parser);

		} catch (IOException ioe) {
			Log.e(TAG, "Failed to fetch items", ioe);
		} 
		//catch (XmlPullParserException xppe) {
		//	Log.e(TAG, "Failed to parse items", xppe);
		//}
		//return items;

	}
	
	public static void longInfo(String str) {
	    if(str.length() > 4000) {
	        Log.i(TAG, str.substring(0, 4000));
	        longInfo(str.substring(4000));
	    } else
	        Log.i(TAG, str);
	}

}
