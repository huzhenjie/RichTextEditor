package com.scrat.app.richtext.util;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by scrat on 2018/1/24.
 */

public class Util {
    public static int getScreenWidth(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.widthPixels;
    }
}
