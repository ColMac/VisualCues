package com.visualcues;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class GetDownloads extends Activity {

	private GridView gView;
	private ArrayList<File> fileArray;
    private String fileName = null;
	private ArrayList<File> bmArray;
	private Utilities utils;
	private Context context;
	private ProgressDialog progressDialog = null;
	private boolean waiting = false;
	final int PIC_CROP = 2;

	@SuppressWarnings("unused")
	private String TAG = "GetDownloads ";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.get_downloads);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		if (gView != null) {
			if (newConfig.orientation == 1) {
				gView.setNumColumns(3);
			} else {
				gView.setNumColumns(4);
			}
		}
		waiting = true;
	}

	public void onResume() {
		super.onResume();

		Intent mainIntent = getIntent();
		fileName = mainIntent.getExtras().get("file_name").toString();

		gView = (GridView) findViewById(R.id.dl_grid_view);

		gView.setFadingEdgeLength(100);
		gView.setVerticalFadingEdgeEnabled(true);

		utils = new Utilities(this);
		context = this;

		String state = Environment.getExternalStorageState();
		if (state.contentEquals(Environment.MEDIA_MOUNTED)
				|| state.contentEquals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
			String homeDir = Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_DOWNLOADS).toString();
			File file = new File(homeDir);
            File[] directories = file.listFiles();


			fileArray = new ArrayList<File>();
			if (directories.length > 0) { // make sure there are actually files available.
				for (File files : directories) {
					if (files.getName().toLowerCase(Locale.US).endsWith(".jpg")) {
						fileArray.add(files);
					}
				}

				progressDialog = new ProgressDialog(this);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progressDialog
				.setTitle("Loading Downloads Gallery, this may take a while!");
				progressDialog.setCancelable(false);
				progressDialog.setProgressNumberFormat(null);
				progressDialog.show();

				LongOperation lo = new LongOperation();
				lo.setFragment(this);
				lo.execute("");
			} 
			
		} else {
			setResult(29);
			finish();
		}



		gView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {

				System.gc();
				try {

					Intent cropIntent = utils.performCrop(
							Uri.fromFile(fileArray.get(position)),
							GetDownloads.this);

					waiting = true;
					startActivityForResult(cropIntent, PIC_CROP);
				} catch (ActivityNotFoundException anfe) {
                    Log.d(TAG, anfe.toString());
				}
			}
		});
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == PIC_CROP) {

			// get the returned data
			Bundle extras = data.getExtras();
			// get the cropped bitmap
			Bitmap cropped = extras.getParcelable("data");

			utils.finalizeImage(cropped, context, fileName);
			setResult(RESULT_OK);
			waiting = false;
			finish();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}

	}

	public void onDestroy() {
		super.onDestroy();
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}

	}

	class LongOperation extends AsyncTask<String, String, String> {

		GetDownloads getDownloads;

		void setFragment(GetDownloads gDownloads) {
			getDownloads = gDownloads;
		}

		@Override
		protected String doInBackground(String... params) {

			FileOutputStream fosBMP;
			int index = 0;
			int size = utils.getScreenSize(context).y / 3;

			bmArray = new ArrayList<File>();
			File tempDir = new File(getFilesDir() + "/tmpImages/");
			if (!tempDir.isDirectory()) {
				tempDir.mkdir();
			}
			for (File tempFile : fileArray) {

				try {
					index++;
					File tempThumb = new File(tempDir.getPath() + "/photo"
							+ index + ".jpg");
					bmArray.add(tempThumb);
					fosBMP = new FileOutputStream(tempDir.getPath() + "/photo"
							+ index + ".jpg");
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inSampleSize = 6;
					Bitmap tempBMP = utils.makeThumb(tempFile.toString(), size,
							context, "", false);

					tempBMP.compress(Bitmap.CompressFormat.JPEG, 60, fosBMP);
					fosBMP.close();
				} catch (FileNotFoundException e) {

					e.printStackTrace();
				} catch (IOException e) {

					e.printStackTrace();
				}

				publishProgress("" + index * 100 / fileArray.size());

			}

			return "finished";
		}

		@Override
		protected void onPostExecute(String result) {

			if (getDownloads != null) {
				ImageAdapter imgAdapter = new ImageAdapter(context, 0, bmArray,
						gView, false, null, null, null);
				imgAdapter.notifyDataSetChanged();
				gView.invalidateViews();
				gView.setAdapter(imgAdapter);
				if (progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
			}
		}

		@Override
		protected void onPreExecute() {

		}

		protected void onProgressUpdate(String... progress) {

			if (getDownloads != null) {
				progressDialog.setProgress(Integer.parseInt(progress[0]));
			}
		}
	}

}
