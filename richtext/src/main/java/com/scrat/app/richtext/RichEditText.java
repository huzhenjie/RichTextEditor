package com.scrat.app.richtext;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BulletSpan;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodManager;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.scrat.app.richtext.glide.GlideApp;
import com.scrat.app.richtext.glide.GlideRequests;
import com.scrat.app.richtext.img.GlideImageGeter;
import com.scrat.app.richtext.parser.HtmlParser;
import com.scrat.app.richtext.parser.MarkdownParser;
import com.scrat.app.richtext.span.MyBulletSpan;
import com.scrat.app.richtext.span.MyImgSpan;
import com.scrat.app.richtext.span.MyQuoteSpan;
import com.scrat.app.richtext.span.MyURLSpan;
import com.scrat.app.richtext.util.BitmapUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RichEditText extends AppCompatEditText implements TextWatcher {
    public static final int FORMAT_BOLD = 0x01;
    public static final int FORMAT_ITALIC = 0x02;
    public static final int FORMAT_UNDERLINED = 0x03;
    public static final int FORMAT_STRIKETHROUGH = 0x04;
    public static final int FORMAT_BULLET = 0x05;
    public static final int FORMAT_QUOTE = 0x06;
    public static final int FORMAT_LINK = 0x07;

    private int bulletColor = 0;
    private int bulletRadius = 0;
    private int bulletGapWidth = 0;
    private boolean historyEnable = true;
    private int historySize = 100;
    private int linkColor = 0;
    private boolean linkUnderline = true;
    private int quoteColor = 0;
    private int quoteStripeWidth = 0;
    private int quoteGapWidth = 0;

    private List<Editable> historyList = new LinkedList<>();
    private boolean historyWorking = false;
    private int historyCursor = 0;

    private SpannableStringBuilder inputBefore;
    private GlideRequests glideRequests;
    private Editable inputLast;

    public RichEditText(Context context) {
        super(context);
        init(context, null);
    }

    public RichEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RichEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        glideRequests = GlideApp.with(context);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RichEditText);
        bulletColor = array.getColor(R.styleable.RichEditText_bulletColor, 0);
        bulletRadius = array.getDimensionPixelSize(R.styleable.RichEditText_bulletRadius, 0);
        bulletGapWidth = array.getDimensionPixelSize(R.styleable.RichEditText_bulletGapWidth, 0);
        historyEnable = array.getBoolean(R.styleable.RichEditText_historyEnable, true);
        historySize = array.getInt(R.styleable.RichEditText_historySize, 100);
        linkColor = array.getColor(R.styleable.RichEditText_linkColor, 0);
        linkUnderline = array.getBoolean(R.styleable.RichEditText_linkUnderline, true);
        quoteColor = array.getColor(R.styleable.RichEditText_quoteColor, 0);
        quoteStripeWidth = array.getDimensionPixelSize(R.styleable.RichEditText_quoteStripeWidth, 0);
        quoteGapWidth = array.getDimensionPixelSize(R.styleable.RichEditText_quoteCapWidth, 0);
        array.recycle();

        if (historyEnable && historySize <= 0) {
            throw new IllegalArgumentException("historySize must > 0");
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        addTextChangedListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeTextChangedListener(this);
    }

    // StyleSpan ===================================================================================

    public void bold(boolean valid) {
        if (valid) {
            styleValid(Typeface.BOLD, getSelectionStart(), getSelectionEnd());
        } else {
            styleInvalid(Typeface.BOLD, getSelectionStart(), getSelectionEnd());
        }
    }

    public void italic(boolean valid) {
        if (valid) {
            styleValid(Typeface.ITALIC, getSelectionStart(), getSelectionEnd());
        } else {
            styleInvalid(Typeface.ITALIC, getSelectionStart(), getSelectionEnd());
        }
    }

    protected void styleValid(int style, int start, int end) {
        switch (style) {
            case Typeface.NORMAL:
            case Typeface.BOLD:
            case Typeface.ITALIC:
            case Typeface.BOLD_ITALIC:
                break;
            default:
                return;
        }

        if (start >= end) {
            return;
        }

        StyleSpan span = new StyleSpan(style);
        getEditableText().setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    protected void styleInvalid(int style, int start, int end) {
        switch (style) {
            case Typeface.NORMAL:
            case Typeface.BOLD:
            case Typeface.ITALIC:
            case Typeface.BOLD_ITALIC:
                break;
            default:
                return;
        }

        if (start >= end) {
            return;
        }

        Editable editable = getEditableText();
        StyleSpan[] spans = editable.getSpans(start, end, StyleSpan.class);
        List<Part> list = new ArrayList<>();

        for (StyleSpan span : spans) {
            if (span.getStyle() == style) {
                list.add(new Part(editable.getSpanStart(span), editable.getSpanEnd(span)));
                editable.removeSpan(span);
            }
        }

        for (Part part : list) {
            if (part.isValid()) {
                if (part.getStart() < start) {
                    styleValid(style, part.getStart(), start);
                }

                if (part.getEnd() > end) {
                    styleValid(style, end, part.getEnd());
                }
            }
        }
    }

    protected boolean containStyle(int style, int start, int end) {
        switch (style) {
            case Typeface.NORMAL:
            case Typeface.BOLD:
            case Typeface.ITALIC:
            case Typeface.BOLD_ITALIC:
                break;
            default:
                return false;
        }

        if (start > end) {
            return false;
        }

        if (start == end) {
            if (start - 1 < 0 || start + 1 > getEditableText().length()) {
                return false;
            }

            StyleSpan[] before = getEditableText().getSpans(start - 1, start, StyleSpan.class);
            StyleSpan[] after = getEditableText().getSpans(start, start + 1, StyleSpan.class);

            return before.length > 0
                    && after.length > 0
                    && before[0].getStyle() == style
                    && after[0].getStyle() == style;
        }

        StringBuilder builder = new StringBuilder();

        // Make sure no duplicate characters be added
        for (int i = start; i < end; i++) {
            StyleSpan[] spans = getEditableText().getSpans(i, i + 1, StyleSpan.class);
            for (StyleSpan span : spans) {
                if (span.getStyle() == style) {
                    builder.append(getEditableText().subSequence(i, i + 1).toString());
                    break;
                }
            }
        }

        return getEditableText().subSequence(start, end).toString().equals(builder.toString());
    }

    // Image ===============================================================================

    public void image(final Uri uri, final int maxWidth) {
        glideRequests.asBitmap()
                .load(uri)
                .thumbnail(0.1f)
                .centerCrop()
                .error(R.drawable.ic_pic_fill)
                .placeholder(R.drawable.ic_pic_fill)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(
                            Bitmap resource, Transition<? super Bitmap> transition) {
                        Bitmap bitmap = BitmapUtil.zoomBitmapToFixWidth(resource, maxWidth);
                        image(uri, bitmap);
                    }
                });
    }

    public void image(final Uri uri) {
        int width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        image(uri, width);
    }

    public void image(Uri uri, Bitmap pic) {
        SpannableString ss = new SpannableString(" \n");
        MyImgSpan span = new MyImgSpan(getContext(), pic, uri);
        ss.setSpan(span, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        int start = getSelectionStart();
        getEditableText().insert(start, ss);// 设置ss要添加的位置
    }

    // UnderlineSpan ===============================================================================

    public void underline(boolean valid) {
        if (valid) {
            underlineValid(getSelectionStart(), getSelectionEnd());
        } else {
            underlineInvalid(getSelectionStart(), getSelectionEnd());
        }
    }

    protected void underlineValid(int start, int end) {
        if (start >= end) {
            return;
        }

        UnderlineSpan span = new UnderlineSpan();
        getEditableText().setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    protected void underlineInvalid(int start, int end) {
        if (start >= end) {
            return;
        }

        Editable editable = getEditableText();
        UnderlineSpan[] spans = editable.getSpans(start, end, UnderlineSpan.class);
        List<Part> list = new ArrayList<>();

        for (UnderlineSpan span : spans) {
            list.add(new Part(editable.getSpanStart(span), editable.getSpanEnd(span)));
            editable.removeSpan(span);
        }

        for (Part part : list) {
            if (part.isValid()) {
                if (part.getStart() < start) {
                    underlineValid(part.getStart(), start);
                }

                if (part.getEnd() > end) {
                    underlineValid(end, part.getEnd());
                }
            }
        }
    }

    protected boolean containUnderline(int start, int end) {
        if (start > end) {
            return false;
        }

        if (start == end) {
            if (start - 1 < 0 || start + 1 > getEditableText().length()) {
                return false;
            } else {
                UnderlineSpan[] before = getEditableText()
                        .getSpans(start - 1, start, UnderlineSpan.class);
                UnderlineSpan[] after = getEditableText()
                        .getSpans(start, start + 1, UnderlineSpan.class);
                return before.length > 0 && after.length > 0;
            }
        } else {
            StringBuilder builder = new StringBuilder();

            for (int i = start; i < end; i++) {
                if (getEditableText().getSpans(i, i + 1, UnderlineSpan.class).length > 0) {
                    builder.append(getEditableText().subSequence(i, i + 1).toString());
                }
            }

            return getEditableText().subSequence(start, end).toString().equals(builder.toString());
        }
    }

    // StrikethroughSpan ===========================================================================

    public void strikethrough(boolean valid) {
        if (valid) {
            strikethroughValid(getSelectionStart(), getSelectionEnd());
        } else {
            strikethroughInvalid(getSelectionStart(), getSelectionEnd());
        }
    }

    protected void strikethroughValid(int start, int end) {
        if (start >= end) {
            return;
        }

        StrikethroughSpan span = new StrikethroughSpan();
        getEditableText().setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    protected void strikethroughInvalid(int start, int end) {
        if (start >= end) {
            return;
        }

        Editable editable = getEditableText();
        StrikethroughSpan[] spans = editable.getSpans(start, end, StrikethroughSpan.class);
        List<Part> list = new ArrayList<>();

        for (StrikethroughSpan span : spans) {
            Part part = new Part(editable.getSpanStart(span), editable.getSpanEnd(span));
            list.add(part);
            editable.removeSpan(span);
        }

        for (Part part : list) {
            if (part.isValid()) {
                if (part.getStart() < start) {
                    strikethroughValid(part.getStart(), start);
                }

                if (part.getEnd() > end) {
                    strikethroughValid(end, part.getEnd());
                }
            }
        }
    }

    protected boolean containStrikethrough(int start, int end) {
        if (start > end) {
            return false;
        }

        if (start == end) {
            if (start - 1 < 0 || start + 1 > getEditableText().length()) {
                return false;
            } else {
                StrikethroughSpan[] before = getEditableText()
                        .getSpans(start - 1, start, StrikethroughSpan.class);
                StrikethroughSpan[] after = getEditableText()
                        .getSpans(start, start + 1, StrikethroughSpan.class);
                return before.length > 0 && after.length > 0;
            }
        } else {
            StringBuilder builder = new StringBuilder();

            for (int i = start; i < end; i++) {
                if (getEditableText().getSpans(i, i + 1, StrikethroughSpan.class).length > 0) {
                    builder.append(getEditableText().subSequence(i, i + 1).toString());
                }
            }

            return getEditableText().subSequence(start, end).toString().equals(builder.toString());
        }
    }

    // BulletSpan ==================================================================================

    public void bullet(boolean valid) {
        if (valid) {
            bulletValid();
        } else {
            bulletInvalid();
        }
    }

    protected void bulletValid() {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");

        for (int i = 0; i < lines.length; i++) {
            if (containBullet(i)) {
                continue;
            }

            int lineStart = 0;
            for (int j = 0; j < i; j++) {
                lineStart = lineStart + lines[j].length() + 1; // \n
            }

            int lineEnd = lineStart + lines[i].length();
            if (lineStart >= lineEnd) {
                continue;
            }

            // Find selection area inside
            int bulletStart = 0;
            int bulletEnd = 0;
            if (lineStart <= getSelectionStart() && getSelectionEnd() <= lineEnd) {
                bulletStart = lineStart;
                bulletEnd = lineEnd;
            } else if (getSelectionStart() <= lineStart && lineEnd <= getSelectionEnd()) {
                bulletStart = lineStart;
                bulletEnd = lineEnd;
            }

            if (bulletStart < bulletEnd) {
                MyBulletSpan span = new MyBulletSpan(bulletColor, bulletRadius, bulletGapWidth);
                getEditableText().setSpan(
                        span, bulletStart, bulletEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    protected void bulletInvalid() {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");

        for (int i = 0; i < lines.length; i++) {
            if (!containBullet(i)) {
                continue;
            }

            int lineStart = 0;
            for (int j = 0; j < i; j++) {
                lineStart = lineStart + lines[j].length() + 1;
            }

            int lineEnd = lineStart + lines[i].length();
            if (lineStart >= lineEnd) {
                continue;
            }

            int bulletStart = 0;
            int bulletEnd = 0;
            if (lineStart <= getSelectionStart() && getSelectionEnd() <= lineEnd) {
                bulletStart = lineStart;
                bulletEnd = lineEnd;
            } else if (getSelectionStart() <= lineStart && lineEnd <= getSelectionEnd()) {
                bulletStart = lineStart;
                bulletEnd = lineEnd;
            }

            if (bulletStart < bulletEnd) {
                BulletSpan[] spans = getEditableText().getSpans(
                        bulletStart, bulletEnd, BulletSpan.class);
                for (BulletSpan span : spans) {
                    getEditableText().removeSpan(span);
                }
            }
        }
    }

    protected boolean containBullet() {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");
        List<Integer> list = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            int lineStart = 0;
            for (int j = 0; j < i; j++) {
                lineStart = lineStart + lines[j].length() + 1;
            }

            int lineEnd = lineStart + lines[i].length();
            if (lineStart >= lineEnd) {
                continue;
            }

            if (lineStart <= getSelectionStart() && getSelectionEnd() <= lineEnd) {
                list.add(i);
            } else if (getSelectionStart() <= lineStart && lineEnd <= getSelectionEnd()) {
                list.add(i);
            }
        }

        for (Integer i : list) {
            if (!containBullet(i)) {
                return false;
            }
        }

        return true;
    }

    protected boolean containBullet(int index) {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");
        if (index < 0 || index >= lines.length) {
            return false;
        }

        int start = 0;
        for (int i = 0; i < index; i++) {
            start = start + lines[i].length() + 1;
        }

        int end = start + lines[index].length();
        if (start >= end) {
            return false;
        }

        BulletSpan[] spans = getEditableText().getSpans(start, end, BulletSpan.class);
        return spans.length > 0;
    }

    // QuoteSpan ===================================================================================

    public void quote(boolean valid) {
        if (valid) {
            quoteValid();
        } else {
            quoteInvalid();
        }
    }

    protected void quoteValid() {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");

        for (int i = 0; i < lines.length; i++) {
            if (containQuote(i)) {
                continue;
            }

            int lineStart = 0;
            for (int j = 0; j < i; j++) {
                lineStart = lineStart + lines[j].length() + 1; // \n
            }

            int lineEnd = lineStart + lines[i].length();
            if (lineStart >= lineEnd) {
                continue;
            }

            int quoteStart = 0;
            int quoteEnd = 0;
            if (lineStart <= getSelectionStart() && getSelectionEnd() <= lineEnd) {
                quoteStart = lineStart;
                quoteEnd = lineEnd;
            } else if (getSelectionStart() <= lineStart && lineEnd <= getSelectionEnd()) {
                quoteStart = lineStart;
                quoteEnd = lineEnd;
            }

            if (quoteStart < quoteEnd) {
                MyQuoteSpan span = new MyQuoteSpan(quoteColor, quoteStripeWidth, quoteGapWidth);
                getEditableText().setSpan(
                        span, quoteStart, quoteEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    protected void quoteInvalid() {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");

        for (int i = 0; i < lines.length; i++) {
            if (!containQuote(i)) {
                continue;
            }

            int lineStart = 0;
            for (int j = 0; j < i; j++) {
                lineStart = lineStart + lines[j].length() + 1;
            }

            int lineEnd = lineStart + lines[i].length();
            if (lineStart >= lineEnd) {
                continue;
            }

            int quoteStart = 0;
            int quoteEnd = 0;
            if (lineStart <= getSelectionStart() && getSelectionEnd() <= lineEnd) {
                quoteStart = lineStart;
                quoteEnd = lineEnd;
            } else if (getSelectionStart() <= lineStart && lineEnd <= getSelectionEnd()) {
                quoteStart = lineStart;
                quoteEnd = lineEnd;
            }

            if (quoteStart < quoteEnd) {
                QuoteSpan[] spans = getEditableText()
                        .getSpans(quoteStart, quoteEnd, QuoteSpan.class);
                for (QuoteSpan span : spans) {
                    getEditableText().removeSpan(span);
                }
            }
        }
    }

    protected boolean containQuote() {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");
        List<Integer> list = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            int lineStart = 0;
            for (int j = 0; j < i; j++) {
                lineStart = lineStart + lines[j].length() + 1;
            }

            int lineEnd = lineStart + lines[i].length();
            if (lineStart >= lineEnd) {
                continue;
            }

            if (lineStart <= getSelectionStart() && getSelectionEnd() <= lineEnd) {
                list.add(i);
            } else if (getSelectionStart() <= lineStart && lineEnd <= getSelectionEnd()) {
                list.add(i);
            }
        }

        for (Integer i : list) {
            if (!containQuote(i)) {
                return false;
            }
        }

        return true;
    }

    protected boolean containQuote(int index) {
        String[] lines = TextUtils.split(getEditableText().toString(), "\n");
        if (index < 0 || index >= lines.length) {
            return false;
        }

        int start = 0;
        for (int i = 0; i < index; i++) {
            start = start + lines[i].length() + 1;
        }

        int end = start + lines[index].length();
        if (start >= end) {
            return false;
        }

        QuoteSpan[] spans = getEditableText().getSpans(start, end, QuoteSpan.class);
        return spans.length > 0;
    }

    // URLSpan =====================================================================================

    public void link(String link) {
        link(link, getSelectionStart(), getSelectionEnd());
    }

    // When KnifeText lose focus, use this method
    public void link(String link, int start, int end) {
        if (link != null && !TextUtils.isEmpty(link.trim())) {
            linkValid(link, start, end);
        } else {
            linkInvalid(start, end);
        }
    }

    protected void linkValid(String link, int start, int end) {
        if (start >= end) {
            return;
        }

        linkInvalid(start, end);
        MyURLSpan span = new MyURLSpan(link, linkColor, linkUnderline);
        getEditableText().setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // Remove all span in selection, not like the boldInvalid()
    protected void linkInvalid(int start, int end) {
        if (start >= end) {
            return;
        }

        URLSpan[] spans = getEditableText().getSpans(start, end, URLSpan.class);
        for (URLSpan span : spans) {
            getEditableText().removeSpan(span);
        }
    }

    protected boolean containLink(int start, int end) {
        if (start > end) {
            return false;
        }

        if (start == end) {
            if (start - 1 < 0 || start + 1 > getEditableText().length()) {
                return false;
            } else {
                URLSpan[] before = getEditableText().getSpans(start - 1, start, URLSpan.class);
                URLSpan[] after = getEditableText().getSpans(start, start + 1, URLSpan.class);
                return before.length > 0 && after.length > 0;
            }
        } else {
            StringBuilder builder = new StringBuilder();

            for (int i = start; i < end; i++) {
                if (getEditableText().getSpans(i, i + 1, URLSpan.class).length > 0) {
                    builder.append(getEditableText().subSequence(i, i + 1).toString());
                }
            }

            return getEditableText().subSequence(start, end).toString().equals(builder.toString());
        }
    }

    // Redo/Undo ===================================================================================

    @Override
    public void beforeTextChanged(CharSequence text, int start, int count, int after) {
        if (!historyEnable || historyWorking) {
            return;
        }

        inputBefore = new SpannableStringBuilder(text);
    }

    @Override
    public void onTextChanged(CharSequence text, int start, int before, int count) {
        // DO NOTHING HERE
    }

    @Override
    public void afterTextChanged(Editable text) {
        if (!historyEnable || historyWorking) {
            return;
        }

        inputLast = new SpannableStringBuilder(text);
        if (text != null && text.toString().equals(inputBefore.toString())) {
            return;
        }

        if (historyList.size() >= historySize) {
            historyList.remove(0);
        }

        historyList.add(inputBefore);
        historyCursor = historyList.size();
    }

    public void redo() {
        if (!redoValid()) {
            return;
        }

        historyWorking = true;

        if (historyCursor >= historyList.size() - 1) {
            historyCursor = historyList.size();
            setText(inputLast);
        } else {
            historyCursor++;
            setText(historyList.get(historyCursor));
        }

        setSelection(getEditableText().length());
        historyWorking = false;
    }

    public void undo() {
        if (!undoValid()) {
            return;
        }

        historyWorking = true;

        historyCursor--;
        setText(historyList.get(historyCursor));
        setSelection(getEditableText().length());

        historyWorking = false;
    }

    public boolean redoValid() {
        if (!historyEnable || historySize <= 0 || historyList.size() <= 0 || historyWorking) {
            return false;
        }

        return historyCursor < historyList.size() - 1
                || historyCursor >= historyList.size() - 1
                && inputLast != null;
    }

    public boolean undoValid() {
        if (!historyEnable || historySize <= 0 || historyWorking) {
            return false;
        }

        if (historyList.size() <= 0 || historyCursor <= 0) {
            return false;
        }

        return true;
    }

    public void clearHistory() {
        if (historyList != null) {
            historyList.clear();
        }
    }

    // Helper ======================================================================================

    public boolean contains(int format) {
        switch (format) {
            case FORMAT_BOLD:
                return containStyle(Typeface.BOLD, getSelectionStart(), getSelectionEnd());
            case FORMAT_ITALIC:
                return containStyle(Typeface.ITALIC, getSelectionStart(), getSelectionEnd());
            case FORMAT_UNDERLINED:
                return containUnderline(getSelectionStart(), getSelectionEnd());
            case FORMAT_STRIKETHROUGH:
                return containStrikethrough(getSelectionStart(), getSelectionEnd());
            case FORMAT_BULLET:
                return containBullet();
            case FORMAT_QUOTE:
                return containQuote();
            case FORMAT_LINK:
                return containLink(getSelectionStart(), getSelectionEnd());
            default:
                return false;
        }
    }

    public void clearFormats() {
        setText(getEditableText().toString());
        setSelection(getEditableText().length());
    }

    public void hideSoftInput() {
        clearFocus();
        InputMethodManager imm =
                (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    public void showSoftInput() {
        requestFocus();
        InputMethodManager imm =
                (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void fromHtml(String source) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(HtmlParser.fromHtml(source, new GlideImageGeter(this, glideRequests)));
        switchToKnifeStyle(builder, 0, builder.length());
        setText(builder);
    }

    public String toMarkdown() {
        return MarkdownParser.toMarkdown(getEditableText());
    }

    public String toHtml() {
        return HtmlParser.toHtml(getEditableText());
    }

    protected void switchToKnifeStyle(Editable editable, int start, int end) {
        BulletSpan[] bulletSpans = editable.getSpans(start, end, BulletSpan.class);
        for (BulletSpan span : bulletSpans) {
            int spanStart = editable.getSpanStart(span);
            int spanEnd = editable.getSpanEnd(span);
            spanEnd = 0 < spanEnd && spanEnd < editable.length() && editable.charAt(spanEnd) == '\n' ? spanEnd - 1 : spanEnd;
            editable.removeSpan(span);
            MyBulletSpan bulletSpan = new MyBulletSpan(bulletColor, bulletRadius, bulletGapWidth);
            editable.setSpan(bulletSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        QuoteSpan[] quoteSpans = editable.getSpans(start, end, QuoteSpan.class);
        for (QuoteSpan span : quoteSpans) {
            int spanStart = editable.getSpanStart(span);
            int spanEnd = editable.getSpanEnd(span);
            spanEnd = 0 < spanEnd && spanEnd < editable.length() && editable.charAt(spanEnd) == '\n' ? spanEnd - 1 : spanEnd;
            editable.removeSpan(span);
            MyQuoteSpan quoteSpan = new MyQuoteSpan(quoteColor, quoteStripeWidth, quoteGapWidth);
            editable.setSpan(quoteSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        URLSpan[] urlSpans = editable.getSpans(start, end, URLSpan.class);
        for (URLSpan span : urlSpans) {
            int spanStart = editable.getSpanStart(span);
            int spanEnd = editable.getSpanEnd(span);
            editable.removeSpan(span);
            MyURLSpan urlSpan = new MyURLSpan(span.getURL(), linkColor, linkUnderline);
            editable.setSpan(urlSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

    }
}
