package com.vmware.android.photogallery;

import java.io.IOException;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

/**
 * 
 * @author stosun
 *
 *  Flickr id/password: tosunst/Y..1..(unlem)
 *	App Name: PhotoGallery Test
 *  Key: cc7bcb648f013d58d9b4929c71e592f8
 *  Secret: b231319634c7b049
 *
 */
public class PhotoGalleryFragment extends Fragment {
	GridView mGridView;
	private static final String TAG = "PhotoGalleryFragment";
	private ArrayList<GalleryItem> mItems;
	ThumbnailDownloader<ImageView> mThumbnailThread;
	
	private int mPages;
	private int mTotalCount = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPages = 1;
		setRetainInstance(true);
		setHasOptionsMenu(true);
		// new FetchItemsTask().execute(++mPages);
		updateItems();

		//Intent i = new Intent(getActivity(), PollService.class);
	    //getActivity().startService(i);
	    PollService.setServiceAlarm(getActivity(), true);


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

	public void updateItems() {
	    new FetchItemsTask().execute(mPages);
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

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container,
				false);

		mGridView = (GridView) v.findViewById(R.id.gridView);
		mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				if ((visibleItemCount > 0)
						&& ((firstVisibleItem + visibleItemCount) == totalItemCount)
						&& (totalItemCount > mTotalCount)) {

					mTotalCount = totalItemCount;
					new FetchItemsTask().execute(++mPages);
					Log.e(TAG, "Scrolled: current_page: " + mPages);
				}
			}

			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
			}

		});
		
		setupAdapter();

		return v;
	}

	void setupAdapter() {
		if (getActivity() == null || mGridView == null)
			return;
		
		if (mItems != null) {

			if (mGridView.getAdapter() == null) {
				mGridView.setAdapter(new GalleryItemAdapter(mItems));
			} else {
				// This makes sure newly added items to the data set get displayed.
				ArrayAdapter<GalleryItem> gItemsAdapter = (GalleryItemAdapter) mGridView
						.getAdapter();
				gItemsAdapter.notifyDataSetChanged();
			}

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

	private class FetchItemsTask extends AsyncTask<Integer, Void, ArrayList<GalleryItem>> {
		@Override
		protected ArrayList<GalleryItem> doInBackground(Integer... params) {
			// String query = "android"; // Just for testing
			Activity activity = getActivity();
	        if (activity == null)
	            return new ArrayList<GalleryItem>();

	        String query = PreferenceManager.getDefaultSharedPreferences(activity)
	            .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);


            if (query != null) {
            	// Pass activity for challenge 2 to be able to get total count back through
            	// the preferences file.
                return new FlickrFetchr().search(query, activity.getApplicationContext());
            } else {
            	return new FlickrFetchr().fetchItems(params[0], activity);
            }
		}
		
		@Override
        protected void onPostExecute(ArrayList<GalleryItem> items) {
            if (mItems != null){
            	mItems.addAll(items);
            }
            else{
            	mItems = items;
            }
            // Show the toast here for challenge 2? "Total # of results received is: nnn"
            // https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=cc7bcb648f013d58d9b4929c71e592f8&extras=url_s&text=banana
	        String totalResultCount = PreferenceManager.getDefaultSharedPreferences(getActivity())
		            .getString(FlickrFetchr.PREF_TOTAL_COUNT, null);
	        Toast.makeText(getActivity(), totalResultCount, Toast.LENGTH_LONG).show();

            setupAdapter();
		}
	}
	
	// Handle search via Options Menu
	// ==============================
	@Override
	@TargetApi(11)
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_photo_gallery, menu);
		
		// More info: http://developer.android.com/guide/topics/search/search-dialog.html
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Pull out the SearchView
			MenuItem searchItem = menu.findItem(R.id.menu_item_search);
			SearchView searchView = (SearchView) searchItem.getActionView();
			// Get the data from our searchable.xml as a SearchableInfo
			SearchManager searchManager = (SearchManager) getActivity()
					.getSystemService(Context.SEARCH_SERVICE);
			ComponentName name = getActivity().getComponentName();
			SearchableInfo searchInfo = searchManager.getSearchableInfo(name);

			searchView.setSearchableInfo(searchInfo);

			MenuItem cancelItem = menu.findItem(R.id.menu_item_clear);
			cancelItem.setVisible(false);
			
			searchView.setOnSearchClickListener(new OnClickListener() {

			    @Override
			    public void onClick(View v) {
			    	Log.e(TAG, "Do we even get to search button click from action bar with SearchView implementation?");
					String currentQuery = PreferenceManager.getDefaultSharedPreferences(getActivity())
							   .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
					((SearchView)v).setQuery(currentQuery, false);

			    }
			});
			
			searchView.setOnQueryTextListener(new OnQueryTextListener() {

			    @Override
			    public boolean onQueryTextSubmit(String query) {
			        //onSearchRequested();
			    	Log.e(TAG, "Do we even get to search submit with SearchView implementation?");
			    	mItems.clear();
			    	return false;
			    }

			    @Override
			    public boolean onQueryTextChange(String newText) {
			        // TODO Auto-generated method stub
			        return false;
			    }
			});
			
			// Note that with this you don't even get the menu_item_clear event below.
			//searchView.setIconifiedByDefault(false);
			
		}
		
	}

	@Override
	@TargetApi(11)
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.menu_item_search:
	            //getActivity().onSearchRequested();
	        	// Challenge 1
	        	// See if I can clear current results:
	        	Log.e(TAG, "Do we even get to menu_item_search with SearchView implementation?");
	        	mItems.clear();
	        	
		        String currentQuery = PreferenceManager.getDefaultSharedPreferences(getActivity())
	            									   .getString(FlickrFetchr.PREF_SEARCH_QUERY, null);

	            // Note that following has no affect on post-Honeycomb devices with SearchView support
	        	// This is based on phillips' comment at http://forums.bignerdranch.com/viewtopic.php?f=425&t=6994
		        //getActivity().startSearch(currentQuery, true, null, false);

		        return true;
	        case R.id.menu_item_clear:
	        	Log.e(TAG, "Do we even get to menu_item_clear with SearchView implementation?");
	        	PreferenceManager.getDefaultSharedPreferences(getActivity())
	        				     .edit()
                                 .putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
                                 .commit();
	        	updateItems();
	            return true;
	        case R.id.menu_item_toggle_polling:
	            boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
	            PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
	            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
	                getActivity().invalidateOptionsMenu();
	            return true;

	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
		if (PollService.isServiceAlarmOn(getActivity())) {
			toggleItem.setTitle(R.string.stop_polling);
		} else {
			toggleItem.setTitle(R.string.start_polling);
		}
	}

	// ==============================

}
