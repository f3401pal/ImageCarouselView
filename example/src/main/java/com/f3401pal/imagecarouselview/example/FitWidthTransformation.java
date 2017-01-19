package com.f3401pal.imagecarouselview.example;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

public class FitWidthTransformation extends BitmapTransformation {
    private static final String TAG = FitWidthTransformation.class.getSimpleName();

    private final int VERSION = 1;

    public FitWidthTransformation(Context context) {
        super(context);
    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        float ratio = (float) outWidth / toTransform.getWidth();
        Matrix matrix = new Matrix();
        matrix.postScale(ratio, ratio);
        return Bitmap.createBitmap(toTransform, 0, 0, toTransform.getWidth(),
                toTransform.getHeight(), matrix, false);
    }

    @Override
    public String getId() {
        return TAG + VERSION;
    }
}