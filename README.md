# ImageCarouselView
Custom image carousel view for Android. Support featrures:
* Infinite auto-scroll (**ALWAYS** in one direction)
* Scrollable manually with auto-scroll
* Auto-settle to the closest position on scrolling manually

![demo](https://github.com/f3401pal/ImageCarouselView/blob/master/demo.gif)

**Note:** The library does not provide ways for loading images from URL. You could use libraries like [Glide](https://github.com/bumptech/glide) (see the example), [Picasso](http://square.github.io/picasso/), [Fresco](http://frescolib.org/) or write your own.

# Limitations
* Only support horizontal scroll
* Only support images from URL
* Manual scroll does not work smoothly inside `NestedScrollView`

# QuickStart
This is a example of using Glide as image loader. See the [example app](https://github.com/f3401pal/ImageCarouselView/tree/master/example) for more details.
* Create a image loader class extend `ImageLoader` and implement `loadImage`
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
**PS:** make sure to call `onResourceReady` when the image bitmap is ready

* Add `ImageCarouselView` into your layout XML
```xml
<com.f3401pal.imagecarouselview.ImageCarouselView
        android:id="@+id/carouselView"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        />
```

* Set the image URLs and a instance of your image loader in `ImageCarouselView`
```android
ImageCarouselView carouselView = (ImageCarouselView) findViewById(R.id.carouselView);
carouselView.setImageUrls(URLs, new GlideImageLoader());
```