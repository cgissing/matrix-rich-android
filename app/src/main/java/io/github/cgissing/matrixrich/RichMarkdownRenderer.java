package io.github.cgissing.matrixrich;

import android.content.Context;
import android.widget.TextView;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;

public final class RichMarkdownRenderer {
    private RichMarkdownRenderer() {
    }

    public static Markwon create(Context context, TextView target) {
        return Markwon.builder(context)
                .usePlugin(MarkwonInlineParserPlugin.create())
                .usePlugin(TablePlugin.create(context))
                .usePlugin(JLatexMathPlugin.create(target.getTextSize(), new JLatexMathPlugin.BuilderConfigure() {
                    @Override
                    public void configureBuilder(JLatexMathPlugin.Builder builder) {
                        builder.inlinesEnabled(true);
                    }
                }))
                .build();
    }

    public static void render(Markwon markwon, TextView target, String markdown) {
        markwon.setMarkdown(target, normalizeMathDelimiters(markdown));
    }

    static String normalizeMathDelimiters(String markdown) {
        if (markdown == null || markdown.indexOf('$') < 0) {
            return markdown == null ? "" : markdown;
        }
        StringBuilder out = new StringBuilder(markdown.length() + 8);
        int i = 0;
        while (i < markdown.length()) {
            char ch = markdown.charAt(i);
            if (ch != '$') {
                out.append(ch);
                i++;
                continue;
            }
            if (i + 1 < markdown.length() && markdown.charAt(i + 1) == '$') {
                out.append("$$");
                i += 2;
                continue;
            }
            int close = findClosingDollar(markdown, i + 1);
            if (close < 0) {
                out.append(ch);
                i++;
                continue;
            }
            String body = markdown.substring(i + 1, close).trim();
            if (body.isEmpty() || body.contains("\n")) {
                out.append(markdown, i, close + 1);
            } else {
                out.append("$$").append(body).append("$$");
            }
            i = close + 1;
        }
        return out.toString();
    }

    private static int findClosingDollar(String markdown, int from) {
        for (int i = from; i < markdown.length(); i++) {
            if (markdown.charAt(i) == '$' && (i == 0 || markdown.charAt(i - 1) != '\\')) {
                return i;
            }
        }
        return -1;
    }
}
