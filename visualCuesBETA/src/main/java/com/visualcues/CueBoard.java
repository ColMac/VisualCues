package com.visualcues;

import java.io.File;
import java.util.ArrayList;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

public class CueBoard extends Activity implements OnInitListener {

	private int clickCount = 0;
	private ArrayList<File> fList;
	private ArrayList<ImageButton> readyChoices;
	private LinearLayout ll;
	private ArrayList<String> cueNames;
	private DatabaseHelper dbHelper;
	private TextView tv;
	private AudioPlayer ap = null;
	@SuppressWarnings("unused")
	private final String TAG = "CueBoard ";
	private static TextToSpeech textToSpeech = null;
	private SensorManager mSensorManager;
	private ShakeEventListener mSensorListener;

	Utilities utils = new Utilities(this);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.cueboard);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mSensorListener = new ShakeEventListener();
		
		ll = (LinearLayout) findViewById(R.id.button_holder);

	}

	public void onResume() {
		super.onResume();
		ap = null;
		textToSpeech = null;
		ll.removeAllViews();

		if (textToSpeech == null) {
			textToSpeech = new TextToSpeech(this, this);
		}
		Intent mainIntent = getIntent();

		cueNames = mainIntent.getStringArrayListExtra("cue_names");

		fList = new ArrayList<File>();
		dbHelper = new DatabaseHelper(this);
		for (String tempFilename : cueNames) {
			fList.add(dbHelper.getLargeImage(tempFilename, 1));
		}

		prepareChoices(fList.size(), fList);

		mSensorManager.registerListener(mSensorListener,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_UI);

		mSensorListener
				.setOnShakeListener(new ShakeEventListener.OnShakeListener() {

					public void onShake() {

						readyChoices.clear();
						ArrayList<File> shuffledList = new ArrayList<File>();
						ArrayList<String> shuffledNames = new ArrayList<String>();
						ll.removeAllViews();
						ll.setGravity(Gravity.CENTER_HORIZONTAL);
						ll.setGravity(Gravity.CENTER_VERTICAL);

						if (fList.size() == 1) {
							shuffledList.add(fList.get(0));
							shuffledNames.add(cueNames.get(0));
						} else if (fList.size() == 2) {
							for (int index = (fList.size() - 1); index >= 0; index--) {
								shuffledList.add(fList.get(index));
							}
							for (int index = (cueNames.size() - 1); index >= 0; index--) {
								shuffledNames.add(cueNames.get(index));
							}
						} else if (fList.size() == 3) {
							shuffledList.add(fList.get(2));
							shuffledList.add(fList.get(0));
							shuffledList.add(fList.get(1));

							shuffledNames.add(cueNames.get(2));
							shuffledNames.add(cueNames.get(0));
							shuffledNames.add(cueNames.get(1));
						} else {
							// do nothing.
						}

						cueNames = shuffledNames;
						fList = shuffledList;
						prepareChoices(shuffledList.size(), shuffledList);
					}
				});

	}

	/**
	 * Dynamically creates an Array of ImageButtons
	 * 
	 * @param count
	 *            How many buttons
	 * @param files
	 *            ArrayList<File> containing image locations.
	 * @return ArrayList<ImageButton>.
	 */
	private ArrayList<ImageButton> makeButtonArray(int count,
			ArrayList<File> files) {

		ArrayList<ImageButton> buttonArray = new ArrayList<ImageButton>();

		for (int index = 0; index < count; index++) {
			ImageButton iButton = new ImageButton(this);

			Resources r = getResources();
			Drawable[] layers = new Drawable[2];
			layers[0] = Drawable.createFromPath(files.get(index).toString());
			layers[0].setBounds(20, 20, 20, 20);
			layers[1] = r.getDrawable(R.drawable.photo_border_bg);
			LayerDrawable layerDrawable = new LayerDrawable(layers);
			iButton.setImageDrawable(layerDrawable);

			iButton.setId(index);
			buttonArray.add(iButton);
		}
		return buttonArray;
	}

	/**
	 * Creates a LinearLayout containing dynamically created Image Buttons. Sets
	 * onClick listeners for each button.
	 * 
	 * @param count
	 * @param files
	 * @return LinearLayout ready for inflation.
	 */
	public LinearLayout prepareChoices(int count, ArrayList<File> file_list) {

		fList = file_list;
		clickCount = 0;

		readyChoices = makeButtonArray(count, fList);

		for (int index = 0; index < readyChoices.size(); index++) {
			ImageButton imgButton = readyChoices.get(index);
			imgButton.setAdjustViewBounds(true);
			imgButton.setScaleType(ScaleType.CENTER_INSIDE);
			imgButton.setLayoutParams(new TableLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
			imgButton.setBackgroundColor(Color.BLACK);

			ll.addView(imgButton); // add ImageButtons to view.
		}

		for (int index = 0; index < readyChoices.size(); index++) {
			readyChoices.get(index).setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {

					choiceMade(fList, cueNames, view.getId());
					for (int index = 0; index < readyChoices.size(); index++) {
						if (readyChoices.get(index).getId() != view.getId()) {
							readyChoices.get(index).setVisibility(
									ImageButton.GONE);
						}
					}

				}

			});
		}

		return ll;
	}

	/**
	 * When choice is made prepares layout for result.
	 * 
	 * @param list
	 * @param location
	 */
	private void choiceMade(ArrayList<File> list, ArrayList<String> cues,
			int location) {

		int textSize = (utils.getScreenSize(this).x / 2);
		tv = new TextView(this);

		tv.setText(cues.get(location).toUpperCase());
		ll.setGravity(Gravity.CENTER_VERTICAL);

		tv.setTextColor(Color.WHITE);
		tv.setShadowLayer(35, 5, 5, Color.GREEN);

		tv.setTextSize((textSize / cues.get(location).length()) + 10);
		tv.setVisibility(TextView.VISIBLE);
		tv.bringToFront();
		if (clickCount < 1) {
			ll.addView(tv);
			clickCount++;
		}
		File checkForAudio = new File(this.getFilesDir() + "/vq_audio/"
				+ cues.get(location) + ".3gp");
		if (checkForAudio.isFile()) {

			ap = new AudioPlayer(cues.get(location), this);

			try {
				ap.play();
			} catch (IllegalStateException e) {

			}

		} else {

			if (textToSpeech == null) {

				textToSpeech.setSpeechRate((float) 0.85);
				textToSpeech.setPitch((float) 0.1);

			}

			if (!textToSpeech.isSpeaking()) {

				String verb = "I chose ";
				String category = dbHelper.getCategory(cues.get(location));
				if (category.equals("People")) {
					verb = "I chose ";
				} else if (category.equals("Places")) {
					verb = "I want to go to the ";
				} else if (category.equals("Things")) {
					verb = "I want the ";
				}
				textToSpeech.speak(verb + cues.get(location),
						TextToSpeech.QUEUE_FLUSH, null);
			}
		}
	}

	@Override
	protected void onPause() {
		mSensorManager.unregisterListener(mSensorListener);

		if (textToSpeech != null) {
			textToSpeech.shutdown();
			textToSpeech = null;
		}

		if (dbHelper.db.isOpen()) {
			dbHelper.close();
		}
		if (!this.isFinishing()) {
			finish();
		}
		super.onPause();
	}

	protected void onDestroy() {

		mSensorManager.unregisterListener(mSensorListener);
		if (textToSpeech != null) {
			textToSpeech.shutdown();
			textToSpeech = null;

		}
		if (dbHelper.db.isOpen()) {
			dbHelper.close();
		}
		super.onDestroy();
	}

	@Override
	public void onInit(int status) {
		// TODO Auto-generated method stub

	}
}
