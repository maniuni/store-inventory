package com.example.android.storeinventory.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.storeinventory.data.InventoryContract.ProductsEntry;

/**
 * ContentProvider for the Store Inventory app.
 */
public class ProductsProvider extends ContentProvider {

    /** Tag for the log messages */
    public static final String LOG_TAG = ProductsProvider.class.getSimpleName();

    /** URI matcher code for the content URI for the products table */
    public static final int PRODUCTS = 100;

    /** URI matcher code for the content URI for a single product in the products table */
    public static final int PRODUCT_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    public static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form "content://com.example.android.storeinventory/products" will map to the
        // integer code {@link #PRODUCTS}. This URI is used to provide access to MULTIPLE rows
        // of the products table.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCTS, PRODUCTS);
        // The content URI of the form "content://com.example.android.storeinventory/products/#" will map to the
        // integer code {@link #PRODUCT_ID}. This URI is used to provide access to ONE single row
        // of the products table.
        //
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        // For example, "content://com.example.android.storeinventory/products/3" matches, but
        // "content://com.example.android.storeinventory/products" (without a number at the end) doesn't match.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCTS + "/#", PRODUCT_ID);
    }

    /** Database helper object */
    private ProductsDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new ProductsDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match){
            case PRODUCTS:
                // For the PRODUCTS code, query the products table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the products table.
                cursor = database.query(ProductsEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case PRODUCT_ID:
                // For the PRODUCT_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.storeinventory/products/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = ProductsEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};

                // This will perform a query on the products table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(ProductsEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match){
            case PRODUCTS:
                return insertProduct(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a product into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertProduct(Uri uri, ContentValues contentValues) {
        // Check that the name is not null
        String name = contentValues.getAsString(ProductsEntry.COLUMN_PRODUCT_NAME);
        if(name == null){
            throw new IllegalArgumentException("Product requires a name");
        }
        // Check that the price is not null
        Integer price = contentValues.getAsInteger(ProductsEntry.COLUMN_PRODUCT_PRICE);
        if(price == null || price < 0){
            throw new IllegalArgumentException("Product requires a valid price");
        }
        // Check that the quantity is not null
        Integer quantity = contentValues.getAsInteger(ProductsEntry.COLUMN_PRODUCT_QUANTITY);
        if(quantity == null || quantity < 0){
            throw new IllegalArgumentException("Product requires a valid quantity");
        }
        // Check that the image is not null
        byte[] image = contentValues.getAsByteArray(ProductsEntry.COLUMN_PRODUCT_PICTURE);
        if(image == null) {
            throw new IllegalArgumentException("Product requires an image");
        }
        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new product with the given values
        long id = database.insert(ProductsEntry.TABLE_NAME, null, contentValues);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if(id == -1){
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return  null;
        }
        // Notify all listeners that the data has changed for the product content Uri
        getContext().getContentResolver().notifyChange(uri, null);
        // Return the URI of the new product with the appended ID at the end
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        // Track the number of rows that were deleted
        int rowsAffected = 0;

        int match = sUriMatcher.match(uri);
        switch (match){
            case PRODUCTS:
                // Delete all rows that match the selection and selection args
                rowsAffected = database.delete(ProductsEntry.TABLE_NAME, selection, selectionArgs);
                // If 1 or more rows were deleted, then notify all listeners that the data at the
                // given URI has changed
                if(rowsAffected > 0){
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                // Return the number of rows deleted
                return rowsAffected;
            case PRODUCT_ID:
                selection = ProductsEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                rowsAffected = database.delete(ProductsEntry.TABLE_NAME, selection, selectionArgs);
                // If 1 or more rows were deleted, then notify all listeners that the data at the
                // given URI has changed
                if(rowsAffected > 0){
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                // Return the number of rows deleted
                return rowsAffected;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);

        }
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match){
            case PRODUCTS:
                return updateProduct(uri, contentValues, selection, selectionArgs);
            case PRODUCT_ID:
                // For the PRODUCT_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = ProductsEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                return updateProduct(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update products in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more products).
     * Return the number of rows that were successfully updated.
     */
    private int updateProduct(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        // If the {@link ProductsEntry#COLUMN_PRODUCT_NAME} key is present,
        // check that the name value is not null.
        if(contentValues.containsKey(ProductsEntry.COLUMN_PRODUCT_NAME)){
            String name = contentValues.getAsString(ProductsEntry.COLUMN_PRODUCT_NAME);
            if(name == null){
                throw new IllegalArgumentException("Product requires a name");
            }
        }

        // If the {@link ProductsEntry#COLUMN_PRODUCT_PRICE} key is present,
        // check that the price value is not null and is valid.
        if(contentValues.containsKey(ProductsEntry.COLUMN_PRODUCT_PRICE)){
            Integer price = contentValues.getAsInteger(ProductsEntry.COLUMN_PRODUCT_PRICE);
            if(price == null || price < 0){
                throw new IllegalArgumentException("Product requires a valid price");
            }
        }

        // If the {@link ProductsEntry#COLUMN_PRODUCT_QUANTITY} key is present,
        // check that the quantity value is not null and is valid.
        if(contentValues.containsKey(ProductsEntry.COLUMN_PRODUCT_QUANTITY)){
            Integer quantity = contentValues.getAsInteger(ProductsEntry.COLUMN_PRODUCT_QUANTITY);
            if(quantity == null || quantity < 0){
                throw new IllegalArgumentException("Product requires a valid quantity");
            }
        }

        // If the {@link ProductsEntry#COLUMN_PRODUCT_PICTURE} key is present,
        // check that the picture value is not null.
        if(contentValues.containsKey(ProductsEntry.COLUMN_PRODUCT_PICTURE)){
            byte[] picture = contentValues.getAsByteArray(ProductsEntry.COLUMN_PRODUCT_PICTURE);
            if(picture == null){
                throw new IllegalArgumentException("Product requires an image");
            }
        }
        // If there are no values to update, then don't try to update the database
        if(contentValues.size() == 0) {
            return 0;
        }

        // Otherwise, get writable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(ProductsEntry.TABLE_NAME, contentValues, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if(rowsUpdated > 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
        return rowsUpdated;
    }
}
