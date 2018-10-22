package com.lightricks.efraim.ex4.util;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;

import org.apache.commons.lang3.mutable.MutableInt;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class Utility {
    public static int calculateNoOfColumns(Context context, int colWidthDp, int marginDp, @Nullable MutableInt outWidthDp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int dpWidth = (int) (displayMetrics.widthPixels / displayMetrics.density);
        int noOfColumns = dpWidth / colWidthDp;
        if (outWidthDp != null) {
            int gap = dpWidth % colWidthDp - (noOfColumns + 1) * marginDp;
            outWidthDp.setValue(colWidthDp + gap / noOfColumns);
        }
        return noOfColumns;
    }

    public static Set<String> getDirectories(Collection<String> files) {
        if(files == null){
            return null;
        }
        Set<String> dirs = new HashSet<>();
        for (String file : files) {
            if (file.contains(File.separator)) {
                int index = file.lastIndexOf(File.separator);
                String dir = file.substring(0,index);
                dirs.add(dir);
            }
        }
        return dirs;
    }
}