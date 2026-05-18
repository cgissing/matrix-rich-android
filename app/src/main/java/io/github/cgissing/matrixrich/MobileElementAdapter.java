package io.github.cgissing.matrixrich;

public final class MobileElementAdapter {
    private static final String CSS =
            "html.matrix-rich-android-shell,html.matrix-rich-android-shell body{min-width:0!important;max-width:100vw!important;overflow-x:hidden!important;}" +
            "html.matrix-rich-android-shell .mx_MatrixChat{min-width:0!important;}" +
            "html.matrix-rich-android-shell .mx_RoomView{min-width:0!important;max-width:100vw!important;}" +
            "html.matrix-rich-android-shell .mx_MainSplit{min-width:0!important;}" +
            "html.matrix-rich-android-shell .mx_RoomView_body{min-width:0!important;}" +
            "html.matrix-rich-android-shell .mx_EventTile{max-width:100vw!important;}" +
            "html.matrix-rich-android-shell .mx_EventTile_line{max-width:100%!important;}" +
            "html.matrix-rich-android-shell .mx_EventTile_body{max-width:100%!important;overflow-wrap:anywhere!important;}" +
            "html.matrix-rich-android-shell table{display:block!important;max-width:100%!important;overflow-x:auto!important;border-collapse:collapse!important;}" +
            "html.matrix-rich-android-shell th,html.matrix-rich-android-shell td{padding:6px 8px!important;border:1px solid rgba(0,0,0,.18)!important;white-space:nowrap!important;}" +
            "html.matrix-rich-android-shell pre{max-width:100%!important;overflow-x:auto!important;}" +
            "html.matrix-rich-android-shell code{white-space:pre-wrap!important;overflow-wrap:anywhere!important;}" +
            "html.matrix-rich-android-shell .katex-display{max-width:100%!important;overflow-x:auto!important;overflow-y:hidden!important;padding:.35rem 0!important;}" +
            "html.matrix-rich-android-shell .katex{font-size:1.03em!important;}" +
            "html.matrix-rich-android-shell .mx_RightPanel{max-width:100vw!important;}" +
            "@media(max-width:720px){" +
            "html.matrix-rich-android-shell .mx_LeftPanel{max-width:86vw!important;}" +
            "html.matrix-rich-android-shell .mx_RoomHeader{min-width:0!important;}" +
            "html.matrix-rich-android-shell .mx_MessageComposer{min-width:0!important;}" +
            "html.matrix-rich-android-shell .mx_MessageComposer_wrapper{min-width:0!important;}" +
            "}";

    private MobileElementAdapter() {
    }

    public static String injectionScript() {
        return "(function(){try{"
                + "localStorage.setItem('mx_labs_feature_feature_latex_maths','true');"
                + "document.cookie='element_mobile_redirect_to_guide=false; path=/; max-age=31536000; SameSite=Lax';"
                + "var meta=document.querySelector('meta[name=\"viewport\"]');"
                + "if(!meta){meta=document.createElement('meta');meta.name='viewport';document.head.appendChild(meta);}"
                + "meta.setAttribute('content','width=device-width, initial-scale=1, viewport-fit=cover');"
                + "document.documentElement.classList.add('matrix-rich-android-shell');"
                + "var style=document.getElementById('matrix-rich-mobile-css');"
                + "if(!style){style=document.createElement('style');style.id='matrix-rich-mobile-css';style.textContent=" + quoted(CSS) + ";document.head.appendChild(style);}"
                + "}catch(e){console.warn('Matrix Rich mobile adapter failed',e);}})();";
    }

    private static String quoted(String value) {
        return "'" + value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n") + "'";
    }
}
