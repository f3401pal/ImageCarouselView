package com.f3401pal.imagecarouselview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_UP;

public class ImageCarouselView extends View {
    private static final String TAG = ImageCarouselView.class.getSimpleName();

    private static final boolean AUTO_SCROLL = true;
    private static final DIRECTION AUTO_SCROLL_DIRECTION = DIRECTION.RIGHT;

    private static final long ANIMATION_DELAY = 3000;
    private static final long ANIMATION_DURATION_FULL = 500;
    private static final long ANIMATION_DURATION_HALF = ANIMATION_DURATION_FULL >> 1;
    private static final LinearInterpolator ANIMATION_INTERPOLATOR = new LinearInterpolator();

    private static final float ANIMATION_LEFT_VALUE = -1f;
    private static final float ANIMATION_MIDDLE_VALUE = 0f;
    private static final float ANIMATION_RIGHT_VALUE = 1f;

    private static final float SCROLL_SETTLING_THRESHOLD_LEFT = -0.5f;
    private static final float SCROLL_SETTLING_THRESHOLD_RIGHT = 0.5f;

    private final ValueAnimator TO_RIGHT_ANIMATOR = createAutoScrollAnimation(DIRECTION.RIGHT);
    private final ValueAnimator TO_LEFT_ANIMATOR = createAutoScrollAnimation(DIRECTION.LEFT);

    private enum DIRECTION {LEFT, RIGHT, MIDDLE}

    private final AnimatorUpdateListener animatorUpdateListener = new AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if(isAttachedToWindow()) {
                float f = (float) animation.getAnimatedValue();
                xOffset = (int) (f * getWidth());
                invalidate();
            } else {
                animation.cancel();
            }
        }
    };

    @Nullable
    private ImageLoader imageLoader;
    @Nullable
    private String[] keys;

    @Nullable
    private AnimationTask animationTask;
    private int curPosition;
    private GestureDetectorCompat gestureDetector;
    private SwipeGestureListener gestureListener;
    private Rect hitRect;

    private int xOffset;
    private Paint imagePaint;

    public ImageCarouselView(Context context) {
        super(context);
        init(context);
    }

    public ImageCarouselView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ImageCarouselView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setImageUrls(@NonNull String[] urls, @NonNull ImageLoader imageLoader) {
        keys = urls;
        this.imageLoader = imageLoader;
        if(animationTask == null) {
            // load the first image
            imageLoader.initialLoad(keys[curPosition], this);
            // preload image to the left
            String preLoadKey = keys[getNextPosition(DIRECTION.LEFT)];
            imageLoader.preLoadImage(preLoadKey);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case ACTION_CANCEL:
                Log.d(TAG, "ACTION_CANCEL");
            case ACTION_UP:
                return gestureListener.onUp();
            default:
                return gestureDetector.onTouchEvent(event);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        getHitRect(hitRect);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        switch (visibility) {
            case VISIBLE:
                autoScroll();
                break;
            case INVISIBLE:
            case GONE:
                if(animationTask != null) {
                    animationTask.animator.cancel();
                }
                break;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(imageLoader != null && keys != null) {
            if(xOffset == 0) {
                Bitmap curBitmap = imageLoader.getImageBitmap(keys[curPosition]);
                if(curBitmap != null) {
                    drawImageBitmap(canvas, curBitmap);
                }
            } else {
                Bitmap left;
                Bitmap right;
                int translateLeft;
                int translateRight;
                if(xOffset < 0) {
                    // to the left
                    left = imageLoader.getImageBitmap(keys[getNextPosition(DIRECTION.LEFT)]);
                    right = imageLoader.getImageBitmap(keys[curPosition]);
                    translateLeft = -(canvas.getWidth() + xOffset);
                    translateRight = -xOffset;
                } else {
                    // to the right
                    left = imageLoader.getImageBitmap(keys[curPosition]);
                    right = imageLoader.getImageBitmap(keys[getNextPosition(DIRECTION.RIGHT)]);
                    translateLeft = -xOffset;
                    translateRight = canvas.getWidth() - xOffset;
                }
                if(left != null) {
                    //Save the current state of the canvas
                    canvas.save();
                    canvas.translate(translateLeft, 0);
                    drawImageBitmap(canvas, left);
                    canvas.restore();
                }
                if(right != null) {
                    canvas.save();
                    canvas.translate(translateRight, 0);
                    drawImageBitmap(canvas, right);
                    canvas.restore();
                }
            }
        }
    }

    private void init(Context context) {
        curPosition = 0;
        xOffset = 0;
        imagePaint = new Paint();
        hitRect = new Rect();

        gestureListener = new SwipeGestureListener();
        gestureDetector = new GestureDetectorCompat(context, gestureListener);
    }

    private ValueAnimator createAutoScrollAnimation(DIRECTION direction) {
        ValueAnimator animator;
        switch (direction) {
            case RIGHT:
                animator = ValueAnimator.ofFloat(ANIMATION_MIDDLE_VALUE, ANIMATION_RIGHT_VALUE);
                break;
            case LEFT:
                animator = ValueAnimator.ofFloat(ANIMATION_MIDDLE_VALUE, ANIMATION_LEFT_VALUE);
                break;
            default:
                throw new RuntimeException("Unknown direction " + direction);
        }
        animator.setDuration(ANIMATION_DURATION_FULL);
        animator.setInterpolator(ANIMATION_INTERPOLATOR);
        animator.addListener(new ImageAnimationListener(direction));
        return animator;
    }

    private ValueAnimator createSettlingAnimation(float curFraction) {
        ValueAnimator animator;
        DIRECTION direction;
        if(curFraction < SCROLL_SETTLING_THRESHOLD_LEFT) {
            // settling to current position -1
            animator = ValueAnimator.ofFloat(curFraction, ANIMATION_LEFT_VALUE);
            direction = DIRECTION.LEFT;
        } else if(curFraction >= SCROLL_SETTLING_THRESHOLD_LEFT
                && curFraction <= SCROLL_SETTLING_THRESHOLD_RIGHT) {
            // settling to current position
            animator = ValueAnimator.ofFloat(curFraction, ANIMATION_MIDDLE_VALUE);
            direction = DIRECTION.MIDDLE;
        } else {
            // settling to current position +1
            animator = ValueAnimator.ofFloat(curFraction, ANIMATION_RIGHT_VALUE);
            direction = DIRECTION.RIGHT;
        }

        animator.setDuration(ANIMATION_DURATION_HALF);
        animator.setInterpolator(ANIMATION_INTERPOLATOR);
        animator.addUpdateListener(animatorUpdateListener);
        ImageAnimationListener listener = new ImageAnimationListener(direction);
        animator.addListener(listener);
        if(keys != null) {
            // pre load the image if it did not load by auto scroll
            String key = keys[getNextPosition(direction)];
            if(imageLoader != null && !imageLoader.isImageLoaded(key)) {
                imageLoader.preLoadImage(key);
            }
        }
        return animator;
    }

    private int getNextPosition(DIRECTION direction) {
        if(keys != null) {
            switch (direction) {
                case RIGHT:
                    return (curPosition + 1) % keys.length;
                case LEFT:
                    return (keys.length + curPosition - 1) % keys.length;
                case MIDDLE:
                    return curPosition;
                default:
                    return -1;
            }
        }
        return -1;
    }

    private void autoScroll() {
        if(keys != null && imageLoader != null) {
            if(keys.length > 1 && AUTO_SCROLL) {
                this.animationTask = new AnimationTask(ANIMATION_DELAY, AUTO_SCROLL_DIRECTION);
                imageLoader.autoLoad(keys[getNextPosition(AUTO_SCROLL_DIRECTION)], animationTask);
            } else {
                String url = keys[getNextPosition(AUTO_SCROLL_DIRECTION)];
                imageLoader.preLoadImage(url);
            }
        }
    }

    private void drawImageBitmap(Canvas canvas, Bitmap bitmap) {
        int cx = (canvas.getWidth() - bitmap.getWidth()) >> 1;
        int cy = (canvas.getHeight() - bitmap.getHeight()) >> 1;
        canvas.drawBitmap(bitmap, cx, cy, imagePaint);
    }

    protected class AnimationTask implements Runnable {

        private final long createdTime;
        private final long delay;

        @NonNull
        private final ValueAnimator animator;

        private AnimationTask(long delay, DIRECTION direction) {
            this.delay = delay;
            this.createdTime = SystemClock.elapsedRealtime();
            switch (direction) {
                case RIGHT:
                    animator = TO_RIGHT_ANIMATOR;
                    break;
                case LEFT:
                    animator = TO_LEFT_ANIMATOR;
                    break;
                default:
                    throw new RuntimeException("Unknown direction " + direction);
            }
            animator.removeAllUpdateListeners();
            animator.addUpdateListener(animatorUpdateListener);
        }

        @Override
        public void run() {
            long delta = SystemClock.elapsedRealtime() - createdTime;
            if(delta > this.delay) {
                animator.setStartDelay(0);
            } else {
                animator.setStartDelay(this.delay - delta);
            }
            animator.start();
        }

        private boolean onScrolled(int savedOffset) {
            if(savedOffset == xOffset || xOffset == 0) {
                animator.resume();
                return true;
            } else {
                animator.cancel();
                return false;
            }
        }
    }

    protected class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {

        private int savedOffset;

        private boolean onUp() {
            if(getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(false);
            }
            if(animationTask != null && animationTask.onScrolled(savedOffset)) {
                Log.d(TAG, "no op");
            } else {
                Log.d(TAG, "settling...");
                float fraction = (float) xOffset / getWidth();
                createSettlingAnimation(fraction).start();
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            savedOffset = xOffset;
            if(animationTask != null) {
                animationTask.animator.pause();
            }
            if(getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if(distanceX != 0) {
                if(animationTask != null) {
                    animationTask.animator.cancel();
                    animationTask = null;
                }
                xOffset +=distanceX;
                invalidate();
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return onUp();
        }

    }

    protected class ImageAnimationListener implements Animator.AnimatorListener {

        private boolean canceled;
        private final DIRECTION direction;

        private ImageAnimationListener(DIRECTION direction) {
            this.direction = direction;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            canceled = false;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if(canceled) {
                Log.d(TAG, "animation canceled");
            } else {
                curPosition = getNextPosition(direction);
                Log.d(TAG, "animation end with position " + curPosition);
                xOffset = 0;
                autoScroll();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            canceled = true;
        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }
}
