package com.example.android.storeinventory.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.storeinventory.data.InventoryContract.ProductsEntry;

import java.io.File;

/**
 * Create,open,upgrade or delete the products table in the database.
 */
public class ProductsDbHelper extends SQLiteOpenHelper {
    /** File name of the database */
    public static final String DATABASE_NAME = "inventory.db";
    /** Database version number. If you change the database schema you must increment this number.*/
    public static final int DATABASE_VERSION = 1;

    public ProductsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        /** SQL command to create the table */
        String SQL_CREATE_PRODUCTS_TABLE =
                "CREATE TABLE products (" +
                        ProductsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        ProductsEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, " +
                        ProductsEntry.COLUMN_PRODUCT_PRICE + " INTEGER NOT NULL, " +
                        ProductsEntry.COLUMN_PRODUCT_QUANTITY + " INTEGER NOT NULL DEFAULT 0, " +
                        ProductsEntry.COLUMN_PRODUCT_PICTURE + " BLOB NOT NULL);";

        db.execSQL(SQL_CREATE_PRODUCTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
