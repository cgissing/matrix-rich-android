package io.github.cgissing.matrixrich;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ElementWebConfigJsonTest {
    @Test
    public void enablesLatexMathsFeatureInElementConfig() throws Exception {
        JSONObject config = new JSONObject(ElementWebConfigJson.withShellDefaults("{\"features\":{\"feature_pinning\":true}}"));

        assertTrue(config.getJSONObject("features").getBoolean("feature_pinning"));
        assertTrue(config.getJSONObject("features").getBoolean("feature_latex_maths"));
        assertTrue(config.getBoolean("show_labs_settings"));
    }

    @Test
    public void createsFeatureObjectWhenMissing() throws Exception {
        JSONObject config = new JSONObject(ElementWebConfigJson.withShellDefaults("{}"));

        assertTrue(config.getJSONObject("features").getBoolean("feature_latex_maths"));
        assertEquals("{}", config.optString("features_missing", "{}"));
    }
}
