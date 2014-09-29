package com.vmware.android.photogallery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

public class FlickrFetchr {
	public static final String TAG = "FlickrFetchr";
	public static final String PREF_SEARCH_QUERY = "searchQuery";
	public static final String PREF_TOTAL_COUNT = "N/A";
	public static final String PREF_LAST_RESULT_ID = "lastResultId";
	
	private static final String ENDPOINT = "https://api.flickr.com/services/rest/";
    private static final String API_KEY = "cc7bcb648f013d58d9b4929c71e592f8";
    private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String METHOD_SEARCH = "flickr.photos.search";
    private static final String PARAM_EXTRAS = "extras";
    private static final String PARAM_TEXT = "text";

    private static final String PAGE = "page";
    private static final String EXTRA_SMALL_URL = "url_s";
    private static final String XML_PHOTO = "photo";
    private static final String XML_PHOTOS = "photos";
    
    private Context mAppContext;
    
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

	void parseItems(ArrayList<GalleryItem> items, XmlPullParser parser)
			throws XmlPullParserException, IOException {
		int eventType = parser.next();

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG
					&& XML_PHOTO.equals(parser.getName())) {
				String id = parser.getAttributeValue(null, "id");
				String caption = parser.getAttributeValue(null, "title");
				String smallUrl = parser.getAttributeValue(null,
						EXTRA_SMALL_URL);

				GalleryItem item = new GalleryItem();
				item.setId(id);
				item.setCaption(caption);
				item.setUrl(smallUrl);
				items.add(item);
			}
			else if (eventType == XmlPullParser.START_TAG
					&& XML_PHOTOS.equals(parser.getName())) {
				String totalResultCount = parser.getAttributeValue(null, "total");
	        	PreferenceManager.getDefaultSharedPreferences(mAppContext)
	        					 .edit()
	        					 .putString(FlickrFetchr.PREF_TOTAL_COUNT, totalResultCount)
	        					 .commit();
			}

			eventType = parser.next();
		}
	}

	public ArrayList<GalleryItem> fetchItems(Integer page, Context appContext) {
		String url = Uri.parse(ENDPOINT).buildUpon()
				.appendQueryParameter("method", METHOD_GET_RECENT)
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
				.appendQueryParameter(PAGE, page.toString())
				.build().toString();
		mAppContext = appContext;
		return downloadGalleryItems(url);

	}
	
	/*
	 * I added activity here only for challenge 2. This is to be able to write total result count
	 * into the preferences file so I can pass it back to the main thread for toast display.
	 */
	public ArrayList<GalleryItem> search(String query, Context appContext) {
		String url = Uri.parse(ENDPOINT).buildUpon()
				.appendQueryParameter("method", METHOD_SEARCH)
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
				.appendQueryParameter(PARAM_TEXT, query)
				.build().toString();
		mAppContext = appContext;
		return downloadGalleryItems(url);
	}
	
	public ArrayList<GalleryItem> downloadGalleryItems(String url) {
		ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
		try {
			Log.i(TAG, "URL called: " + url);
			String xmlString = getUrl(url);
			//Log.i(TAG, "Received xml: " + xmlString);
			//longInfo("Received xml: " + xmlString);
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new StringReader(xmlString));

			parseItems(items, parser);

		} catch (IOException ioe) {
			Log.e(TAG, "Failed to fetch items", ioe);
		} catch (XmlPullParserException xppe) {
			Log.e(TAG, "Failed to parse items", xppe);
		}
		return items;

	}
	
	public static void longInfo(String str) {
	    if(str.length() > 4000) {
	        Log.i(TAG, str.substring(0, 4000));
	        longInfo(str.substring(4000));
	    } else
	        Log.i(TAG, str);
	}

}