package com.vmware.android.photogallery;

import java.io.IOException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

// http://stackoverflow.com/questions/5832287/what-goes-into-source-control
// http://stackoverflow.com/questions/16736856/what-should-be-in-my-gitignore-for-an-android-studio-project
public class PhotoGalleryFragment extends Fragment {
	private GridView mGridView;
	private static final String TAG = "PhotoGalleryFragment";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//mPages = 0;
		setRetainInstance(true);
		//new FetchItemsTask().execute(++mPages);
		//new FetchItemsTask().execute(current_page);
		new FetchItemsTask().execute();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container,
				false);

		mGridView = (GridView) v.findViewById(R.id.gridView);
		
		return v;
	}
	
	private class FetchItemsTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			new FlickrFetchr().fetchItems(1);
			return null;
		}
	}

}
