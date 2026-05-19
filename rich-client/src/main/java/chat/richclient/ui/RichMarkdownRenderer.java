package chat.richclient.ui;

import android.content.Context;
import android.widget.TextView;
import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.ext.latex.JLatexMathTheme;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;

public final class RichMarkdownRenderer {
    private final Markwon markwon;

    public RichMarkdownRenderer(Context context) {
        markwon = Markwon.builder(context)
                .usePlugin(TablePlugin.create(context))
                .usePlugin(JLatexMathPlugin.create(44F, builder -> {
                    builder.inlinesEnabled(true);
                    builder.theme().inlinePadding(JLatexMathTheme.Padding.symmetric(24, 8));
                }))
                .usePlugin(MarkwonInlineParserPlugin.create())
                .build();
    }

    public void setMarkdown(TextView view, String markdown) {
        markwon.setMarkdown(view, markdown);
    }
}
