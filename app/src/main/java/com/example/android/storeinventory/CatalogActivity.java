package com.example.android.storeinventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.storeinventory.data.InventoryContract.ProductsEntry;

public class CatalogActivity extends AppCompatActivity
implements LoaderManager.LoaderCallbacks<Cursor>{

    /** Identifier for the product data loader */
    private final int LOADER_ID = 0;

    // This is the Adapter being used to display the list's data.
    ProductCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find the ListView which will be populated with the product data
        ListView listView = (ListView) findViewById(R.id.list_view_products);

        // Setup an adapter to create a list item for each row of product data in the Cursor.
        // There is no product data yet, so pass in null for the Cursor.
        mCursorAdapter = new ProductCursorAdapter(this, null);
        listView.setAdapter(mCursorAdapter);

        // Set a click listener on each item in the list so that it opens the Editor Activity with the
        // information for this item which is sent with the Uri of the item
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                intent.setData(ContentUris.withAppendedId(ProductsEntry.CONTENT_URI, id));
                startActivity(intent);
            }
        });

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()){
            // Respond to a click on the "Insert dummy product" menu option
            case R.id.action_insert_dummy_product:
                insertProduct();
                return true;
            // Respond to a click on the "Delete all products" menu option
            case R.id.action_delete_all:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.delete_dialog_msg_all_products));
        builder.setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // User clicked the "Delete" button, so delete the products.
                deleteAllProducts();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // User clicked the "Cancel" button, so dismiss the dialog
                if(dialogInterface != null){
                    dialogInterface.dismiss();
                }
            }
        });
        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteAllProducts() {
        // Call the ContentResolver to delete the products at the given content URI.
        // Pass in null for the selection and selection args
        int rowsDeleted = getContentResolver().delete(ProductsEntry.CONTENT_URI, null, null);

        // Show a toast message depending on whether or not the delete was successful.
        if(rowsDeleted == 0){
            // If no rows were deleted, then there was an error with the delete.
            Toast.makeText(this, getString(R.string.catalog_delete_products_failed), Toast.LENGTH_SHORT).show();
        }
        else {
            // Otherwise, the delete was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.catalog_delete_products_successful), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Helper method to insert hardcoded product data into the database. For debugging purposes only.
     */
    private void insertProduct() {
        // Create a ContentValues object where column names are the keys,
        // and bread product attributes are the values.
        ContentValues values = new ContentValues();
        values.put(ProductsEntry.COLUMN_PRODUCT_NAME, "Country Sourdough");
        values.put(ProductsEntry.COLUMN_PRODUCT_PRICE, 3.5);
        values.put(ProductsEntry.COLUMN_PRODUCT_QUANTITY, 7);

        // Make the sample image smaller so it doesn't take too much space in the memory
        Bitmap sourdoughBitmap = ProductsEntry.decodeSampledBitmapFromResource(getResources(), R.drawable.sourdough_picture, 72, 72);

        // Convert into byte array to be saved in the db
        byte[] sourdoughBytes = ProductsEntry.getBitmapAsByteArray(sourdoughBitmap);

        // Save in the values object along with the other values
        values.put(ProductsEntry.COLUMN_PRODUCT_PICTURE, sourdoughBytes);

        // Insert a new row for the Country Sourdough into the provider using the ContentResolver.
        // Use the {@link ProductsEntry#CONTENT_URI} to indicate that we want to insert
        // into the products database table.
        // Receive the new content URI that will allow us to access the Country Sourdough data in the future.

        Uri newUri = getContentResolver().insert(ProductsEntry.CONTENT_URI, values);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                ProductsEntry._ID,
                ProductsEntry.COLUMN_PRODUCT_NAME,
                ProductsEntry.COLUMN_PRODUCT_PRICE,
                ProductsEntry.COLUMN_PRODUCT_QUANTITY
        };
        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this, ProductsEntry.CONTENT_URI, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update {@link CursorAdapter} with this new cursor containing updated product data
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }
}
