package com.example.android.storeinventory;

import android.content.ContentValues;
import android.graphics.BitmapFactory;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.storeinventory.data.InventoryContract;
import com.example.android.storeinventory.data.InventoryContract.ProductsEntry;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Add a new product or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity
implements LoaderManager.LoaderCallbacks<Cursor>{

    /** Identifier for the product data loader */
    private static final int EXISTING_PRODUCT_LOADER = 1;

    /** Request code for the product picture */
    private static final int RESULT_LOAD_PICTURE = 1;

    /** Content URI for the existing product (null if it's a new product) */
    private Uri mCurrentProductUri;

    /** EditText field to enter the product's name */
    private EditText mNameEditText;

    /** EditText field to enter the product's price */
    private EditText mPriceEditText;

    /** EditText field to enter the product's quantity */
    private EditText mQuantityEditText;

    /** ImageView to show the selected product's picture */
    private ImageView mImageView;

    /** Button field to add the product's picture */
    Button mAddPictureButton;

    /** EditTexts to read how much to order/sell for a product */
    private EditText mOrderEditText;

    private EditText mSellEditText;

    /** A counter to keep track of the amount for sale */
    private int mSellCounter = 1;

    /** A counter to keep track of the amount for ordering */
    private int mOrderCounter = 1;

    /** Boolean flag that keeps track of whether the product has been edited (true) or not (false) */
    private boolean mProductHasChanged = false;

    /** Holds the scaled bitmap of the picture of the product */
    Bitmap scaledPictureBitmap;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mProductHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener(){
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new product or editing an existing one.
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // Find the button that loads the picture from the user's gallery
        mAddPictureButton = (Button) findViewById(R.id.buttonLoadPicture);

        // If the intent DOES NOT contain a product content URI, then we know that we are
        // creating a new product.
        if(mCurrentProductUri == null){
            // This is a new product, so change the app bar to say "Add a Product"
            setTitle(R.string.editor_activity_title_new_product);
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            // This triggers the onPrepareOptionsMenu() to be called and we overwrite it down below
            invalidateOptionsMenu();
        }
        else {
            // Otherwise this is an existing product, so change app bar to say "Edit Product"
            setTitle(R.string.editor_activity_title_edit_product);

            // There should be an image already, so change the button to say "Change picture"
            mAddPictureButton.setText(R.string.edit_product_change_photo);

            // Initialize a loader to read the product data from the database
            // and display the current values in the editor
            getSupportLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mQuantityEditText = (EditText) findViewById(R.id.edit_product_quantity);
        mImageView = (ImageView) findViewById(R.id.picture);
        /** Buttons to increase or decrease the amount to sell or order */
        Button orderMoreButton = (Button) findViewById(R.id.order_more_btn);
        Button orderLessButton = (Button) findViewById(R.id.order_less_btn);
        Button sellMoreButton = (Button) findViewById(R.id.sell_more_btn);
        Button sellLessButton = (Button) findViewById(R.id.sell_less_btn);
        Button sellButton = (Button) findViewById(R.id.sell_btn);
        Button orderButton = (Button) findViewById(R.id.order_btn);
        mOrderEditText = (EditText) findViewById(R.id.edit_product_order);
        mSellEditText = (EditText) findViewById(R.id.edit_product_sell);

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        orderMoreButton.setOnTouchListener(mTouchListener);
        orderLessButton.setOnTouchListener(mTouchListener);
        sellMoreButton.setOnTouchListener(mTouchListener);
        sellLessButton.setOnTouchListener(mTouchListener);
        mOrderEditText.setOnTouchListener(mTouchListener);
        mSellEditText.setOnTouchListener(mTouchListener);

        // Add a clickListener on the EditTexts for order and sell as well
        // because the quantity can be edited directly from there also
        mOrderEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOrderCounter = Integer.valueOf(mOrderEditText.getText().toString());
            }
        });

        mSellEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSellCounter = Integer.valueOf(mSellEditText.getText().toString());
            }
        });


        // Set OnClickListener so that when the user clicks the button we can open
        // the gallery to choose new or different photo
        mAddPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProductHasChanged = true;
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_PICTURE);
            }
        });

        // Add onClickListeners on the Buttons that can indicate by how much
        // the sell/order amount is increased then update the EditTexts to show
        // the new quantity
        orderLessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOrderCounter > 1) {
                    mOrderCounter--;
                    mOrderEditText.setText(String.valueOf(mOrderCounter));
                }
            }
        });

        orderMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOrderCounter++;
                mOrderEditText.setText(String.valueOf(mOrderCounter));
            }
        });

        sellLessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSellCounter > 1) {
                    mSellCounter--;
                    mSellEditText.setText(String.valueOf(mSellCounter));
                }
            }
        });

        sellMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSellCounter++;
                mSellEditText.setText(String.valueOf(mSellCounter));
            }
        });

        // Add onClickListener on the sell button so that
        // it changes the text in the quantity field by as much as it shows in the mSellEditText
        sellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Take the amount from the mSellEditText field
                int sellAmount = Integer.parseInt(mSellEditText.getText().toString());
                // Take the current quantity amount
                int currentAmount = Integer.parseInt(mQuantityEditText.getText().toString());
                // Calculate the new quantity and change the quantity field so that
                // when the user clicks "Save" the updated amount is saved
                int newQuantity = currentAmount - sellAmount;
                // A bigger sell amount than the current quantity means a mistake
                if(newQuantity < 0) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.editor_quantity_warning), Toast.LENGTH_SHORT).show();
                }
                else {
                    mQuantityEditText.setText(String.valueOf(newQuantity));
                }
            }
        });

        // Add onClickListener on the order button so that
        // it opens an intent to an email app to order more product
        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentProductName = mNameEditText.getText().toString().trim();
                int orderAmount = Integer.parseInt(mOrderEditText.getText().toString());
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("*/*");
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, currentProductName);
                emailIntent.putExtra(Intent.EXTRA_TEXT,
                        getString(R.string.editor_order_more_email, orderAmount, currentProductName));
                if(emailIntent.resolveActivity(getPackageManager()) != null){
                    startActivity(emailIntent);
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == RESULT_LOAD_PICTURE && resultCode == RESULT_OK && null != intent) {
            Uri selectedPictureUri = intent.getData();
            // Show the selected picture in an ImageView in the editor
            try {
                // Scale the bitmap received from the uri so it fits in the small ImageView
                scaledPictureBitmap = ProductsEntry.decodeSampledBitmapFromUri(this, selectedPictureUri, 72, 72);
                // Hide the gray picture placeholder
                mImageView.setBackgroundResource(0);
                // Show the scaled bitmap in the ImageView
                mImageView.setImageBitmap(scaledPictureBitmap);
                // The user has chosen a picture and we can change
                // the text of the button to say "Change picture"
                mAddPictureButton.setText(R.string.edit_product_change_photo);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get user input from editor and save product into database.
     */
    private void saveProduct(){
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();

        // Check if this is supposed to be a new product
        // and check if all the fields in the editor are blank
        if(mCurrentProductUri == null
                && TextUtils.isEmpty(nameString) && TextUtils.isEmpty(priceString)
                && TextUtils.isEmpty(quantityString) && scaledPictureBitmap == null){
            // Since no fields were modified, we can return early without creating a new product.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return;
        }

        // If this is a new product and
        // not all of the fields are filled don't save anything
        // but remind the user
        if(mCurrentProductUri == null && (TextUtils.isEmpty(nameString) || TextUtils.isEmpty(priceString) || TextUtils.isEmpty(quantityString) || scaledPictureBitmap == null)) {
            Toast.makeText(this, getString(R.string.editor_insert_info_missing), Toast.LENGTH_SHORT).show();
            // Do not continue instead only warn the user to fill everything first
            return;
        }
        // Create a ContentValues object where column names are the keys,
        // and product attributes from the editor are the values.
        ContentValues values = new ContentValues();
        // First save the name
        values.put(ProductsEntry.COLUMN_PRODUCT_NAME, nameString);
        // Convert the price from a String into double
        double price = Double.parseDouble(priceString);
        values.put(ProductsEntry.COLUMN_PRODUCT_PRICE, price);
        // Convert the quantity from a String into integer
        int quantity = Integer.parseInt(quantityString);
        values.put(ProductsEntry.COLUMN_PRODUCT_QUANTITY, quantity);
        // If the bitmap is null that means the picture hasn't been changed so we can use the old one
        // if it's not null we can use it to convert it to byte array and save it
        if(scaledPictureBitmap != null) {
            // Convert the bitmap into ByteArray so it can be saved into the database
            byte[] picture = ProductsEntry.getBitmapAsByteArray(scaledPictureBitmap);
            values.put(ProductsEntry.COLUMN_PRODUCT_PICTURE, picture);
        }

        // Determine if this is a new or existing pet by checking if mCurrentPetUri is null or not
        if(mCurrentProductUri == null) {
            // This is a NEW product, so insert a new product into the provider,
            // returning the content URI for the new product.
            Uri newProductUri = getContentResolver().insert(ProductsEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if(newProductUri == null){
                Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                        Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        else {
            // Otherwise this is an EXISTING product, so update the product with content URI: mCurrentProductUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentProductUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if(rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_product_failed), Toast.LENGTH_SHORT).show();
            }
            else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_product_successful), Toast.LENGTH_SHORT).show();
            }
        }
        // Close the activity
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if(mCurrentProductUri == null){
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_save:
                // Save product in the database
                saveProduct();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if(!mProductHasChanged){
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };
                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {
        // Only perform the delete if this is an existing product.
        if(mCurrentProductUri != null) {
            // Call the ContentResolver to delete the product at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentProductUri
            // content URI already identifies the product that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_product_failed), Toast.LENGTH_SHORT).show();
            }
            else {
                // Otherwise the delete was successful.
                Toast.makeText(this, getString(R.string.editor_delete_product_successful), Toast.LENGTH_SHORT).show();
            }
        }
        // Close the activity
        finish();
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if(!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i){
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.unsaved_changes_dialog_msg));
        builder.setPositiveButton(getString(R.string.discard), discardButtonClickListener);
        builder.setNegativeButton(getString(R.string.keep_editing), new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int i) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the product.
                if(dialog != null){
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Since the editor shows all pet attributes, define a projection that contains
        // all columns from the pet table
        String[] projection = {
                ProductsEntry._ID,
                ProductsEntry.COLUMN_PRODUCT_NAME,
                ProductsEntry.COLUMN_PRODUCT_PRICE,
                ProductsEntry.COLUMN_PRODUCT_QUANTITY,
                ProductsEntry.COLUMN_PRODUCT_PICTURE};


        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentProductUri,     // Query the content URI for the current product
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if(cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if(cursor.moveToFirst()){
            // Find the columns of product attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(ProductsEntry.COLUMN_PRODUCT_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ProductsEntry.COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductsEntry.COLUMN_PRODUCT_QUANTITY);
            int pictureColumnIndex = cursor.getColumnIndex(ProductsEntry.COLUMN_PRODUCT_PICTURE);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            double price = cursor.getDouble(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            byte[] pictureBytes = cursor.getBlob(pictureColumnIndex);
            // Convert the byte array into a bitmap to show in the editor
            Bitmap picture = ProductsEntry.getByteArrayAsBitmap(pictureBytes);

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mPriceEditText.setText(Double.toString(price));
            mQuantityEditText.setText(Integer.toString(quantity));
            // Hide the gray picture placeholder before showing the picture
            mImageView.setBackgroundResource(0);
            mImageView.setImageBitmap(picture);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
        mImageView.setImageResource(R.drawable.ic_photo);
    }
}
