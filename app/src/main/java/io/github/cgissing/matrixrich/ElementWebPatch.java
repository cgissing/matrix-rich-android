package io.github.cgissing.matrixrich;

public final class ElementWebPatch {
    private ElementWebPatch() {
    }

    public static String mobilePatchScript() {
        String css = ""
                + "html,body,#matrixchat,.mx_MatrixChat{width:100%!important;min-width:0!important;overflow:hidden!important;}"
                + "body{overscroll-behavior:none;background:#f7f9f8!important;}"
                + ".mx_MatrixChat{height:100vh!important;max-width:100vw!important;}"
                + ".mx_RoomView,.mx_RoomView_body,.mx_MainSplit,.mx_RoomView_timeline{min-width:0!important;max-width:100vw!important;}"
                + ".mx_LeftPanel{max-width:86vw!important;}"
                + ".mx_RightPanel,.mx_ResizeHandle{max-width:100vw!important;}"
                + ".mx_MessageComposer{padding-bottom:max(8px,env(safe-area-inset-bottom))!important;}"
                + ".mx_EventTile_content{overflow-wrap:anywhere!important;}"
                + ".mx_EventTile_body table{display:block!important;max-width:100%!important;overflow-x:auto!important;white-space:nowrap!important;}"
                + ".katex-display{overflow-x:auto!important;overflow-y:hidden!important;padding-bottom:4px!important;}"
                + "@media(max-width:720px){"
                + ".mx_LeftPanel{min-width:min(320px,86vw)!important;}"
                + ".mx_RoomHeader{min-width:0!important;}"
                + ".mx_RoomView_MessageList{padding-left:6px!important;padding-right:6px!important;}"
                + ".mx_EventTile{max-width:100%!important;}"
                + ".mx_MessageComposer_wrapper{margin-left:6px!important;margin-right:6px!important;}"
                + "}";
        return "(function(){"
                + "try{"
                + "var viewport=document.getElementById('matrix-rich-viewport')||document.createElement('meta');"
                + "viewport.id='matrix-rich-viewport';"
                + "viewport.name='viewport';"
                + "viewport.content='width=device-width,initial-scale=1,viewport-fit=cover';"
                + "if(!viewport.parentNode){document.head.appendChild(viewport);}"
                + "var style=document.getElementById('matrix-rich-mobile-patch')||document.createElement('style');"
                + "style.id='matrix-rich-mobile-patch';"
                + "style.textContent=" + quoteJs(css) + ";"
                + "if(!style.parentNode){document.head.appendChild(style);}"
                + "try{localStorage.setItem('mx_labs_feature_latex_maths','true');localStorage.setItem('feature_latex_maths','true');}catch(_){ }"
                + "}catch(error){console.warn('Matrix Rich mobile patch failed',error);}"
                + "})();";
    }

    private static String quoteJs(String value) {
        StringBuilder builder = new StringBuilder("'");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\' || ch == '\'') {
                builder.append('\\');
            }
            builder.append(ch);
        }
        builder.append('\'');
        return builder.toString();
    }
}
