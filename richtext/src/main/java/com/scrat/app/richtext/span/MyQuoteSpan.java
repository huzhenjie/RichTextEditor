package com.scrat.app.richtext.span;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.text.Layout;
import android.text.style.QuoteSpan;

public class MyQuoteSpan extends QuoteSpan {
    private static final int DEFAULT_STRIPE_WIDTH = 2;
    private static final int DEFAULT_GAP_WIDTH = 2;
    private static final int DEFAULT_COLOR = 0xff0000ff;

    private int quoteColor;
    private int quoteStripeWidth;
    private int quoteGapWidth;

    public MyQuoteSpan(int quoteColor, int quoteStripeWidth, int quoteGapWidth) {
        this.quoteColor = quoteColor != 0 ? quoteColor : DEFAULT_COLOR;
        this.quoteStripeWidth = quoteStripeWidth != 0 ? quoteStripeWidth : DEFAULT_STRIPE_WIDTH;
        this.quoteGapWidth = quoteGapWidth != 0 ? quoteGapWidth : DEFAULT_GAP_WIDTH;
    }

    public MyQuoteSpan(Parcel src) {
        super(src);
        this.quoteColor = src.readInt();
        this.quoteStripeWidth = src.readInt();
        this.quoteGapWidth = src.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(quoteColor);
        dest.writeInt(quoteStripeWidth);
        dest.writeInt(quoteGapWidth);
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return quoteStripeWidth + quoteGapWidth;
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir,
                                  int top, int baseline, int bottom,
                                  CharSequence text, int start, int end,
                                  boolean first, Layout layout) {
        Paint.Style style = p.getStyle();
        int color = p.getColor();

        p.setStyle(Paint.Style.FILL);
        p.setColor(quoteColor);
        c.drawRect(x, top, x + dir * quoteGapWidth, bottom, p);

        p.setStyle(style);
        p.setColor(color);
    }
}
