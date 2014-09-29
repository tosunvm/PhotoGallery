package com.vmware.android.photogallery;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;

public class ThumbnailDownloader<Token> extends HandlerThread {
	private static final String TAG = "ThumbnailDownloader";
	private static final int MESSAGE_DOWNLOAD = 0;

    Handler mHandler;
    Map<Token, String> requestMap =
            Collections.synchronizedMap(new HashMap<Token, String>());

    Handler mResponseHandler;
    Listener<Token> mListener;
    
    LruCache mThumbnailCache;

	public interface Listener<Token> {
		void onThumbnailDownloaded(Token token, Bitmap thumbnail);
	}

	public void setListener(Listener<Token> listener) {
		mListener = listener;
	}

	public ThumbnailDownloader(Handler responseHandler) {
		super(TAG);
		mResponseHandler = responseHandler;
		mThumbnailCache = new LruCache<String, Bitmap>(21);
	}

	public void queueThumbnail(Token token, String url) {
		Log.i(TAG, "Got an URL: " + url);
		requestMap.put(token, url);

		mHandler.obtainMessage(MESSAGE_DOWNLOAD, token).sendToTarget();
	}

	public void clearQueue() {
	    mHandler.removeMessages(MESSAGE_DOWNLOAD);
	    requestMap.clear();
	}

	@SuppressLint("HandlerLeak")
	@Override
	protected void onLooperPrepared() {
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == MESSAGE_DOWNLOAD) {
					@SuppressWarnings("unchecked")
					Token token = (Token) msg.obj;
					Log.i(TAG,
							"Got a request for url: " + requestMap.get(token));
					handleRequest(token);
				}
			}
		};
	}
	
	private void handleRequest(final Token token) {
		try {
			final String url = requestMap.get(token);
			if (url == null)
				return;
			Bitmap tmpBitmap = (Bitmap) mThumbnailCache.get(url);
			if (tmpBitmap == null){
				Log.i(TAG, "Lru Cache miss for: " + url);
				byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
				tmpBitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0,
						bitmapBytes.length);
				mThumbnailCache.put(url, tmpBitmap);
			}
			else{
				Log.i(TAG, "Lru Cache hit for: " + url);
			}
			//final Bitmap bitmap = Bitmap.createBitmap(tmpBitmap);
			final Bitmap bitmap = tmpBitmap;
			Log.i(TAG, "Bitmap created");
			mResponseHandler.post(new Runnable() {
	            public void run() {
	                if (requestMap.get(token) != url)
	                    return;

	                requestMap.remove(token);
	                mListener.onThumbnailDownloaded(token, bitmap);
	            }
	        });


		} catch (IOException ioe) {
			Log.e(TAG, "Error downloading image", ioe);
		}
	}

}
