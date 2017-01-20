# ImageCarouselView
Custom image carousel view for Android. Support featrures:
* Infinite auto-scroll (ALWAYS in one direction)
* Scrollable manually with auto-scroll
* Auto-settle to the closeset position on scrolling manually

**Note:** The library does not provide ways for loading images from URL. You could use libraries like [Glide](https://github.com/bumptech/glide) (see the example), [Picasso](http://square.github.io/picasso/), [Fresco](http://frescolib.org/) or write your own.
# Limitations
* Manual scroll does not work smoothly inside `NestedScrollView`
* Only support images from URL
# Quick start
This is a example of using Glide as image loader. See the example app for more details.
1. Create a image loader class extend `ImageLoader` and implement `loadImage`
```android
private class GlideImageLoader extends ImageLoader {
        @Override
        protected void loadImage(String url, final OnResourceReadyListener listener) {
            // your own image loading logic here
            Glide.with(ExampleActivity.this)
                    .load(Uri.parse(url).normalizeScheme()).asBitmap()
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            // must call this when bitmap resource is ready
                            listener.onResourceReady(resource);
                        }
                    });
        }
}
```