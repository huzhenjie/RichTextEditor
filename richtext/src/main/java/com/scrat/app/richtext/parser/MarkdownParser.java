package com.scrat.app.richtext.parser;

import android.graphics.Typeface;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.text.style.CharacterStyle;
import android.text.style.ImageSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;

/**
 * Created by yixuanxuan on 16/8/8.
 */
public class MarkdownParser {

    public static String toMarkdown(Spanned text) {
        StringBuilder out = new StringBuilder();
        withinHtml(out, text);
        return out.toString();
    }

    private static void withinHtml(StringBuilder out, Spanned text) {
        int next;

        for (int i = 0; i < text.length(); i = next) {
            next = text.nextSpanTransition(i, text.length(), ParagraphStyle.class);

            ParagraphStyle[] styles = text.getSpans(i, next, ParagraphStyle.class);
            if (styles.length == 2) {
                if (styles[0] instanceof BulletSpan && styles[1] instanceof QuoteSpan) {
                    // Let a <br> follow the BulletSpan or QuoteSpan end, so next++
                    withinBulletThenQuote(out, text, i, next++);
                } else if (styles[0] instanceof QuoteSpan && styles[1] instanceof BulletSpan) {
                    withinQuoteThenBullet(out, text, i, next++);
                } else {
                    withinContent(out, text, i, next);
                }
            } else if (styles.length == 1) {
                if (styles[0] instanceof BulletSpan) {
                    withinBullet(out, text, i, next++);
                } else if (styles[0] instanceof QuoteSpan) {
                    withinQuote(out, text, i, next++);
                } else {
                    withinContent(out, text, i, next);
                }
            } else {
                withinContent(out, text, i, next);
            }
        }
    }

    /**
     * 加粗
     */
    private static void withinBulletThenQuote(StringBuilder out, Spanned text, int start, int end) {
        out.append("**");
        withinQuote(out, text, start, end);
        out.append("**");
    }

    /**
     * 引用
     */
    private static void withinQuoteThenBullet(StringBuilder out, Spanned text, int start, int end) {
        out.append("> ");
        withinBullet(out, text, start, end);
    }

    /**
     * 序号
     */
    private static void withinBullet(StringBuilder out, Spanned text, int start, int end) {

        int next;

        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, BulletSpan.class);

            BulletSpan[] spans = text.getSpans(i, next, BulletSpan.class);
            for (BulletSpan span : spans) {
                out.append("* ");
            }

            withinContent(out, text, i, next);
        }

    }

    /**
     * 引用
     */
    private static void withinQuote(StringBuilder out, Spanned text, int start, int end) {
        int next;

        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, QuoteSpan.class);

            QuoteSpan[] quotes = text.getSpans(i, next, QuoteSpan.class);
            for (QuoteSpan quote : quotes) {
                out.append("> ");
            }

            withinContent(out, text, i, next);
        }
    }

    private static void withinContent(StringBuilder out, Spanned text, int start, int end) {
        int next;

        for (int i = start; i < end; i = next) {
            next = TextUtils.indexOf(text, '\n', i, end);
            if (next < 0) {
                next = end;
            }

            int nl = 0;
            while (next < end && text.charAt(next) == '\n') {
                next++;
                nl++;
            }

            withinParagraph(out, text, i, next - nl, nl);
        }
    }

    // Copy from https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/text/Html.java,
    // remove some tag because we don't need them in Knife.
    private static void withinParagraph(StringBuilder out, Spanned text, int start, int end, int nl) {
        int next;

        for (int i = start; i < end; i = next) {
            next = text.nextSpanTransition(i, end, CharacterStyle.class);

            CharacterStyle[] spans = text.getSpans(i, next, CharacterStyle.class);
            for (CharacterStyle span : spans) {
                if (span instanceof StyleSpan) {
                    int style = ((StyleSpan) span).getStyle();

                    // 加粗
                    if ((style & Typeface.BOLD) != 0) {
                        out.append("**");
                    }

                    // 斜体
                    if ((style & Typeface.ITALIC) != 0) {
                        out.append("*");
                    }
                }

                if (span instanceof UnderlineSpan) {
                    // 下划线
                    out.append("_");
                }

                // Use standard strikethrough tag <del> rather than <s> or <strike>
                // 删除线
                if (span instanceof StrikethroughSpan) {
                    out.append("~~");
                }

                if (span instanceof URLSpan) {
                    out.append("<a href=\"");
                    out.append(((URLSpan) span).getURL());
                    out.append("\">");
                }

                if (span instanceof ImageSpan) {
                    out.append("<img src=\"");
                    out.append(((ImageSpan) span).getSource());
                    out.append("\">");

                    // Don't output the dummy character underlying the image.
                    i = next;
                }
            }

            withinStyle(out, text, i, next);
            for (int j = spans.length - 1; j >= 0; j--) {
                if (spans[j] instanceof URLSpan) {
                    out.append("</a>");
                }

                if (spans[j] instanceof StrikethroughSpan) {
                    // 删除线
                    out.append("～～");
                }

                if (spans[j] instanceof UnderlineSpan) {
                    // 下划线
                    out.append("_");
                }

                if (spans[j] instanceof StyleSpan) {
                    int style = ((StyleSpan) spans[j]).getStyle();

                    if ((style & Typeface.BOLD) != 0) {
                        // 加粗
                        out.append("**");
                    }

                    if ((style & Typeface.ITALIC) != 0) {
                        // 斜体
                        out.append("*");
                    }
                }
            }
        }

        for (int i = 0; i < nl; i++) {
            // 换行
            out.append("  \n");
        }
    }

    private static void withinStyle(StringBuilder out, CharSequence text, int start, int end) {
        out.append(text.subSequence(start, end));
    }

}
