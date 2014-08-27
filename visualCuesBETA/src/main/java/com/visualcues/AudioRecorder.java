package com.visualcues;

import java.io.File;
import java.io.IOException;

import android.content.ContentValues;
import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * 
 */
public class AudioRecorder extends MediaRecorder {

	private MediaRecorder recorder = new MediaRecorder();
	private String path = null;
	private String fName = null;
	private final String subDIR = "/vq_audio/";
	private DatabaseHelper dbHelper;
	private final String TAG = "AudioRecorder ";
	private String cueName;

	/**
	 * Creates a new audio recording at the given path (relative to root of SD
	 * card).
	 */
	public AudioRecorder(String path, String fName, Context context) {
		this.path = path + subDIR;
		this.fName = fName + ".3gp";
		this.cueName = fName;
		dbHelper = new DatabaseHelper(context);
	}

	/**
	 * Starts a new recording.
	 */
	public void start() {

		// make sure the directory we plan to store the recording in exists
		File directory = new File(path + subDIR).getParentFile();
		Log.e(TAG + "39", directory.toString());
		if (!directory.exists()) {
			directory.mkdirs();
		}
		File fileCheck = new File(path + fName);
		if (fileCheck.exists()) {
			fileCheck.delete();
		}
		
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		
		recorder.setAudioSamplingRate(8000);
		recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		 

		recorder.setOutputFile(fileCheck.toString());

		try {
			recorder.prepare();
		} catch (IllegalStateException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}

		recorder.start();
		
	}

	/**
	 * Stops a recording that has been previously started.
	 */
	public void stop() {
		recorder.stop();
		recorder.release();
		int id = dbHelper.getCueId(cueName);
		ContentValues cv = new ContentValues();
		cv.put("audio_location", path + fName);
		dbHelper.updateRow(cv, id);
	}
}
