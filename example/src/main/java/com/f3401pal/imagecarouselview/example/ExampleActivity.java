package com.f3401pal.imagecarouselview.example;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.f3401pal.imagecarouselview.ImageCarouselView;
import com.f3401pal.imagecarouselview.ImageLoader;
import com.f3401pal.imagecarouselview.OnResourceReadyListener;

public class ExampleActivity extends AppCompatActivity {

    private static final String[] URLs = {
            "https://images.pexels.com/photos/29859/pexels-photo-29859.jpg",
            "https://images.pexels.com/photos/94562/pexels-photo-94562.jpeg",
            "https://images.pexels.com/photos/287229/pexels-photo-287229.jpeg"
    };

    private ImageCarouselView carouselView;
    private FitWidthTransformation transformation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);

        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        transformation = new FitWidthTransformation(this);
        carouselView = (ImageCarouselView) findViewById(R.id.carouselView);
        carouselView.setImageUrls(URLs, new GlideImageLoader(point.x));
    }

    private class GlideImageLoader extends ImageLoader {

        private int width;

        private GlideImageLoader(int width) {
            this.width = width;
        }

        @Override
        protected void loadImage(String url, final OnResourceReadyListener listener) {
            Glide.with(ExampleActivity.this)
                    .load(Uri.parse(url).normalizeScheme())
                    .asBitmap().transform(transformation)
                    .diskCacheStrategy( DiskCacheStrategy.ALL)
                    .into(new SimpleTarget<Bitmap>(width, Target.SIZE_ORIGINAL) {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            // must call this when bitmap resource is ready
                            listener.onResourceReady(resource);
                        }
                    });
        }
    }
}
