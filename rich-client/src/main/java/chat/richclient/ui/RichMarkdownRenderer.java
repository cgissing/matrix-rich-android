package chat.richclient.ui;

import android.content.Context;
import android.widget.TextView;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.ext.latex.JLatexMathTheme;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RichMarkdownRenderer {
    private static final Pattern INLINE_MATH_PATTERN = Pattern.compile("(?<!\\\\)(?<!\\$)\\$([^\\n\\$]+?)(?<!\\\\)\\$(?!\\$)");

    private final Markwon markwon;

    public RichMarkdownRenderer(Context context) {
        markwon = Markwon.builder(context)
                .usePlugin(HtmlPlugin.create())
                .usePlugin(TablePlugin.create(context))
                .usePlugin(JLatexMathPlugin.create(44F, builder -> {
                    builder.inlinesEnabled(true);
                    builder.theme().inlinePadding(JLatexMathTheme.Padding.symmetric(24, 8));
                }))
                .usePlugin(MarkwonInlineParserPlugin.create())
                .build();
    }

    static String preprocessMarkdown(String markdown) {
        String source = markdown == null ? "" : markdown;
        Matcher matcher = INLINE_MATH_PATTERN.matcher(source);
        StringBuffer buffer = new StringBuffer(source.length() + 16);
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("$$" + matcher.group(1) + "$$"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public void setMarkdown(TextView view, String markdown) {
        try {
            markwon.setMarkdown(view, preprocessMarkdown(markdown));
        } catch (Throwable failure) {
            view.setText(markdown == null ? "" : markdown);
        }
    }
}
