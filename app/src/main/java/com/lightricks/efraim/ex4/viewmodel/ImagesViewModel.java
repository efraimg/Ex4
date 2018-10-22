package com.lightricks.efraim.ex4.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.os.FileObserver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LruCache;

import com.lightricks.efraim.ex4.ImagesProvider;
import com.lightricks.efraim.ex4.model.ImageModel;
import com.lightricks.efraim.ex4.repository.ImageRepository;
import com.lightricks.efraim.ex4.util.Observer;
import com.lightricks.efraim.ex4.util.Utility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ImagesViewModel extends AndroidViewModel implements ImagesProvider {
    private ImageRepository imageRepository;
    private LiveData<List<String>> imagePathsList;
    private LruCache<String, LiveData<ImageModel>> bitmapCache;
    private static int CACHE_SIZE = 10 * 1024 * 1024;
    private final List<FileObserver> fileObserversList;

    public ImagesViewModel(@NonNull Application application) {
        super(application);
        imageRepository = new ImageRepository(application.getApplicationContext());

        bitmapCache = new LruCache<String, LiveData<ImageModel>>(CACHE_SIZE) {
            protected int sizeOf(String key, LiveData<ImageModel> value) {
                if (value.getValue() != null && value.getValue().getBitmap() != null) {
                    return value.getValue().getBitmap().getByteCount();
                }
                return 0;
            }
        };
        fileObserversList = new ArrayList<FileObserver>();
    }



    public LiveData<List<String>> getImagePaths() {
        if (imagePathsList == null) {
            synchronized (this) {
                if (imagePathsList == null) {
                    imagePathsList = imageRepository.loadImagePaths(null);
                }
            }
        }
        return imagePathsList;
    }

    @NonNull
    private Observer<List<String>> getNotifyLoadDoneCallback() {
        return new Observer<List<String>>() {
            @Override
            public void update(List<String> data) {
                Log.d("LoadDoneCallback", "getNotifyLoadDoneCallback");
                if (data == null) {
                    return;
                }
                Set<String> dirs = Utility.getDirectories(data);
                synchronized (this) {
                    if (fileObserversList.size() > 0) {
                        for (FileObserver fileObserver : fileObserversList) {
                            fileObserver.stopWatching();
                        }
                        fileObserversList.clear();
                    }
                }
                for (String dir : dirs) {
                    FileObserver fileObserver = new ImageFileObserver(dir);
                    fileObserver.startWatching();
                    fileObserversList.add(fileObserver);
                }
            }
        };
    }

    @Override
    public int getCount() {
        LiveData<List<String>> imagePaths = getImagePaths();
        List<String> list = imagePaths.getValue();
        return list != null ? list.size() : 0;
    }

    @Override
    public String getImagePath(int pos) {
        return getImagePaths().getValue().get(pos);
    }

    @Override
    public LiveData<ImageModel> getImage(@NonNull String imagePath, @Nullable Integer height, @Nullable Integer width, @Nullable Integer sampleSize) {
        LiveData<ImageModel> imageModelLiveData;
        synchronized (bitmapCache) {
            imageModelLiveData = bitmapCache.get(imagePath);
        }
        if (imageModelLiveData == null) {
            imageModelLiveData = imageRepository.getImage(imagePath, height, width, sampleSize);
            synchronized (bitmapCache) {
                if (bitmapCache.get(imagePath) == null) {
                    bitmapCache.put(imagePath, imageModelLiveData);
                }
            }
        }

        return imageModelLiveData;
    }

    class ImageFileObserver extends FileObserver {
        String TAG = "ImageFileObserver";
        private String path;
        public ImageFileObserver(String path) {
            super(path);
            this.path = path;
        }

        @Override
        public void onEvent(int event, @Nullable String s) {
            String fullPath = path + File.separator + s;
            if ( (event & FileObserver.DELETE) == FileObserver.DELETE) {
                imageRepository.removeImagePath(fullPath);
                synchronized (bitmapCache) {
                    if (bitmapCache.get(fullPath) == null) {
                        bitmapCache.remove(fullPath);
                    }
                }
                Log.d(TAG, "File Deleted: " + s);
            } else if ((event & FileObserver.CREATE) == FileObserver.CREATE) {
                imageRepository.addImagePath(fullPath);
                Log.d(TAG, "File Added: " + s);
            }
        }
    }

}
