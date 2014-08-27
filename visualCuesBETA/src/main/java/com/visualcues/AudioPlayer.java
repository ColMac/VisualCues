package com.visualcues;

import java.io.IOException;

import android.content.Context;
import android.media.MediaPlayer;

/**
 * @author cmac
 * 
 */
public class AudioPlayer extends MediaPlayer {

	MediaPlayer player = new MediaPlayer();
	private DatabaseHelper dbHelper;
	private String filePath;

	public AudioPlayer(String cueName, Context context) {
		
		

		this.dbHelper = new DatabaseHelper(context);
		this.filePath = this.dbHelper.getAudio(cueName);
	}

	public void play() {

			try {

				player.setDataSource(filePath);

				
				player.prepare();

			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			player.setOnPreparedListener(new OnPreparedListener() {

				@Override
				public void onPrepared(MediaPlayer mp) {

					player.start();
				}

			});

		player.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer arg0) {
				player.stop();
				player.release();
			//	player.reset();
			}
		});
	}
}
