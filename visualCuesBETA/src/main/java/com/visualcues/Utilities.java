package com.visualcues;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @author Colin Maccannell (cmaccannell@gmail.com) Utility helper class.
 *         Assists in text management.
 */
public class Utilities {

	Dialog dialog;
	Context context;
	final String IMG_DIR = "/vq_images/";
	final String THUMB_DIR = IMG_DIR + "thumbs/";
	final String IMG_TYPE = ".jpg";
	final String THUMB_ENDING = "_thumb.jpg";

	public Utilities(Context ctext) {
		this.context = ctext;
	}

	/**
	 * Gets actual screen dimensions.
	 * 
	 * @param context
	 * @return Returns a Point include width and height of screen.
	 */
	public Point getScreenSize(Context context) {
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);

		return size;
	}

	public Boolean saveToJPEG(Bitmap bitmap, String path, String cName,
			Context context, boolean roundCorners) {

		DatabaseHelper dbHelper = new DatabaseHelper(context);
		Boolean result;

		Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 300, 300, false);
		Bitmap bmpThumb;
		Bitmap rounded;
		if (roundCorners) {
			bmpThumb = getRoundedCornerBitmap(scaled, 55);
			rounded = getRoundedCornerBitmap(bitmap, 55);
		} else {
			bmpThumb = scaled;
			rounded = bitmap;
		}

		// check if directory exists, if not create it.
		File dir = new File(path + IMG_DIR);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		File thumbDir = new File(path + THUMB_DIR);
		if (!thumbDir.exists()) {
			thumbDir.mkdirs();
		}

		File largeImg = new File(path + IMG_DIR + cName + IMG_TYPE);
		largeImg.delete();
		File thumbnail = new File(path + THUMB_DIR + cName + THUMB_ENDING);
		thumbnail.delete();
		try {
			FileOutputStream fos = new FileOutputStream(largeImg);
			FileOutputStream fosThumb = new FileOutputStream(thumbnail);
			rounded.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			bmpThumb.compress(Bitmap.CompressFormat.JPEG, 80, fosThumb);
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
		int id = dbHelper.getCueId(cName);
		if (id == -1) {

			ContentValues cv = new ContentValues();
			cv.put("cue_name", cName);
			cv.put("image_location", largeImg.toString());
			cv.put("thumbnail_location", thumbnail.toString());
			cv.put("audio_location", "EMPTY");
			cv.put("text_display", cName.toUpperCase());
			cv.put("category", "EMPTY");
			result = dbHelper.insertNewCue(cv);
		} else {

			result = dbHelper.setImageFile(id, largeImg.toString(),
					thumbnail.toString());
		}
		dbHelper.close();
		return result;
	}

	/**
	 * @param mContext
	 *            Context
	 * @param title
	 *            Dialog Title
	 * @param message
	 *            Dialog Message
	 * @param count
	 *            How may buttons the dialog should hold
	 * @return Custom dialog
	 * 
	 *         Once the dialog has been returned the buttons can be accessed via
	 *         id from 0 to length of count - 1.
	 */
	public Dialog buildConfirmDialog(Context mContext, String title,
			String message, int count, int orientation) {

		dialog = new Dialog(mContext, R.style.CustomDialogTheme);
		dialog.setTitle(title);
		dialog.setContentView(R.layout.customdialog);
		if (orientation == 1) {
			LinearLayout ll = (LinearLayout)dialog.findViewById(R.id.button_layout);
			ll.setOrientation(LinearLayout.VERTICAL);
		}
		TextView tv = (TextView) dialog.findViewById(R.id.text);
		LinearLayout ll = (LinearLayout) dialog
				.findViewById(R.id.button_layout);
		for (int index = 0; index < count; index++) {
			Button newButton = new Button(mContext);
			int drawableID = mContext.getResources().getIdentifier(
					"custom_button", "drawable", mContext.getPackageName());
			newButton.setBackgroundResource(drawableID);
			newButton.setId(index);
			ll.addView(newButton);
		}

		tv.setText(message);
		dialog.setTitle(title);

		return dialog;

	}

	/**
	 * Scales image to desired size returning a square image in Bitmap form.
	 * 
	 * @param path
	 *            URI path of image file to be scaled.
	 * @param size
	 *            integer size width and height equal.
	 * @return Returns a thumbnail scaled to the provided size.
	 */
	public Bitmap makeThumb(String path, int size, Context context,
			String cueName, Boolean doText) {

		Utilities utils = new Utilities(context);
		Bitmap b;
		int max_size = utils.getScreenSize(context).x;
		int IMAGE_MAX_SIZE = (max_size) / 5;

		// Decode image size
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		o.inSampleSize = 3;

		BitmapFactory.decodeFile(path, o);

		int scale = 2;
		if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
			scale = (int) Math.pow(
					2,
					(int) Math.round(Math.log(IMAGE_MAX_SIZE
							/ (double) Math.max(o.outHeight, o.outWidth))
							/ Math.log(0.5)));
		}

		// Decode with inSampleSize
		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize = scale;

		b = BitmapFactory.decodeFile(path, o2);

		return Bitmap.createScaledBitmap(b, IMAGE_MAX_SIZE,
				IMAGE_MAX_SIZE, true);

	}

	/**
	 * @param bitmap
	 * @param pixels
	 * @return
	 */
	public Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {

		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);

		Drawable imageDrawable = new BitmapDrawable(context.getResources(),
				bitmap);
		Canvas canvas = new Canvas(output);

		RectF outerRect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
		float cornerRadius = bitmap.getWidth() / 10f;

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.RED);
		canvas.drawRoundRect(outerRect, cornerRadius, cornerRadius, paint);

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		imageDrawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getWidth());

		// Save the layer to apply the paint
		canvas.saveLayer(outerRect, paint, Canvas.ALL_SAVE_FLAG);
		imageDrawable.draw(canvas);
		canvas.restore();

		return output;
	}

	public Intent performCrop(Uri picUri, Context context) {
		Intent cropIntent;

		cropIntent = new Intent(context, com.android.camera.CropImage.class);

		cropIntent.setDataAndType(picUri, "image/*");
		cropIntent.putExtra("aspectX", 1);
		cropIntent.putExtra("aspectY", 1);
		cropIntent.putExtra("outputX", 256);
		cropIntent.putExtra("outputY", 256);
		cropIntent.putExtra("return-data", true);

		return cropIntent;
	}

	public void finalizeImage(Bitmap cropped, Context context, String finalName) {
		Bitmap scaledBitmap;
		int height = cropped.getHeight();
		int width = cropped.getWidth();
		Float scale = (float) getScreenSize(context).y / height;

		// scale bitmap after cropping
		scaledBitmap = Bitmap.createScaledBitmap(cropped,
				(int) (scale * width), (int) (scale * height), true);

		cropped.recycle();

		saveToJPEG(scaledBitmap, context.getFilesDir().toString(), finalName,
				context, true);

		System.gc();
	}
}
