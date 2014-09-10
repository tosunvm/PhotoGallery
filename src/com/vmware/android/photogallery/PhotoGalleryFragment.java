package com.vmware.android.photogallery;

import java.io.IOException;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;

// http://stackoverflow.com/questions/5832287/what-goes-into-source-control
// http://stackoverflow.com/questions/16736856/what-should-be-in-my-gitignore-for-an-android-studio-project
public class PhotoGalleryFragment extends Fragment {
	private GridView mGridView;
	private static final String TAG = "PhotoGalleryFragment";
	private ArrayList<GalleryItem> mItems;
	private int mPages;
	private int mTotalCount = 0;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPages = 0;
		setRetainInstance(true);
		new FetchItemsTask().execute(++mPages);
		//new FetchItemsTask().execute();
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
				mGridView.setAdapter(new ArrayAdapter<GalleryItem>(
						getActivity(), android.R.layout.simple_gallery_item, mItems));
			} else {
				// This makes sure newly added items to the data set get displayed.
				ArrayAdapter<GalleryItem> gItemsAdapter = (ArrayAdapter<GalleryItem>) mGridView
						.getAdapter();
				gItemsAdapter.notifyDataSetChanged();
			}
		} else {
			mGridView.setAdapter(null);
		}
	}

	
	private class FetchItemsTask extends AsyncTask<Integer, Void, ArrayList<GalleryItem>> {
		@Override
		protected ArrayList<GalleryItem> doInBackground(Integer... params) {
			return new FlickrFetchr().fetchItems(params[0]);
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
            //fetched_page++;
		}
	}

}
