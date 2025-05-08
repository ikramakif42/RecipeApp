package com.example.recipeapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "recipes.db";
    public static final int DATABASE_VERSION = 1;

    // Recipes table - id, title, calorieInfo, servingInfo, ingredients, instructions;
    public static final String TABLE_RECIPES = "recipes";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CALORIE_INFO = "calorieInfo";
    public static final String COLUMN_SERVING_INFO = "servingInfo";
    public static final String COLUMN_INGREDIENTS = "ingredients";
    public static final String COLUMN_INSTRUCTIONS = "instructions";

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
                + COLUMN_INSTRUCTIONS + "TEXT)";
        db.execSQL(CREATE_RECIPES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        onCreate(db);
    }

    public long insertRecipe(String id, String title, String calorieInfo, String servingInfo,
                             String ingredients, String instructions) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, id);
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CALORIE_INFO, calorieInfo);
        values.put(COLUMN_SERVING_INFO, servingInfo);
        values.put(COLUMN_INGREDIENTS, ingredients);
        values.put(COLUMN_INSTRUCTIONS, instructions);
        return db.insert(TABLE_RECIPES, null, values);
    }

    public Cursor getAllRecipes() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_RECIPES, null, null, null, null, null, null);
    }

}