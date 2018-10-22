package com.lightricks.efraim.ex4;

import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.lightricks.efraim.ex4.model.ImageModel;
import com.lightricks.efraim.ex4.repository.ImageRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;


public class ImageFullViewActivity extends AppCompatActivity {

    private ScaleGestureDetector mScaleGestureDetector;
    private float mScaleFactor = 1.0f;
    private ImageView imageView;
    private MyGestureListener myGestureListener;
    private GestureDetectorCompat mDetector;
    int layoutWidth;
    int layoutHeight;
    float marginX = -1;
    float marginY = -1;
    final Set<Integer> pointerIds = Collections.synchronizedSet(new HashSet<Integer>());
    private AtomicLong scaleDone = new AtomicLong(0);
    private ScaleListener scaleListener = new ScaleListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_fullview);

        final RelativeLayout viewGroup = findViewById(R.id.imageFullView);

        imageView = findViewById(R.id.imageView);
        mScaleGestureDetector = new ScaleGestureDetector(this, scaleListener);

        fetchPhotoWhenLayoutSizeKnown(viewGroup);
        myGestureListener = new MyGestureListener();
        mDetector = new GestureDetectorCompat(this, myGestureListener);


    }

    private void fetchPhotoWhenLayoutSizeKnown(final RelativeLayout viewGroup) {
        ViewTreeObserver observer = viewGroup.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                int[] arr = new int[2];
                viewGroup.getLocationOnScreen(arr);
                viewGroup.getLocationInWindow(arr);
                int height = viewGroup.getHeight();
                int width = viewGroup.getWidth();
                ImageFullViewActivity.this.layoutHeight = height;
                ImageFullViewActivity.this.layoutWidth = width;

                viewGroup.getViewTreeObserver().removeGlobalOnLayoutListener(
                        this);
                ImageRepository imageRepository = new ImageRepository(ImageFullViewActivity.this);
                Intent intent = getIntent();
                String path = intent.getStringExtra(MainActivity.IMAGE_PATH_KEY);
                imageRepository.getImage(path, height, width, null).observe(ImageFullViewActivity.this, new Observer<ImageModel>() {
                    @Override
                    public void onChanged(@Nullable ImageModel imageModel) {

                        ImageView imageView = viewGroup.findViewById(R.id.imageView);
                        imageView.setImageBitmap(imageModel.getBitmap());

                    }
                });

            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        scaleListener.motionEvent = motionEvent;
        mScaleGestureDetector.onTouchEvent(motionEvent);
        boolean procced = true;
        for(int i = 0 ; i < motionEvent.getPointerCount(); i++){
            if(pointerIds.contains(motionEvent.getPointerId(i))){
                procced = false;
            }
        }

        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
           if(procced){
               myGestureListener.onUp();
           }
           pointerIds.clear();
        }
        if(procced){
            mDetector.onTouchEvent(motionEvent);
        }

        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        public static final String TAG = "TAGGG";
        float marginX = -1;
        float marginY = -1;
        private float relativeLocationX;
        private float relativeLocationY;
        MotionEvent motionEvent;

        public boolean onScaleBegin(ScaleGestureDetector detector) {
            if (marginX == -1) {
                marginX = imageView.getX();
                marginY = imageView.getY();
            }
            int midX = layoutWidth / 2;
            int midY = layoutHeight / 2;
            float focusY = detector.getFocusY();
            float focusX = detector.getFocusX();
            relativeLocationX = (midX - focusX)/midX;
            relativeLocationY = (midY - focusY)/midY;

            for(int i = 0 ; i < motionEvent.getPointerCount(); i++){
                pointerIds.add(motionEvent.getPointerId(i));
            }

            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            float prevScaleFactor = mScaleFactor;
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(0.5f,
                    Math.min(mScaleFactor, 10.0f));
            if(prevScaleFactor == mScaleFactor){
                return true;
            }
            imageView.animate().scaleX(mScaleFactor).scaleY(mScaleFactor).setDuration(0).start();

            float transXDif = (mScaleFactor - (mScaleFactor/scaleGestureDetector.getScaleFactor())) * imageView.getWidth()/2;
            float moveX = transXDif * relativeLocationX;
            float transYDif = (mScaleFactor - (mScaleFactor/scaleGestureDetector.getScaleFactor())) * imageView.getHeight()/2;
            float moveY = transYDif * relativeLocationY;

            float x = imageView.getX() + moveX;
            float y = imageView.getY() + moveY;

            float[] m = new float[9];
            imageView.getMatrix().getValues(m);

            float width = imageView.getWidth()* mScaleFactor;
            float height = imageView.getHeight()* mScaleFactor;
            float transX =  m[Matrix.MTRANS_X] - transXDif;
            float transY =  m[Matrix.MTRANS_Y] - transYDif;
            Log.d(TAG,"transX " + transX);
            Log.d(TAG,"transY " + transY);
            Log.d(TAG,"width " + width);
            Log.d(TAG,"height " + height);
//            imageView.setX(x);
//            imageView.setY(y);
            fixLocation(transX,transY,x,y,width,height,0,0);


            return true;
        }


        public void onScaleEnd(ScaleGestureDetector detector) {
            if(mScaleFactor < 1f){
                mScaleFactor = 1f;
                imageView.animate().scaleX(mScaleFactor).scaleY(mScaleFactor).setDuration(300).start();

            }
            int width = (int) (imageView.getWidth() * imageView.getScaleX());
            int height = (int) (imageView.getHeight() * imageView.getScaleY());

            float[] m = new float[9];
            imageView.getMatrix().getValues(m);
            Log.d(TAG,"transX " + m[Matrix.MTRANS_X]);
            Log.d(TAG,"transY " + m[Matrix.MTRANS_Y]);
            Log.d(TAG,"width " + width);
            Log.d(TAG,"height " + height);
            fixLocation(m[Matrix.MTRANS_X],m[Matrix.MTRANS_Y],imageView.getX(),imageView.getY(),width,height,300,0);
            scaleDone.set(System.currentTimeMillis());
        }

        private  void fixLocation(float transX,float transY, float x, float y, float width, float height, long duration,long delta ) {


            if (width < layoutWidth) {
               x = marginX;
            } else if (-transX < marginX + delta) {
                x = ImageFullViewActivity.this.imageView.getX() - (transX + marginX);
            } else if (transX + marginX + width < layoutWidth + delta) {
                x = ImageFullViewActivity.this.imageView.getX() + (layoutWidth - (transX + marginX + width));
            }

            if (height < layoutHeight) {
               y = marginY;
            } else if (-transY < marginY) {
                y = ImageFullViewActivity.this.imageView.getY() - (transY + marginY);
            } else if (transY + marginY + height < layoutHeight + delta) {
                y = ImageFullViewActivity.this.imageView.getY() + (layoutHeight - (transY + marginY + height));
            }

            if(ImageFullViewActivity.this.imageView.getX() != x || ImageFullViewActivity.this.imageView.getY() != y){
                ImageFullViewActivity.this.imageView.animate()
                        .x(x)
                        .y(y)
                        .setDuration(duration)
                        .start();
            }

        }
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        public static final String TAG = "TAG";

        float dx;
        float dy;




        @Override
        public boolean onDown(MotionEvent e) {
            dx = imageView.getX() - e.getRawX();
            dy = imageView.getY() - e.getRawY();
            if (marginX == -1) {
                marginX = imageView.getX();
                marginY = imageView.getY();
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                                float distanceY) {

            Log.d("POS",imageView.getX() + " " + imageView.getY());

            float moveX = event2.getRawX() + dx;
            float moveY = event2.getRawY() + dy;


            imageView.animate()
                    .x(moveX)
                    .y(moveY)
                    .setDuration(0)
                    .start();
            return true;
        }


        public boolean onUp() {


            int width = (int) (imageView.getWidth() * imageView.getScaleX());
            int height = (int) (imageView.getHeight() * imageView.getScaleY());

            float[] m = new float[9];
            imageView.getMatrix().getValues(m);

            float x = ImageFullViewActivity.this.imageView.getX();
            float y = ImageFullViewActivity.this.imageView.getY();
            if (width < layoutWidth) {
                x = marginX;
            } else if (-m[Matrix.MTRANS_X] < marginX) {
                x = ImageFullViewActivity.this.imageView.getX() - (m[Matrix.MTRANS_X] + marginX);
            } else if (m[Matrix.MTRANS_X] + marginX + width < layoutWidth) {
                x = ImageFullViewActivity.this.imageView.getX() + (layoutWidth - (m[Matrix.MTRANS_X] + marginX + width));
            }

            if (height < layoutHeight) {
                y = marginY;
            } else if (-m[Matrix.MTRANS_Y] < marginY) {
                y = ImageFullViewActivity.this.imageView.getY() - (m[Matrix.MTRANS_Y] + marginY);
            } else if (m[Matrix.MTRANS_Y] + marginY + height < layoutHeight) {
                y = ImageFullViewActivity.this.imageView.getY() + (layoutHeight - (m[Matrix.MTRANS_Y] + marginY + height));
            }

            if(ImageFullViewActivity.this.imageView.getX() != x || ImageFullViewActivity.this.imageView.getY() != y){
                ImageFullViewActivity.this.imageView.animate()
                        .x(x)
                        .y(y)
                        .setDuration(300)
                        .start();
            }

            return true;
        }

    }

}


