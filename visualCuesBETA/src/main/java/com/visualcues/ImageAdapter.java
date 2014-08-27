package com.visualcues;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

public class ImageAdapter extends ArrayAdapter<File> {

	ArrayList<File> fList;
	ImageView imageView;
	Context mContext;
	TextView tv;
	GridView gView;
	private Boolean flag;
	Utilities utils;
	private LayoutInflater mInflater;
	ViewHolder holder;
	private String cueName;
	private ArrayList<String> cacheList;
	private LinearLayout chosenCues, spaceHolder;
	@SuppressWarnings("unused")
	private String TAG = "ImageAdapter ";

	public ImageAdapter(Context context, int textViewResourceId,
			ArrayList<File> photo_list, GridView photoView, Boolean textFlag,
			ArrayList<String> cacheList, LinearLayout choiceBox,
			LinearLayout spaceHolder) {
		super(context, textViewResourceId, photo_list);

		this.cacheList = cacheList;
		fList = photo_list;
		mContext = context;
		gView = photoView;
		flag = textFlag;
		this.chosenCues = choiceBox;
		this.spaceHolder = spaceHolder;
		utils = new Utilities(mContext);
		mInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

	}

	public ArrayList<String> getArray() {
		return holder.ar_list;
	}

	public void clearArray() {
		holder.ar_list.clear();
	}

	@Override
	public int getCount() {

		return fList.size();
	}

	@Override
	public File getItem(int position) {

		return fList.get(position);
	}

	@Override
	public long getItemId(int position) {

		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		int realPosition = position;

		cueName = new DatabaseHelper(mContext).getText(fList.get(realPosition)
				.toString());

		if (convertView == null) {
			holder = new ViewHolder();
			if (cacheList != null) {
				holder.ar_list = cacheList;
			}
			convertView = mInflater.inflate(R.layout.grid_item, null);

			holder.imageview = (ImageView) convertView
					.findViewById(R.id.thumbImage);
			
			holder.overlay = (ImageView) convertView
					.findViewById(R.id.thumbOverlay);
			
			holder.cueCaption = (TextView) convertView
					.findViewById(R.id.cue_caption);

			convertView.setTag(holder);

		} else {

			holder = (ViewHolder) convertView.getTag();
			if (cacheList != null) {
				holder.ar_list = cacheList;
			}

		}

		if (flag) {

			holder.imageview.setId(realPosition);
			holder.overlay.setId(realPosition);
			holder.position = realPosition;

			holder.cueCaption.setText((!cueName.equals("ERROR")?cueName:""));

			if (chosenCues != null) {
				holder.imageview.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {

						ImageView iv = (ImageView) v;

						if (holder.ar_list.size() < 3
								|| holder.ar_list.contains(null)) {

							if (holder.ar_list.contains(null)) {
								holder.ar_list.set(
										holder.ar_list.indexOf(null),
										new DatabaseHelper(mContext).getText(
												fList.get(iv.getId())
														.toString())
												.toLowerCase());

							} else {
								holder.ar_list.add(new DatabaseHelper(mContext)
										.getText(
												fList.get(iv.getId())
														.toString())
										.toLowerCase());

							}
							if (chosenCues != null) {
								ImageView selected_iv = new ImageView(mContext);
								selected_iv.setImageDrawable(Drawable
										.createFromPath(fList.get(iv.getId())
												.toString()));
								selected_iv.setId(holder.ar_list.size() - 1);
								selected_iv
										.setOnClickListener(new OnClickListener() {

											@Override
											public void onClick(View thisView) {

												chosenCues.removeView(thisView);

												holder.ar_list.set(
														thisView.getId(), null);

											}

										});
								selected_iv.setScaleType(ScaleType.FIT_CENTER);
								selected_iv
										.setLayoutParams(new TableLayout.LayoutParams(
												LayoutParams.WRAP_CONTENT,
												LayoutParams.WRAP_CONTENT, 1f));

								spaceHolder.setVisibility(LinearLayout.VISIBLE);
								chosenCues.addView(selected_iv);
								if (holder.ar_list.size() > 0) {

								
									spaceHolder
											.setLayoutParams(new LinearLayout.LayoutParams(
													LayoutParams.MATCH_PARENT,
													utils.getScreenSize(mContext).y / 5));

								} else {
								
									spaceHolder
											.setVisibility(LinearLayout.GONE);
								}

							}

						} else {

							final Dialog dialog = utils
									.buildConfirmDialog(
											mContext,
											"Alert",
											"Sorry you can only pick a maximum of three cues."
													+ " Either remove a choice by clicking on the picture at the bottom of the screen,"
													+ " or press Clear Choices to clear all.",
											1, 0);
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
				});

				holder.imageview
						.setOnLongClickListener(new OnLongClickListener() {

							public boolean onLongClick(View v) {
								ImageView iv = (ImageView) v;

								Intent editCue = new Intent(mContext,
										CueEditor.class);
								editCue.putExtra("path", mContext.getFilesDir());
								editCue.putExtra(
										"cue_name",
										new DatabaseHelper(mContext).getText(
												fList.get(iv.getId())
														.toString())
												.toLowerCase());

								mContext.startActivity(editCue);

								return false;
							}
						});
			}

			Resources r = mContext.getResources();
			Drawable[] layers = new Drawable[2];
			layers[0] = Drawable.createFromPath(fList.get(realPosition)
					.toString());
			try {
				layers[0].setBounds(20, 20, 20, 20);

				layers[1] = r.getDrawable(R.drawable.photo_border_bg);
				LayerDrawable layerDrawable = new LayerDrawable(layers);
				holder.imageview.setImageDrawable(layerDrawable);
			} catch (NullPointerException e) {
			}

		} else {

			holder.imageview.setImageURI(Uri.fromFile(fList.get(realPosition)));
		}

		return convertView;
	}

	class ViewHolder {
		ImageView imageview;
		ImageView overlay;
		TextView cueCaption;
		LinearLayout chosenContent;
		int position;
		int id;
		ArrayList<String> ar_list = new ArrayList<String>();
	}

}
