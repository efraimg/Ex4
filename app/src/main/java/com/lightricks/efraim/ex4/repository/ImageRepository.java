package com.lightricks.efraim.ex4.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.lightricks.efraim.ex4.model.ImageModel;
import com.lightricks.efraim.ex4.util.Observer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ImageRepository {
    public static final int GL_MAX_TEXTURE_SIZE_MIN_VAL = 2048 * 2048;
    private Context context;
    private ThreadPoolExecutor threadPoolExecutor;
    private final MutableLiveData<List<String>> imagePathsListLiveData = new MutableLiveData<>();

    private Object loadImagesMutex = new Object();


    private static final Integer SAMPLE_SIZE_DEFAULT = 16;


    private  final List<String> imagePaths = Collections.synchronizedList(new ArrayList()); ;

    public ImageRepository(Context context) {
        this.context = context;
        initThreadPool();
        Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        context.getContentResolver().registerContentObserver(imagesUri, true, new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                loadImagePaths(null,true);

            }
        });
    }



    public LiveData<List<String>> loadImagePaths(@Nullable Observer<List<String>> notifyLoadDone) {
        return loadImagePaths(notifyLoadDone,false);
    }
    public LiveData<List<String>> loadImagePaths(@Nullable Observer<List<String>> notifyLoadDone,boolean notifyOnlyWhenDone) {
        new ImagePathsRetriever(notifyLoadDone,notifyOnlyWhenDone).start();
        return imagePathsListLiveData;
    }

    public LiveData<ImageModel> getImage(String imagePath, @Nullable Integer height, @Nullable Integer width, @Nullable Integer sampleSize) {
        MutableLiveData<ImageModel> image = new MutableLiveData<>();
        threadPoolExecutor.execute(new ImageRetriever(image, imagePath, height, width, sampleSize));
        return image;
    }

    public void addImagePath(String imagePath){
        imagePaths.add(0,imagePath);
        postImagePathes(imagePaths);
    }

    public void removeImagePath(String imagePath){
        imagePaths.remove(imagePath);
        postImagePathes(imagePaths);
    }

    private void initThreadPool() {
        int cores = Runtime.getRuntime().availableProcessors();
        LinkedBlockingDeque<Runnable> blockingStack = new Stack<>();

        threadPoolExecutor = new ThreadPoolExecutor(0,cores*2, 1000*4, TimeUnit.MILLISECONDS,blockingStack);

    }

    private void postImagePathes(List<String> imagePathes) {
        imagePathsListLiveData.postValue(new ArrayList<String>(imagePathes));
    }

    class ImagePathsRetriever extends Thread {
        private Observer<List<String>> notifyLoadDone;
        private boolean notifyOnlyWhenDone;
        public ImagePathsRetriever(Observer<List<String>> notifyLoadDone, boolean notifyOnlyWhenDone) {
            this.notifyLoadDone = notifyLoadDone;
            this.notifyOnlyWhenDone = notifyOnlyWhenDone;
        }
        public ImagePathsRetriever(){}



        @Override
        public void run() {
            List<String> loadedImagePaths =  loadImagePaths(!notifyOnlyWhenDone);
            synchronized (ImageRepository.this.imagePaths){
                ImageRepository.this.imagePaths.clear();
                ImageRepository.this.imagePaths.addAll(loadedImagePaths);
            }

            if(notifyOnlyWhenDone){
                postImagePathes(loadedImagePaths);
            }

            if(this.notifyLoadDone != null){
                this.notifyLoadDone.update(new ArrayList<String>(loadedImagePaths));
            }
        }

    }

    class ImageRetriever extends Thread {
        private MutableLiveData<ImageModel> imageModel;
        private String imagePath;
        private Integer height;
        private Integer width;
        private Integer sampleSize;

        ImageRetriever(MutableLiveData<ImageModel> image, String imagePath, @Nullable Integer height, @Nullable Integer width, @Nullable Integer sampleSize) {
            this.imageModel = image;
            this.imagePath = imagePath;
            this.height = height;
            this.width = width;
            this.sampleSize = sampleSize;
        }

        @Override
        public void run() {
            loadImage(imageModel, imagePath, height, width, sampleSize);
        }
    }

    private List<String> loadImagePaths(boolean notify) {
        synchronized (loadImagesMutex){
            Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
            final String orderBy = MediaStore.Images.Media.DATE_TAKEN;
            Cursor cursor = context.getContentResolver().query(imagesUri, projection, null, null, orderBy + " DESC");
            int columnIndexData = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

            List<String> imagePaths = new ArrayList<>();
            while (cursor.moveToNext()) {
                imagePaths.add(cursor.getString(columnIndexData));
                if(notify){
                    postImagePathes(imagePaths);
                }

            }

            Log.d("",imagePaths.toString());
            return imagePaths;
        }
    }

    private void loadImage(@NonNull MutableLiveData<ImageModel> imageModelLiveData, String imagePath, @Nullable Integer height, @Nullable Integer width, @Nullable Integer sampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        if (sampleSize == null) {
            if (height != null && width != null) {
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imagePath, options);
                options.inSampleSize = calculateInSampleSize(options, width, height);
                options.inJustDecodeBounds = false;
            } else {
                options.inSampleSize = SAMPLE_SIZE_DEFAULT;
            }
        } else {
            options.inSampleSize = sampleSize;
        }

        if (height != null && width != null) {
            options.outWidth = width;
            options.outHeight = height;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

        imageModelLiveData.postValue(new ImageModel(imagePath, bitmap));
    }

    private static int calculateInSampleSize(
        BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            
            while (((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) || (height * width / inSampleSize > GL_MAX_TEXTURE_SIZE_MIN_VAL)) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
    class Stack<T> extends LinkedBlockingDeque<T> {

        @Override
        public boolean offer(T t) {
            return super.offerFirst(t);
        }

        @Override
        public T remove() {
            return super.removeFirst();
        }
    }


}
