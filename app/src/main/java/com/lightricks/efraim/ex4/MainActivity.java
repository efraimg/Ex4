package com.lightricks.efraim.ex4;

import android.Manifest;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.lightricks.efraim.ex4.model.ImageModel;
import com.lightricks.efraim.ex4.util.Utility;
import com.lightricks.efraim.ex4.viewmodel.ImagesViewModel;

import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;

public class MainActivity extends AppCompatActivity {


    public static final String IMAGE_PATH_KEY = "image_path";
    private static int READ_EXTERNAL_STORAGE_CODE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadImagesAndVerifayPermission();
    }

    private void loadImagesAndVerifayPermission() {
        if (isReadStoragePermissionGranted()) {
            initImageGrid();
        }

    }

    public boolean isReadStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_CODE);
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_EXTERNAL_STORAGE_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initImageGrid();
            } else {
                Toast.makeText(this, "can't display images without permission",
                        Toast.LENGTH_LONG).show();
            }
        }
    }


    private void initImageGrid() {
        RecyclerView mRecyclerView = findViewById(R.id.recycler_view);
        MutableInt outWidth = new MutableInt();
        int numCols = Utility.calculateNoOfColumns(this,100,2,outWidth);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this,numCols);
        mRecyclerView.setLayoutManager(mLayoutManager);

        final ImagesViewModel imagesViewModel = ViewModelProviders.of(this).get(ImagesViewModel.class);
        final ImagesAdaptor imagesAdaptor = new ImagesAdaptor(imagesViewModel, this,outWidth.getValue());
        imagesAdaptor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ImageFullViewActivity.class);
                intent.putExtra(IMAGE_PATH_KEY, view.getTag().toString());
                MainActivity.this.startActivity(intent);
                Log.d("onClick", view.getTag() + "");
            }
        });
        mRecyclerView.setAdapter(imagesAdaptor);
        imagesViewModel.getImagePaths().observe(this, new Observer<List<String>>() {
            @Override
            public void onChanged(@Nullable List<String> contacts) {
                Log.d("image-observer", contacts.size() + "");
                imagesAdaptor.notifyDataSetChanged();

            }
        });

    }
}




class ImagesAdaptor extends RecyclerView.Adapter<ImagesAdaptor.ViewHolder> {
    final private ImagesProvider dataProvider;
    final private AppCompatActivity activity;
    private View.OnClickListener onClickListener;
    private int itemWidthAndHeight;

    ImagesAdaptor(ImagesProvider dataProvider, AppCompatActivity activity, int itemWidthAndHeightDp) {
        this.dataProvider = dataProvider;
        this.activity = activity;
        this.itemWidthAndHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                itemWidthAndHeightDp, activity.getResources().getDisplayMetrics());;
    }

    @Override
    @NonNull
    public ImagesAdaptor.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                       int viewType) {

        LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(activity)
                .inflate(R.layout.image_view_grid, parent, false);

        ImageView imageView = linearLayout.findViewById(R.id.iv_image);
        imageView.getLayoutParams().height = itemWidthAndHeight;
        imageView.getLayoutParams().width = itemWidthAndHeight;
        if(onClickListener != null){
            imageView.setOnClickListener(onClickListener);
        }

        return new ViewHolder(linearLayout);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final ImageView imageView = holder.listItem.findViewById(R.id.iv_image);
        final String imagePath = dataProvider.getImagePath(position);
        imageView.setTag(imagePath);
        imageView.setImageBitmap(null);
        final LiveData<ImageModel> imageModelLiveData = dataProvider.getImage(imagePath, 100, 100,null);
        if(imageModelLiveData.getValue() != null && imageModelLiveData.getValue().getBitmap()!= null){
            imageView.setImageBitmap(imageModelLiveData.getValue().getBitmap());
        }else{
            imageModelLiveData.observe(activity, new Observer<ImageModel>() {
                @Override
                public void onChanged(@Nullable ImageModel imageModel) {
                    imageModelLiveData.removeObserver(this);
                    if (imageModel.getImagePath().equals(imageView.getTag())) {
                        imageView.setImageBitmap(imageModel.getBitmap());
                    }

                }
            });
        }


    }

    @Override
    public int getItemCount() {
        int count = dataProvider.getCount();
        Log.d("image-count", count + "");
        return count;
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout listItem;

        ViewHolder(LinearLayout view) {
            super(view);
            listItem = view;
        }
    }
}