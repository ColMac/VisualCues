package com.visualcues;

import java.io.File;
import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;

public class CameraManager extends Activity implements SurfaceHolder.Callback {

	private static Camera camera = null;
	private boolean cameraConfigured = false;
	private static boolean inPreview = false;
	private static String file_path;
	private static String fName;
	private final String subDir = "/vq_images/";
	private static Bitmap rotated = null;
	private SurfaceView surfaceView = null;
	private SurfaceHolder surfaceHolder = null;
	private LayoutInflater inflater;
	private Button takePicture;
	private Intent mainIntent;
	private CameraInfo cameraInfo;
	private Context cContext;
	private View overView = null;
	private Utilities utils;
	@SuppressWarnings("unused")
	private String TAG = "CameraManager ln: ";
	private int result, degrees;
	private File dir = null;
	SavePhotoTask sTask = null;
	LinearLayout rightCrop, leftCrop, topCrop, bottomCrop;
	private int size;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.camera);

		surfaceView = (SurfaceView) findViewById(R.id.surface);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);

		inflater = LayoutInflater.from(this);
		overView = inflater.inflate(R.layout.cameraoverlay, null);

		this.addContentView(overView, new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		cContext = this;
		utils = new Utilities(cContext);

		rightCrop = (LinearLayout) overView.findViewById(R.id.right_crop);
		leftCrop = (LinearLayout) overView.findViewById(R.id.left_crop);
		topCrop = (LinearLayout) overView.findViewById(R.id.top_crop);
		bottomCrop = (LinearLayout) overView.findViewById(R.id.bottom_crop);

		if (getResources().getConfiguration().orientation == 1 /* portrait */) {

			rightCrop.setVisibility(LinearLayout.GONE);
			leftCrop.setVisibility(LinearLayout.GONE);
			topCrop.setVisibility(LinearLayout.VISIBLE);
			bottomCrop.setVisibility(LinearLayout.VISIBLE);

			size = (utils.getScreenSize(cContext).y - utils
					.getScreenSize(cContext).x) / 2;

		} else { /* landscape */

			rightCrop.setVisibility(LinearLayout.VISIBLE);
			leftCrop.setVisibility(LinearLayout.VISIBLE);
			topCrop.setVisibility(LinearLayout.GONE);
			bottomCrop.setVisibility(LinearLayout.GONE);

			size = (utils.getScreenSize(cContext).x - utils
					.getScreenSize(cContext).y) / 2;

		}

		LayoutParams topParams = topCrop.getLayoutParams();
		topParams.height = size;

		LayoutParams bottomParams = bottomCrop.getLayoutParams();
		bottomParams.height = size;

		LayoutParams leftParams = leftCrop.getLayoutParams();
		leftParams.width = size;

		LayoutParams rightParams = rightCrop.getLayoutParams();
		rightParams.width = size;

		takePicture = (Button) findViewById(R.id.snap_button);

		mainIntent = getIntent();
		setFileName(mainIntent.getStringExtra("file_name"));

		takePicture.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				camera.takePicture(null, null, photoCallback);

			}
		});
	}

	/**
	 * @param width
	 * @param height
	 * @param parameters
	 * @return
	 */
	private Camera.Size getBestPreviewSize(int width, int height,
			Camera.Parameters parameters) {
		Camera.Size result = null;

		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;

					if (newArea > resultArea) {
						result = size;
					}
				}
			}
		}

		return (result);
	}

	/**
	 * 
	 */
	private void startPreview() {
		if (cameraConfigured && camera != null) {
			camera.startPreview();
			inPreview = true;

		}
	}

	/**
	 * @param width
	 * @param height
	 */
	private void initPreview(int width, int height) {
		if (camera != null && surfaceHolder.getSurface() != null) {
			try {
				camera.setPreviewDisplay(surfaceHolder);
			} catch (Throwable t) {
				t.printStackTrace();
			}

			WindowManager winManager = (WindowManager) cContext
					.getSystemService(Context.WINDOW_SERVICE);
			int rotation = winManager.getDefaultDisplay().getRotation();

			degrees = 0;

			switch (rotation) {

			case Surface.ROTATION_0:
				degrees = 0;
				break;
			case Surface.ROTATION_90:
				degrees = 90;
				break;
			case Surface.ROTATION_180:
				degrees = 180;
				break;
			case Surface.ROTATION_270:
				degrees = 270;
				break;
			}

			camera.setDisplayOrientation(degrees);

			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				result = (cameraInfo.orientation + degrees) % 360;
				result = (360 - result) % 360; // compensate the mirror
			} else { // back-facing
				result = (cameraInfo.orientation - degrees + 360) % 360;
			}
			camera.setDisplayOrientation(result);
		

			if (!cameraConfigured) {
				Camera.Parameters parameters = camera.getParameters();
				Camera.Size size = getBestPreviewSize(width, height, parameters);
				Camera.Size pSize = getMediumPictureSize(parameters);

				if (size != null) {
					parameters.setPreviewSize(size.width, size.height);
					parameters.setPictureSize(pSize.width, pSize.height);

					camera.setParameters(parameters);
					cameraConfigured = true;
				}
			}
		}
	}

	/**
	 * @param parameters
	 * @return
	 */
	public Camera.Size getMediumPictureSize(Camera.Parameters parameters) {
		Camera.Size result = null;

		List<Camera.Size> params = parameters.getSupportedPictureSizes();
		result = params.get(params.size() / 2);

		return (result);
	}

	/**
	 * @param file_name
	 */
	public void setFileName(String file_name) {
		CameraManager.fName = file_name;
	}

	/**
	 * 
	 */
	PictureCallback photoCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {

			if (!isFinishing()) {
				sTask = new SavePhotoTask();
				sTask.execute(data).toString();
				camera.startPreview();
				inPreview = true;
			}
		}
	};

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

		try {
			if (inPreview) {
				camera.stopPreview();
				inPreview = false;
			}
			initPreview(width, height);
			startPreview();
			inPreview = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

		try {
			cameraInfo = new Camera.CameraInfo();
			for (int i = 0; i < Camera.getNumberOfCameras() && camera == null; i++) {
				Camera.getCameraInfo(i, cameraInfo);
	//			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
					camera = Camera.open(i);
	//			} else {
	//				camera = Camera.open(i);
	//			}
			}
			if (camera == null) {
				camera = Camera.open();
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

		if (camera != null) {
			camera.stopPreview();
			inPreview = false;
			camera.release();
			camera = null;
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		switch (newConfig.orientation) {
		case 1: /* portrait */
			rightCrop.setVisibility(LinearLayout.GONE);
			leftCrop.setVisibility(LinearLayout.GONE);
			topCrop.setVisibility(LinearLayout.VISIBLE);
			bottomCrop.setVisibility(LinearLayout.VISIBLE);
			break;
		case 2: /* landscape */
			rightCrop.setVisibility(LinearLayout.VISIBLE);
			leftCrop.setVisibility(LinearLayout.VISIBLE);
			topCrop.setVisibility(LinearLayout.GONE);
			bottomCrop.setVisibility(LinearLayout.GONE);
			break;
		}
	}

	/**
	 * @author cmac
	 * 
	 */
	class SavePhotoTask extends AsyncTask<byte[], String, String> {

		@Override
		protected String doInBackground(byte[]... jpeg) {

			utils = new Utilities(cContext);

			file_path = getFilesDir() + subDir;
			dir = new File(file_path);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			File photo = new File(dir, fName + ".jpg");
			if (photo.exists()) {
				photo.delete();
			}

			Bitmap rawBMP = BitmapFactory.decodeByteArray(jpeg[0], 0,
					jpeg[0].length);

			Matrix matrix = new Matrix();

			matrix.postRotate(360 - result);
			try {

				rotated = Bitmap.createBitmap(rawBMP, 0, 0, rawBMP.getWidth(),
						rawBMP.getHeight(), matrix, true);

			} catch (Exception e1) {

				e1.printStackTrace();
			}

			return "true";
		}

		protected void onPostExecute(String result) {

			utils.saveToJPEG(rotated, getFilesDir().toString(), fName,
					cContext, false);
			if (camera != null) {
				camera.setPreviewCallback(null);
				camera.stopPreview();
				camera.release();
				camera = null;
				inPreview = false;
			}
			setResult(RESULT_OK);

			if (!isFinishing()) {
				finish();
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (camera != null) {
			camera.stopPreview();
			inPreview = false;
			camera.release();
			camera = null;
		}

	}
}
