package com.scrat.app.richtext.img;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.WindowManager;
import android.widget.TextView;

import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.scrat.app.richtext.glide.GlideRequest;
import com.scrat.app.richtext.glide.GlideRequests;

import java.util.HashSet;

/**
 * Created by yixuanxuan on 16/8/10.
 */
public class GlideImageGeter implements Html.ImageGetter {

    private HashSet<Target> targets;
    private HashSet<GifDrawable> gifDrawables;
    private final Context mContext;
    private final TextView mTextView;
    private GlideRequest<GifDrawable> gifLoadRequest;
    private GlideRequest<Bitmap> bitmapLoadRequest;

    public void recycle() {
        targets.clear();
        for (GifDrawable gifDrawable : gifDrawables) {
            gifDrawable.setCallback(null);
            gifDrawable.recycle();
        }
        gifDrawables.clear();
    }

    public GlideImageGeter(Context context, TextView textView, GlideRequests glideRequests) {
        this.mContext = context;
        this.mTextView = textView;
        targets = new HashSet<>();
        gifDrawables = new HashSet<>();
        gifLoadRequest = glideRequests.asGif();
        bitmapLoadRequest = glideRequests.asBitmap();
//        mTextView.setTag(R.id.img_tag, this);
    }

    @Override
    public Drawable getDrawable(String url) {
        if (url == null)
            return null;

        final UrlDrawable urlDrawable = new UrlDrawable();
        final Target target;
        if (isGif(url)) {
            target = new GifTarget(urlDrawable);
            gifLoadRequest.load(url).into(target);
        } else {
            target = new BitmapTarget(urlDrawable);
            bitmapLoadRequest.load(url).into(target);
        }
        targets.add(target);
        return urlDrawable;
    }

    private static boolean isGif(String path) {
        int index = path.lastIndexOf('.');
        return index > 0 && "gif".toUpperCase().equals(path.substring(index + 1).toUpperCase());
    }

    private class GifTarget extends SimpleTarget<GifDrawable> {
        private final UrlDrawable urlDrawable;


        private GifTarget(UrlDrawable urlDrawable) {
            this.urlDrawable = urlDrawable;
        }

        @Override
        public void onResourceReady(GifDrawable resource, Transition<? super GifDrawable> transition) {
            int w = getScreenSize(mContext).x;
            int hh = resource.getIntrinsicHeight();
            int ww = resource.getIntrinsicWidth();
            int high = hh * (w - 50) / ww;
            Rect rect = new Rect(20, 20, w - 30, high);
            resource.setBounds(rect);
            urlDrawable.setBounds(rect);
            urlDrawable.setDrawable(resource);
            gifDrawables.add(resource);
            resource.setCallback(mTextView);
            resource.start();
            resource.setLoopCount(GifDrawable.LOOP_FOREVER);
            mTextView.setText(mTextView.getText());
            mTextView.invalidate();
        }
    }

    private class BitmapTarget extends SimpleTarget<Bitmap> {
        private final UrlDrawable urlDrawable;

        private BitmapTarget(UrlDrawable urlDrawable) {
            this.urlDrawable = urlDrawable;
        }

        @Override
        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
            Drawable drawable = new BitmapDrawable(mContext.getResources(), resource);
            int w = getScreenSize(mContext).x;
            int hh = drawable.getIntrinsicHeight();
            int ww = drawable.getIntrinsicWidth();
            int high = hh * (w - 40) / ww;
            Rect rect = new Rect(20, 20, w - 20, high);
            drawable.setBounds(rect);
            urlDrawable.setBounds(rect);
            urlDrawable.setDrawable(drawable);
            mTextView.setText(mTextView.getText());
            mTextView.invalidate();
        }
    }

    private Point getScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point screenSize = new Point();
        if (wm != null) {
            wm.getDefaultDisplay().getSize(screenSize);
        }
        return screenSize;
    }
}
