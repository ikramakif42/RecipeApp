package com.example.recipeapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.List;
import java.util.ArrayList;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "recipes.db";
    public static final int DATABASE_VERSION = 3; // Change this to force upgrade

    // Recipes table
    public static final String TABLE_RECIPES = "recipes";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CALORIE_INFO = "calorieInfo";
    public static final String COLUMN_SERVING_INFO = "servingInfo";
    public static final String COLUMN_INGREDIENTS = "ingredients";
    public static final String COLUMN_INSTRUCTIONS = "instructions";

    // Favorites table
    public static final String TABLE_FAVORITES = "favorites";
    public static final String COLUMN_FAV_ID = "id";
    public static final String COLUMN_FAV_NAME = "name";
    public static final String COLUMN_FAV_DETAILS = "details";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_RECIPES_TABLE = "CREATE TABLE " + TABLE_RECIPES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_CALORIE_INFO + " TEXT,"
                + COLUMN_SERVING_INFO + " TEXT,"
                + COLUMN_INGREDIENTS + " TEXT,"
                + COLUMN_INSTRUCTIONS + " TEXT)";
        db.execSQL(CREATE_RECIPES_TABLE);

        String CREATE_FAVORITES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_FAVORITES + "("
                + COLUMN_FAV_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_FAV_NAME + " TEXT,"
                + COLUMN_FAV_DETAILS + " TEXT)";
        db.execSQL(CREATE_FAVORITES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        onCreate(db);
    }

    public void addToFavorites(String name, String details) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FAV_NAME, name);
        values.put(COLUMN_FAV_DETAILS, details);
        long result = db.insert(TABLE_FAVORITES, null, values);

        if (result == -1) {
            throw new RuntimeException("Failed to insert into database");
        }
        db.close();
    }

    public List<Recipe> getAllFavoriteRecipes() {
        List<Recipe> favorites = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_FAVORITES, null);

        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FAV_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FAV_NAME));
                String ingredients = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FAV_DETAILS));

                Recipe recipe = new Recipe(id, title, "", "", ingredients, "");
                favorites.add(recipe);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return favorites;
    }

    public boolean deleteFavorite(Recipe toDelete){
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_FAVORITES, COLUMN_FAV_NAME + "=?", new String[]{toDelete.getTitle()});
        db.close();
        return result > 0;
    }
}
