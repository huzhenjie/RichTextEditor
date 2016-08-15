package com.scrat.app.richtext.img;

import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * Created by yixuanxuan on 16/8/10.
 */
public class UrlDrawable extends BitmapDrawable implements Drawable.Callback {
    private Drawable drawable;

    @Override
    public void draw(Canvas canvas) {
        if (drawable != null)
            drawable.draw(canvas);
    }

    public Drawable getDrawable() {
        return drawable;
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }
}
