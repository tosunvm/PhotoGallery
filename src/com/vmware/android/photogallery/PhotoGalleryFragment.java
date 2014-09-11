package com.vmware.android.photogallery;

import java.io.IOException;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

// http://stackoverflow.com/questions/5832287/what-goes-into-source-control
// http://stackoverflow.com/questions/16736856/what-should-be-in-my-gitignore-for-an-android-studio-project
public class PhotoGalleryFragment extends Fragment {
	private GridView mGridView;
	private static final String TAG = "PhotoGalleryFragment";
	private ArrayList<GalleryItem> mItems;
	ThumbnailDownloader<ImageView> mThumbnailThread;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//mPages = 0;
		setRetainInstance(true);
		//new FetchItemsTask().execute(++mPages);
		//new FetchItemsTask().execute(current_page);
		new FetchItemsTask().execute();
		
		mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
	    mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
	        public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
	          if (isVisible()) {
	                imageView.setImageBitmap(thumbnail);
	            }
	        }
	    });

        mThumbnailThread.start();
        mThumbnailThread.getLooper();
        Log.i(TAG, "Background thread started");
	}
	
	@Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailThread.quit();
        Log.i(TAG, "Background thread destroyed");
    }
	
	@Override
	public void onDestroyView() {
	    super.onDestroyView();
	    mThumbnailThread.clearQueue();
	}


	public void testConflict(int myResolvedInt){
		int tempInt = 0;
		tempInt = myResolvedInt;
		Log.i(TAG, "Input integer: " + tempInt);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container,
				false);

		mGridView = (GridView) v.findViewById(R.id.gridView);
		setupAdapter();
		return v;
	}
	
	void setupAdapter() {
		if (getActivity() == null || mGridView == null)
			return;
		
		if (mItems != null) {

			//if (mGridView.getAdapter() == null) {
			mGridView.setAdapter(new GalleryItemAdapter(mItems));
			/* } else {
				// This makes sure newly added items to the data set get displayed.
				ArrayAdapter<GalleryItem> gItemsAdapter = (ArrayAdapter<GalleryItem>) mGridView
						.getAdapter();
				gItemsAdapter.notifyDataSetChanged();
			}
			*/

		} else {
			mGridView.setAdapter(null);
		}
	}

	private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {
		public GalleryItemAdapter(ArrayList<GalleryItem> items) {
			super(getActivity(), 0, items);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(
						R.layout.gallery_item, parent, false);
			}

			ImageView imageView = (ImageView) convertView
					.findViewById(R.id.gallery_item_imageView);
			imageView.setImageResource(R.drawable.brian_up_close);

			GalleryItem item = getItem(position);
			// Note that this is called as items come into view while you are scrolling
	        Log.i(TAG, "Just became visible: item in position " + position);
	        mThumbnailThread.queueThumbnail(imageView, item.getUrl());

			return convertView;
		}
	}
	
	private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>> {
		@Override
		protected ArrayList<GalleryItem> doInBackground(Void... params) {
			return new FlickrFetchr().fetchItems(1);
		}
		@Override
        protected void onPostExecute(ArrayList<GalleryItem> items) {
            if (mItems != null){
            	mItems.addAll(items);
            }
            else{
            	mItems = items;
            }
            setupAdapter();
		}
	}

}
