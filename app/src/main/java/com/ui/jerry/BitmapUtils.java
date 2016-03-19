package com.ui.jerry;

import android.graphics.Bitmap;
import android.graphics.Matrix;


public class BitmapUtils {
    public static final String TAG = "BitmapUtils";


    public static Bitmap scale(Bitmap bitmap, float scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap outBitmap = Bitmap.createBitmap(bitmap, 0, 0, (int) (bitmap.getWidth()), (int) (bitmap.getHeight()), matrix, true);
        bitmap.recycle();
        return outBitmap;
    }

    public static Bitmap resizeBmp(Bitmap bitmap, int requiredSizeW, int requireSizeH) {
        Bitmap outBitamp = Bitmap.createBitmap(bitmap, 0, 0, requiredSizeW, requireSizeH);
        bitmap.recycle();
        return outBitamp;
    }


}
