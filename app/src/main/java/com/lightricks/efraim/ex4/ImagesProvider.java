package com.lightricks.efraim.ex4;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.lightricks.efraim.ex4.model.ImageModel;

public interface ImagesProvider {

    public int getCount();

    public String getImagePath(int pos);

    public LiveData<ImageModel> getImage(@NonNull String imagePath, @Nullable Integer height, @Nullable Integer width,@Nullable Integer sampleSize);

}
