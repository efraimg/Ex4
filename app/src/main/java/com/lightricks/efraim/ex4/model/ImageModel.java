package com.lightricks.efraim.ex4.model;

import android.graphics.Bitmap;

public class ImageModel {
    private String imagePath;
    private Bitmap bitmap;

    public String getImagePath() {
        return imagePath;
    }

    public ImageModel() {
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public ImageModel(String imagePath, Bitmap bitmap) {
        this.imagePath = imagePath;
        this.bitmap = bitmap;
    }
}
