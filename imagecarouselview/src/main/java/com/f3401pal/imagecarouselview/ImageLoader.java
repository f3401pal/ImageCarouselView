package com.f3401pal.imagecarouselview;


import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.View;

public abstract class ImageLoader {
    // make sure the LruCache can fit at least 3 of your bitmap
    private static final int LRU_CACHE_SIZE_FACTOR = 8;

    private LruCache<String, Bitmap> memCache;

    protected ImageLoader() {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / LRU_CACHE_SIZE_FACTOR;

        memCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap image) {
                return image.getByteCount()/1024;
            }
        };
    }

    protected abstract void loadImage(String url, OnResourceReadyListener listener);

    synchronized Bitmap getImageBitmap(String key) {
        return memCache.get(key);
    }

    synchronized boolean isImageLoaded(String key) {
        return memCache.get(key) != null;
    }

    final void preLoadImage(String url) {
        softLoadImage(url, new BaseTarget(url));
    }

    final void initialLoad(String url, View view) {
        softLoadImage(url, new InitialLoadTarget(url, view));
    }

    final void autoLoad(String url, ImageCarouselView.AnimationTask task) {
        softLoadImage(url, new AutoScrollLoadTarget(url, task));
    }

    private void softLoadImage(String url, BaseTarget listener) {
        if(isImageLoaded(url)) {
            listener.postAction();
        } else {
            loadImage(url, listener);
        }
    }

    private class BaseTarget implements OnResourceReadyListener {

        private final String toKey;

        private BaseTarget(String toKey) {
            this.toKey = toKey;
        }

        @Override
        public void onResourceReady(Bitmap resource) {
            memCache.put(toKey, resource);
            postAction();
        }

        public void postAction() {
            // does nothing
        }
    }

    private class InitialLoadTarget extends BaseTarget {

        private final View targetView;

        private InitialLoadTarget(String toKey, View targetView) {
            super(toKey);
            this.targetView = targetView;
        }

        @Override
        public void postAction() {
            targetView.invalidate();
        }
    }

    private class AutoScrollLoadTarget extends BaseTarget {

        private final ImageCarouselView.AnimationTask animationTask;

        private AutoScrollLoadTarget(String toKey, ImageCarouselView.AnimationTask animationTask) {
            super(toKey);
            this.animationTask = animationTask;
        }

        @Override
        public void postAction() {
            animationTask.run();
        }
    }

}
