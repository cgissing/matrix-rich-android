package io.github.cgissing.matrixrich;

import org.json.JSONException;
import org.json.JSONObject;

public final class ElementWebConfigJson {
    private ElementWebConfigJson() {
    }

    static String withShellDefaults(String rawConfig) {
        try {
            JSONObject root = new JSONObject(rawConfig == null || rawConfig.trim().isEmpty() ? "{}" : rawConfig);
            JSONObject features = root.optJSONObject("features");
            if (features == null) {
                features = new JSONObject();
                root.put("features", features);
            }
            features.put("feature_latex_maths", true);
            root.put("show_labs_settings", true);
            return root.toString();
        } catch (JSONException ignored) {
            return rawConfig == null ? "{}" : rawConfig;
        }
    }
}
