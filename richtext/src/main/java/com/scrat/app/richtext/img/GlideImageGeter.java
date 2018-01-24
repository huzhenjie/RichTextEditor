package com.scrat.app.richtext.img;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.widget.TextView;

import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.scrat.app.richtext.glide.GlideRequest;
import com.scrat.app.richtext.glide.GlideRequests;
import com.scrat.app.richtext.util.Util;

import java.util.HashSet;

/**
 * Created by yixuanxuan on 16/8/10.
 */
public class GlideImageGeter implements Html.ImageGetter {

    private HashSet<Target> targets;
    private HashSet<GifDrawable> gifDrawables;
    private final TextView textView;
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

    public GlideImageGeter(TextView textView, GlideRequests glideRequests) {
        this.textView = textView;
        targets = new HashSet<>();
        gifDrawables = new HashSet<>();
        gifLoadRequest = glideRequests.asGif();
        bitmapLoadRequest = glideRequests.asBitmap();
//        mTextView.setTag(R.id.img_tag, this);
    }

    @Override
    public Drawable getDrawable(String url) {
        if (url == null) {
            return null;
        }

        final UrlDrawable urlDrawable = new UrlDrawable();
        final Target target;
        if (isGif(url)) {
            target = new GifTarget(urlDrawable, textView, gifDrawables);
            gifLoadRequest.load(url).into(target);
        } else {
            target = new BitmapTarget(urlDrawable, textView);
            bitmapLoadRequest.load(url).into(target);
        }
        targets.add(target);
        return urlDrawable;
    }

    private static boolean isGif(String path) {
        int index = path.lastIndexOf('.');
        return index > 0 && "gif".toUpperCase().equals(path.substring(index + 1).toUpperCase());
    }

    private static class GifTarget extends SimpleTarget<GifDrawable> {
        private final UrlDrawable urlDrawable;
        private TextView textView;
        private HashSet<GifDrawable> gifDrawables;

        private GifTarget(
                UrlDrawable urlDrawable, TextView textView, HashSet<GifDrawable> gifDrawables) {
            this.urlDrawable = urlDrawable;
            this.textView = textView;
            this.gifDrawables = gifDrawables;
        }

        @Override
        public void onResourceReady(GifDrawable resource, Transition<? super GifDrawable> transition) {
            int w = Util.getScreenWidth(textView.getContext())
                    - textView.getPaddingRight()
                    - textView.getPaddingLeft();
            int hh = resource.getIntrinsicHeight();
            int ww = resource.getIntrinsicWidth();
            int high = hh * w / ww;
            Rect rect = new Rect(0, 0, w, high);
            resource.setBounds(rect);
            urlDrawable.setBounds(rect);
            urlDrawable.setDrawable(resource);
            gifDrawables.add(resource);
            resource.setCallback(textView);
            resource.start();
            resource.setLoopCount(GifDrawable.LOOP_FOREVER);
            textView.setText(textView.getText());
            textView.invalidate();
        }

    }

    private static class BitmapTarget extends SimpleTarget<Bitmap> {
        private final UrlDrawable urlDrawable;
        private TextView textView;

        private BitmapTarget(UrlDrawable urlDrawable, TextView textView) {
            this.urlDrawable = urlDrawable;
            this.textView = textView;
        }

        @Override
        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
            Drawable drawable = new BitmapDrawable(textView.getContext().getResources(), resource);
            int screenWidth = Util.getScreenWidth(textView.getContext())
                    - textView.getPaddingRight()
                    - textView.getPaddingLeft();
            int drawableHeight = drawable.getIntrinsicHeight();
            int drawableWidth = drawable.getIntrinsicWidth();
            int targetHeight = drawableHeight * screenWidth / drawableWidth;
            Rect rect = new Rect(0, 0, screenWidth, targetHeight);
            drawable.setBounds(rect);
            urlDrawable.setBounds(rect);
            urlDrawable.setDrawable(drawable);
            textView.setText(textView.getText());
            textView.invalidate();
        }
    }

}
