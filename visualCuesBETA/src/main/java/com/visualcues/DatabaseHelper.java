package com.visualcues;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

class DatabaseHelper extends SQLiteOpenHelper {
	static final String TAG = "DatabaseHelper";
	static final String DATABASE_NAME = "visualcues_temp2.db";
	static final int DATABASE_VERSION = 1;
	static final String CUE_TABLE = "cues";
	private File largeImage;

    SQLiteDatabase db;

	DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);

		db = getWritableDatabase();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
	
		db.execSQL("CREATE TABLE " + CUE_TABLE + " ("
				+ "id INTEGER PRIMARY KEY AUTOINCREMENT," + "cue_name TEXT,"
				+ "image_location TEXT," + "thumbnail_location TEXT,"
				+ "audio_location TEXT," + "text_display TEXT,"
				+ "category TEXT" + ");");
	}

	@Override
	public synchronized void close() {
		db.close();
		super.close();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	public Boolean insertNewCue(ContentValues values) {
		
		Long rowid = db.insert(CUE_TABLE, "", values);

        return rowid >= 0;
	}

	public int getCueId(String cueName) {

		db = getWritableDatabase();
		Cursor c = db.rawQuery("SELECT id FROM " + CUE_TABLE
				+ " WHERE cue_name =" + DatabaseUtils.sqlEscapeString(cueName), null);

		int id;

		if (c.getCount() > 0) {
			c.moveToFirst();
			id = c.getInt(0);
			c.close();
			return id;
		} else {
			c.close();
			return -1;
		}
	}

	public Cursor queryAll(String category) {

		Cursor c;
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(DatabaseHelper.CUE_TABLE);
		if (category.equals("ALL")) {
			c = qb.query(db, null, null, null, null, null, "cue_name");
		} else {
			String whereClause = "category = ?";
			String[] test = { category };
			c = qb.query(db, null, whereClause, test, null, null, "cue_name");
		}

		return c;

	}

	public Cursor getCueById(int mount_id) {

		SQLiteQueryBuilder mqb = new SQLiteQueryBuilder();
		mqb.setTables(DatabaseHelper.CUE_TABLE);

		String proj = String.format("id=%d", mount_id);
		return mqb.query(db, null, proj, null, null, null, null);
	}

	public Boolean setImageFile(int id, String largeImg, String thumb) {

        return (db.rawQuery(
                "UPDATE " + CUE_TABLE + " SET image_location='" + largeImg
                        + "'" + " WHERE id= " + id, null).getCount() == 1)
                && (db.rawQuery(
                "UPDATE " + CUE_TABLE + " SET thumbnail_location='"
                        + "'" + thumb + " WHERE id= " + id, null)
                .getCount() == 1);
	}

	public String getAudio(String cueName) {
		Cursor c = db.rawQuery("SELECT audio_location FROM " + CUE_TABLE
				+ " WHERE cue_name='" + cueName + "'", null);
		c.moveToFirst();
		String result = c.getString(0);
		c.close();
		return result;
	}

	public String getText(String thumbnail) {

		db = getWritableDatabase();
		String result;
		Cursor c = db.rawQuery("SELECT text_display FROM " + CUE_TABLE
				+ " WHERE thumbnail_location='" + thumbnail + "'", null);
		if (c.getCount() > 0) {
			c.moveToFirst();
			result = c.getString(0);
			close();
			return result;
		} else {
			result = "ERROR";
			close();
			return result;
		}
	}

	public String getCategory(String cue) {

        Log.d(TAG, DatabaseUtils.sqlEscapeString(cue));
		Cursor c = db.rawQuery("SELECT category FROM " + CUE_TABLE
				+ " WHERE cue_name=" + DatabaseUtils.sqlEscapeString(cue), null);
		c.moveToFirst();
		return c.getString(0);
	}

	public File getLargeImage(String match, int id_type) {

		switch (id_type) {
		case 1:

            Cursor cs = db.rawQuery("SELECT image_location FROM " + CUE_TABLE
                    + " WHERE cue_name=" + DatabaseUtils.sqlEscapeString(match), null);
			cs.moveToFirst();
			largeImage = new File(cs.getString(0));
			break;
		case 2:
			cs = db.rawQuery("SELECT image_location FROM " + CUE_TABLE
					+ " WHERE thumbnail_location=" + DatabaseUtils.sqlEscapeString(match), null);
			cs.moveToFirst();
			largeImage = new File(cs.getString(0));

			break;
		case 3:
			break;
		}
		return largeImage;

	}

	public boolean updateRow(ContentValues cv, int id) {

		String where = "id=" + id;

        return db.update(CUE_TABLE, cv, where, null) == 1;
	}

	public File[] getFilesList(String cueName) {
		Cursor c = db
				.rawQuery(
						"SELECT image_location, thumbnail_location, audio_location FROM "
								+ CUE_TABLE + " WHERE cue_name=" + DatabaseUtils.sqlEscapeString(cueName), null);
		c.moveToFirst();
		File[] list = { new File(c.getString(0)), new File(c.getString(1)),
				new File(c.getString(2)) };
		c.close();
		return list;

	}

	public boolean deleteCue(String cueName) {

		return db.delete(CUE_TABLE, "cue_name =" + DatabaseUtils.sqlEscapeString(cueName), null) > 0;
	}
}