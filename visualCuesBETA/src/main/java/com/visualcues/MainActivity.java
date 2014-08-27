package com.visualcues;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Colin Maccannell (cmaccannell@gmail.com)
 * 
 */
public class MainActivity extends Activity {

	private Button setupCues, addPhoto, clearChoices;
	private GridView photoView;
	private ArrayList<String> ar_list = new ArrayList<String>();
	private LinearLayout choiceBox, space_holder, button_holder, base_holder;
	private Context mContext;
	private ImageAdapter imgAdapter;
	private ListView categories;
	private DatabaseHelper dbHelper;
	private final String[] values = new String[] { "ALL", "People", "Places",
			"Things" };
	private int currentSelection;
	@SuppressWarnings("unused")
	private final String TAG = "MainActivity ";
	private TextView noCuesText;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		photoView = (GridView) findViewById(R.id.gridView1);
		choiceBox = (LinearLayout) findViewById(R.id.chosen_content);
		setupCues = (Button) findViewById(R.id.cues_selected);
		clearChoices = (Button) findViewById(R.id.clear_choices);
		addPhoto = (Button) findViewById(R.id.add_cue);
		categories = (ListView) findViewById(R.id.category_list);
		space_holder = (LinearLayout) findViewById(R.id.space_holder);
		button_holder = (LinearLayout) findViewById(R.id.button_holder);
		base_holder = (LinearLayout) findViewById(R.id.content);
		noCuesText = (TextView) findViewById(R.id.no_cues_text);

		photoView.setFadingEdgeLength(100);
		photoView.setVerticalFadingEdgeEnabled(true);

		mContext = this;
		
	}

	@Override
	public void onResume() {
		super.onResume();

		choiceBox.removeAllViews();
		
		noCuesText.setVisibility(TextView.GONE);
		photoView.setVisibility(GridView.VISIBLE);

		if (getResources().getConfiguration().orientation == 1 /* portrait */) {
			
			photoView.setNumColumns(2);
			space_holder.setOrientation(1);
			button_holder.setOrientation(2);

		} else { /* landscape */
			
			photoView.setNumColumns(4);
			space_holder.setOrientation(2);
			button_holder.setOrientation(1);
		}
		space_holder.setVisibility(LinearLayout.GONE);

		ar_list.clear();
		
		buildGallery("ALL");
		dbHelper = new DatabaseHelper(this);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.custom_checked_list, android.R.id.text1,
				values);
		categories.setAdapter(adapter);
		categories.setItemChecked(0, true);

		addPhoto.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(mContext, CueEditor.class);

				startActivity(intent);
			}
		});

		setupCues.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				ar_list = imgAdapter.getArray();

				if (ar_list.size() > 0) {

					ArrayList<String> cleanArray = new ArrayList<String>();
					for (String temp : ar_list) {
						if (temp != null) {
							cleanArray.add(temp);
						}
					}
					Intent setupCues = new Intent(mContext, CueBoard.class);
					setupCues.putStringArrayListExtra("cue_names", cleanArray);
					startActivity(setupCues);
					ar_list.clear();
				} else {
					Toast warning = Toast
							.makeText(mContext,
									"You must select atleast 1 cue!",
									Toast.LENGTH_LONG);
					warning.setGravity(Gravity.CENTER, 0, 0);
					warning.setMargin(20, 20);
					warning.show();
				}
			}
		});

		clearChoices.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				imgAdapter.clearArray();
				ar_list.clear();
				choiceBox.removeAllViews();

				space_holder.setVisibility(LinearLayout.GONE);
			

			}
		});

		categories.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				if (position != currentSelection) {

					try {
						ar_list = imgAdapter.getArray();
						if (buildGallery(parent.getItemAtPosition(position).toString())) {
							categories.setItemChecked(position, true);
							currentSelection = position;
						} else {
							categories.setItemChecked(0, true);
							currentSelection = 0;
						}
					} catch(NullPointerException e) {
						
					}
				}

			}
		});

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		
		if (newConfig.orientation == 1 /* Portrait */) {
			
			photoView.setNumColumns(2);
			space_holder.setOrientation(1);
			button_holder.setOrientation(2);
			
		} else if (newConfig.orientation == 2 /* landscape */) {
			
			photoView.setNumColumns(4);
			space_holder.setOrientation(2);
			button_holder.setOrientation(1);
			
		}
		super.onConfigurationChanged(newConfig);
	}

	/**
	 * 
	 */
	private boolean buildGallery(String category) {

		ar_list.trimToSize();

		DatabaseHelper dbHelper = new DatabaseHelper(this);
		Cursor c = dbHelper.queryAll(category);
		ArrayList<File> aFile = new ArrayList<File>();
		if (c.getCount() > 0) { // database is not empty for chosen category.
			while (c.moveToNext()) {

				aFile.add(new File(c.getString(3)));
			}
			
			imgAdapter = new ImageAdapter(this, 0, aFile, photoView, true,
					ar_list, choiceBox, space_holder);
			imgAdapter.notifyDataSetChanged();
			photoView.invalidateViews();
			photoView.setAdapter(imgAdapter);
			if (!c.isClosed()) {
				c.close();
			}
			if (dbHelper.db.isOpen()) {
				dbHelper.close();
			}
			return true;

		} else {
			if (imgAdapter != null) {
				imgAdapter.notifyDataSetChanged();
				photoView.invalidateViews();
				photoView.setAdapter(imgAdapter);
			}
			if (!category.equals("ALL")) { 
				buildGallery("ALL");
				if (!c.isClosed()) {
					c.close();
				}
				if (dbHelper.db.isOpen()) {
					dbHelper.close();
				}
				return false;
			} else { // database is empty.
				photoView.setVisibility(GridView.GONE);
				noCuesText.setVisibility(TextView.VISIBLE);
			}
		}
		if (!c.isClosed()) {
			c.close();
		}
		if (dbHelper.db.isOpen()) {
			dbHelper.close();
		}
		return false;
	}

	@Override
	public void onPause() {

		if (dbHelper.db.isOpen()) {
			dbHelper.close();
		}
		
		super.onPause();
	}

	public void onDestroy() {

		if (dbHelper.db.isOpen()) {
			dbHelper.close();
		}
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK && requestCode == 0) {
			buildGallery("ALL");
		}
	}
	class ViewHolder {
		
	}
}