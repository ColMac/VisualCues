package com.visualcues;

import java.io.File;
import java.util.Locale;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class CueEditor extends Activity {

	private Button finished, delete, changeName, changeImage;
	private ImageView image;
	private String cueName = "null";
	private String path = "null";
	private Context tContext;
	private DatabaseHelper dbHelper;
	@SuppressWarnings("unused")
	private final String TAG = "CueEditor.java ";
	private Spinner category_chooser;
	private TextView titleText = null, categoryName;
	boolean newPhoto = false;
	private Dialog dialog = null;
	private EditText nameEntry;
	private Utilities utils;
	private final String IMG_DIR = "/vq_images/";
	private final String THUMB_DIR = IMG_DIR + "thumbs/";
	private final String THUMB_END = "_thumb.jpg";
	private final String IMG_TYPE = ".jpg";
	private LinearLayout editHolder;
	private int orientation;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.cueeditor);

		finished = (Button) findViewById(R.id.finished);
		delete = (Button) findViewById(R.id.del_cue);
		changeName = (Button) findViewById(R.id.changeName);
		changeImage = (Button) findViewById(R.id.change_picture);
		image = (ImageView) findViewById(R.id.current_image);
		category_chooser = (Spinner) findViewById(R.id.category_spinner);
		titleText = (TextView) findViewById(R.id.cue_name);
		categoryName = (TextView) findViewById(R.id.category_name);
		editHolder = (LinearLayout) findViewById(R.id.edit_holder);
		tContext = this;
	}

	@Override
	public void onResume() {
		super.onResume();

		String currentCategory = "Category";
		dbHelper = new DatabaseHelper(this);
		utils = new Utilities(tContext);

		Intent mainIntent = getIntent();

		if (getResources().getConfiguration().orientation == 1 /* portrait */) {

			editHolder.setOrientation(LinearLayout.VERTICAL);
			orientation = 1;

		} else { /* landscape */
			editHolder.setOrientation(LinearLayout.HORIZONTAL);
			orientation = 2;

		}

		if (mainIntent.hasExtra("cue_name") && mainIntent.hasExtra("path")) {

			path = mainIntent.getExtras().get("path").toString();
			cueName = mainIntent.getExtras().getString("cue_name");
			titleText.setText(cueName.toUpperCase());

			image.setImageDrawable(Drawable.createFromPath(path + THUMB_DIR
					+ cueName.replaceAll("[^-_.A-Za-z0-9]","") + THUMB_END));

		} else {
			path = getFilesDir().toString();
			titleText.setText("change_this");
			cueName = "change_this";
		}

		if (path.equals("null") && cueName.equals("null")) {

            //noinspection StatementWithEmptyBody
            if (!titleText.getText().toString().trim().equals("change_this")) {
				// TODO prompt user to set name of cue.
			}

		} else {

			if (!titleText.getText().toString().matches("")
					&& !titleText.getText().toString().equals("change_this")) {
				currentCategory = (dbHelper.getCategory(titleText.getText()
						.toString().toLowerCase(Locale.US)));
				if (currentCategory.equals("EMPTY")) {
					currentCategory = "ALL";
				}
				categoryName.setText(currentCategory);

			}

			String[] values = new String[] { "Change", "People", "Places",
			"Things" };

			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_item, values);
			adapter.setDropDownViewResource(R.layout.custom_spinner);

			category_chooser.setAdapter(adapter);

		}

		if (cueName.equals("change_this") && !newPhoto) {

			getNewPhoto();
		}

		// Cleanup temp directory.
		File tempDir = new File(getFilesDir() + "/tmpImages/");
		if (!tempDir.exists()) {
			tempDir.mkdirs();
		}
		File[] tmpFileArray = tempDir.listFiles();
		for (File temp : tmpFileArray) {
			temp.delete();
		}

		changeImage.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				getNewPhoto();

			}
		});

		changeName.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				getNameDialog();
			}
		});

		category_chooser
		.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent,
					View view, int position, long id) {

				if (!parent.getItemAtPosition(position).toString()
						.trim().equals("Change")) {
					int cue_id = dbHelper.getCueId(cueName);
					ContentValues cv = new ContentValues();
					cv.put("category",
							parent.getItemAtPosition(position)
							.toString());
					dbHelper.updateRow(cv, cue_id);
					categoryName.setText(parent.getItemAtPosition(
							position).toString());
					parent.setSelection(0);

					final Dialog dialog = utils
							.buildConfirmDialog(
									tContext,
									"Success",
									"Successfuly changed category to: "
											+ parent.getItemAtPosition(
													position)
													.toString(), 1,
													orientation);
					dialog.show();

					Button ok = (Button) dialog.findViewById(0);
					ok.setText("OK");
					ok.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							dialog.dismiss();

						}

					});
				}

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});

		finished.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();

			}

		});

		delete.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				final Dialog dial = utils.buildConfirmDialog(
						tContext,
						"Confirm Delete.",
						"Are you sure you would like to Delete "
								+ cueName.toUpperCase()
								+ " including all associated files?", 2,
								orientation);

				Button del_button = (Button) dial.findViewById(0);
				@SuppressWarnings("ResourceType") Button cancel_button = (Button) dial.findViewById(1);

				del_button.setText("Confirm Delete");
				cancel_button.setText("Cancel");
				dial.show();

				del_button.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						File[] file_list = dbHelper.getFilesList(cueName);

						for (File tempFile : file_list) {
							tempFile.delete();
						}
						dbHelper.deleteCue(cueName);

						dial.dismiss();
						finish();

					}

				});

				cancel_button.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						dial.dismiss();

					}

				});
			}
		});
	}

	private void getNameDialog() {

		dialog = new Dialog(tContext, R.style.CustomDialogTheme);
		dialog.setContentView(R.layout.new_name_dialog);
		dialog.setTitle("Enter New Cue Name");

		dialog.show();

		nameEntry = (EditText) dialog.findViewById(R.id.name_entry);
		Button save = (Button) dialog.findViewById(R.id.save_button);
		Button cancel = (Button) dialog.findViewById(R.id.cancel_button);

		save.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Editable newName = nameEntry.getText();
				if (changeCueName(newName)) {
					dialog.dismiss();
					titleText.setText(newName.toString().toUpperCase());
					cueName = newName.toString().toLowerCase();
				} else {
					newName.clear();
				}
			}

		});

		cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss(); // no changes are made.

			}

		});
	}

	private boolean changeCueName(Editable newName) {

		String oldName = cueName;

        String safeFileName = newName.toString().replaceAll("[^-_.A-Za-z0-9]", "");

        Log.d(TAG, safeFileName);

		File oldImage = new File(getFilesDir() + IMG_DIR + oldName.replaceAll("[^-_.A-Za-z0-9]", "") + IMG_TYPE);
		File oldThumb = new File(getFilesDir() + THUMB_DIR + oldName.replaceAll("[^-_.A-Za-z0-9]", "")
				+ THUMB_END);
		File newImage = new File(getFilesDir() + IMG_DIR + safeFileName + IMG_TYPE);
		File newThumb = new File(getFilesDir() + THUMB_DIR + safeFileName + THUMB_END);

		if (!newImage.exists() && !newThumb.exists()) {

			oldImage.renameTo(newImage);

			oldThumb.renameTo(newThumb);

			int id = dbHelper.getCueId(oldName);

			if (id == -1) {

				ContentValues cv = new ContentValues();
				cv.put("cue_name", newName.toString());
				cv.put("image_location", newImage.toString());
				cv.put("thumbnail_location", newThumb.toString());
				cv.put("audio_location", "EMPTY");
				cv.put("text_display", newName.toString().toUpperCase());
				cv.put("category", "EMPTY");
				return dbHelper.insertNewCue(cv);

			} else {

				ContentValues cv = new ContentValues();
				cv.put("cue_name", newName.toString());
				cv.put("image_location", newImage.toString());
				cv.put("thumbnail_location", newThumb.toString());
				cv.put("text_display", newName.toString().toUpperCase());

				if (dbHelper.updateRow(cv, id)) {
					final Dialog dialog = utils.buildConfirmDialog(tContext,
							"Success", "Successfuly changed Cue name to: "
									+ newName.toString().toUpperCase(), 1,
									orientation);
					dialog.show();

					Button ok = (Button) dialog.findViewById(0);
					ok.setText(" OK ");
					ok.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							dialog.dismiss();

						}

					});
					cueName = newName.toString();
					return true;
				} else {
					final Dialog dialog = utils.buildConfirmDialog(tContext,
							"Failure", "Name Change failed, please try again.",
							1, orientation);
					dialog.show();

					Button ok = (Button) dialog.findViewById(0);
					ok.setText("OK");
					ok.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							dialog.dismiss();

						}

					});
					return false;
				}
			}

		} else {

			final Dialog dialog = utils
					.buildConfirmDialog(
							tContext,
							"Failure",
							"Sorry the name "
									+ newName.toString()
									+ " is already taken, please try again. If you would like to change the cue with that name return to the main screen then press and hold the cue.",
									1, orientation);
			dialog.show();

			Button ok = (Button) dialog.findViewById(0);
			ok.setText("OK");
			ok.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					dialog.dismiss();

				}

			});
			return false;
		}
	}

	private void getNewPhoto() {

		if (!titleText.getText().toString().trim().matches("")) {

			cueName = titleText.getText().toString().trim().toLowerCase();

		} else {
			titleText.setText("change_this");
			cueName = titleText.getText().toString().toLowerCase().trim();
		}

		final Dialog dialog = utils
				.buildConfirmDialog(
						tContext,
						"Get new Image",
						"To add a new image you can either take a picture or Email yourself the picture and save it, then select From Downloads.",
						3, orientation);
		Button fromCamera = (Button) dialog.findViewById(0);
		fromCamera.setText("Camera");
		@SuppressWarnings("ResourceType") Button fromDownloads = (Button) dialog.findViewById(1);
		fromDownloads.setText("Downloads");
		@SuppressWarnings("ResourceType") Button cancel = (Button) dialog.findViewById(2);
		cancel.setText("Cancel");
		dialog.show();
		fromCamera.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent takePhoto = new Intent(tContext, CameraManager.class);
				takePhoto.putExtra("file_name", cueName.toLowerCase().trim().replaceAll("[^-_.A-Za-z0-9]", ""));
				startActivityForResult(takePhoto, 3);
				dialog.dismiss();

			}

		});

		fromDownloads.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();

				Intent getDownloads = new Intent(tContext, GetDownloads.class);
				getDownloads
				.putExtra("file_name", cueName.toLowerCase().trim().replaceAll("[^-_.A-Za-z0-9]", ""));
				startActivityForResult(getDownloads, 0);

			}

		});

		cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
				finish();

			}

		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case 3: {
			if (resultCode == RESULT_OK) {

				Intent cropIntent = utils.performCrop(
						Uri.fromFile(new File(path
								+ IMG_DIR
								+ titleText.getText().toString().trim()
								.toLowerCase().replaceAll("[^-_.A-Za-z0-9]", "") + IMG_TYPE)),
								CueEditor.this);
				startActivityForResult(cropIntent, 1);
			}
			newPhoto = true;
			break;
		}
		case 1: {
			if (resultCode == RESULT_OK) {
				Bundle extras = data.getExtras();
				Bitmap cropped = extras.getParcelable("data");
				utils.finalizeImage(cropped, CueEditor.this, titleText
						.getText().toString().trim().toLowerCase().replaceAll("[^-_.A-Za-z0-9]", ""));

				newPhoto = true;

				cueName = titleText.getText().toString();
				if (cueName.equals("change_this")) {
					getNameDialog();
				}
				image.setImageDrawable(Drawable.createFromPath(path + THUMB_DIR
						+ titleText.getText().toString().trim().toLowerCase().replaceAll("[^-_.A-Za-z0-9]", "")
						+ THUMB_END));
				cueName = titleText.getText().toString();

			} else {
				if (!dbHelper.db.isOpen()) {
					dbHelper = new DatabaseHelper(this);
				}
				File[] file_list = dbHelper.getFilesList(cueName);

				for (File tempFile : file_list) {
					tempFile.delete();
				}
				dbHelper.deleteCue(cueName);
				finish();
			}
			break;
		}
		case 0: {
			if (resultCode == RESULT_OK) {
				image.setImageDrawable(Drawable.createFromPath(path + THUMB_DIR
						+ titleText.getText().toString().trim().toLowerCase().replaceAll("[^-_.A-Za-z0-9]", "")
						+ THUMB_END));
				cueName = titleText.getText().toString();

				newPhoto = true;

				if (cueName.equals("change_this")) {
					getNameDialog();
				}

				break;
			} else {
				Dialog errorDialog = utils.buildConfirmDialog(tContext,
						"Error", "Sorry there are no files available", 1,
						orientation);
				Button ok = (Button) errorDialog.findViewById(0);
				ok.setText("OK");
				ok.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						// TODO Auto-generated method stub
						finish();
					}

				});
				errorDialog.show();
			}
		}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		if (newConfig.orientation == 1 /* Portrait */) {

			editHolder.setOrientation(LinearLayout.VERTICAL);
			orientation = 1;
		} else if (newConfig.orientation == 2 /* landscape */) {
			editHolder.setOrientation(LinearLayout.HORIZONTAL);
			orientation = 2;
		}
		super.onConfigurationChanged(newConfig);
	}

	public void onPause() {
		super.onPause();
		if (dbHelper.db.isOpen()) {
			dbHelper.close();
		}

	}

	public void onDestroy() {
		super.onDestroy();
		if (dbHelper.db.isOpen()) {
			dbHelper.close();
		}

	}
}
