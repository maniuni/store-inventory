package com.example.android.storeinventory.data;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.BaseColumns;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;

/**
 * Defines table and column names for the Inventory database.
 */
public final class InventoryContract {
    /**
     * The "Content authority" is a name for the entire content provider, similar to the
     * relationship between a domain name and its website.  A convenient string to use for the
     * content authority is the package name for the app, which is guaranteed to be unique on the
     * device.
     */
    public static final String CONTENT_AUTHORITY = "com.example.android.storeinventory";

    /**
     * Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
     * the content provider.
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Possible path (appended to base content URI for possible URI's).
     */
    public static final String PATH_PRODUCTS = "products";

    public static abstract class ProductsEntry implements BaseColumns {
        /** The content URI to access the product data in the provider */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);
        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of products.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;
        /**
         * The MIME type of the {@link #CONTENT_URI} for a single product.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;
        // Name of the table
        public static final String TABLE_NAME = "products";
        // Names of the columns
        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_PRODUCT_NAME = "name";
        public static final String COLUMN_PRODUCT_PRICE = "price";
        public static final String COLUMN_PRODUCT_QUANTITY = "quantity";
        public static final String COLUMN_PRODUCT_PICTURE = "picture";

        /**
         * Converts the bitmap from the user gallery into a ByteArray which can be saved into the database
         */
        public static byte[] getBitmapAsByteArray(Bitmap bitmap){
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
            return outputStream.toByteArray();
        }

        /**
         * Converts the byte array from the database into a Bitmap that can be shown on the screen
         */
        public static Bitmap getByteArrayAsBitmap(byte[] byteArray){
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        }

        /**
         * Helps create a smaller bitmap image from resource
         * @param resources - a resources object
         * @param resourceId - the id of the image in the drawable folder
         * @param requiredWidth - the width that we want for the final image
         * @param requiredHeight - the height that we want for the final image
         * @return the decoded Bitmap
         */
        public static Bitmap decodeSampledBitmapFromResource(Resources resources, int resourceId,
                                                             int requiredWidth, int requiredHeight){
            // First decode with inJustDecodeBounds = true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(resources, resourceId, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, requiredWidth, requiredHeight);

            // Decode bitmap with inJustDecodeBounds = false
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeResource(resources, resourceId, options);
        }

        /**
         * Helps create a smaller bitmap image from a uri
         * @param context - a context object
         * @param uri - the uri of the image
         * @param requiredWidth - the width that we want for the final image
         * @param requiredHeight - the height that we want for the final image
         * @return the decoded Bitmap
         */
        public static Bitmap decodeSampledBitmapFromUri(Context context, Uri uri, int requiredWidth, int requiredHeight)
        throws FileNotFoundException {
            // First decode with inJustDecodeBounds = true, only to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, requiredWidth, requiredHeight);

            // Decode bitmap with inJustDecodeBounds = false
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
        }

        /**
         * Calculate a sample size value that is a power of 2 based on a target width and height
         * @param options is used to pass options to the BitmapFactory
         * @param requiredWidth is the width that we want for the final image
         * @param requiredHeight is the height that we want for the final image
         * @return by how much to scale down the image
         */
        private static int calculateInSampleSize(BitmapFactory.Options options, int requiredWidth, int requiredHeight) {
            // Raw height and width of the image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > requiredHeight || width > requiredWidth) {

                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the required height and width
                while ((halfHeight / inSampleSize) >= requiredHeight
                        && (halfWidth / inSampleSize) >= requiredWidth){
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }
    }
}
