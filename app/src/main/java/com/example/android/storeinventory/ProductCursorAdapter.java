package com.example.android.storeinventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.storeinventory.data.InventoryContract;
import com.example.android.storeinventory.data.InventoryContract.ProductsEntry;

/**
 * {@link ProductCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of product data as its data source. This adapter knows
 * how to create list items for each row of product data in the {@link Cursor}.
 */
public class ProductCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link ProductCursorAdapter}.
     *
     * @param context The context
     * @param cursor  The cursor from which to get the data.
     */
    public ProductCursorAdapter(Context context, Cursor cursor){
        super(context, cursor, 0 /* flags */);
    }
    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Return the empty list item view
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current product can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        // Find individual view that we want to modify in the list_item layout
        TextView productNameTextView = (TextView) view.findViewById(R.id.name);
        TextView productPriceTextView = (TextView) view.findViewById(R.id.price);
        final TextView productQuantityTextView = (TextView) view.findViewById(R.id.quantity);

        // Extract properties from cursor for the current product
        final String productName = cursor.getString(cursor.getColumnIndex(ProductsEntry.COLUMN_PRODUCT_NAME));
        double productPrice = cursor.getDouble(cursor.getColumnIndex(ProductsEntry.COLUMN_PRODUCT_PRICE));
        final int productQuantity = cursor.getInt(cursor.getColumnIndex(ProductsEntry.COLUMN_PRODUCT_QUANTITY));

        // Populate fields with the extracted properties
        productNameTextView.setText(productName);
        productPriceTextView.setText(Double.toString(productPrice));
        productQuantityTextView.setText(Integer.toString(productQuantity));

        Button sellButton = (Button) view.findViewById(R.id.catalog_sell_button);
        // Add the information for the product ID to the sell button
        sellButton.setTag(cursor.getLong(cursor.getColumnIndex(ProductsEntry._ID)));
        // Set on click listener so that we can lower the quantity on the click of the sell button
        sellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int quantity = productQuantity;
                // Lower the quantity only if there is any quantity left
                if(quantity > 0) {
                    // Update the field for quantity by one with the click of the sell button
                    quantity -= 1;
                    updateQuantityInDb(quantity, v, context);
                    productQuantityTextView.setText(Integer.toString(quantity));
                }
            }
        });
    }

    /**
     * Update the quantity in the database with the new quantity read from the quantity field
     */
    private void updateQuantityInDb(int quantity, View v, Context context) {
        // Id of the product takes from the cursor
        long id = (Long) v.getTag();
        // The uri of the product
        Uri productUri = ContentUris.withAppendedId(ProductsEntry.CONTENT_URI, id);
        // Uri uri, ContentValues contentValues, String selection, String[] selectionArgs
        // Create a ContentValues object where column names are the keys,
        // and product attributes are the values. In this case we will alter only the quantity attribute
        ContentValues values = new ContentValues();
        values.put(ProductsEntry.COLUMN_PRODUCT_QUANTITY, quantity);

        int rowsUpdated = context.getContentResolver().update(productUri, values, null, null);

        if(rowsUpdated < 1){
            throw new IllegalArgumentException("Update is not supported for " + productUri);
        }
        else {
            // Toast message to say that the product quantity has been updated successfully
            Toast.makeText(context, context.getString(R.string.catalog_update_product_successful), Toast.LENGTH_SHORT).show();
        }
    }
}
