package org.mozilla.javascript.tools.compattable;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.*;

public class TestersDeserializer implements JsonDeserializer<Map<String, List<Testers.Test>>> {
    @Override
    public Map<String, List<Testers.Test>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Map<String, List<Testers.Test>> tests = new HashMap<>();
        Set<Map.Entry<String, JsonElement>> elements =  json.getAsJsonObject().entrySet();

        for (Map.Entry<String, JsonElement> element : elements) {
            List<Testers.Test> subTests = new ArrayList<>();

            for (Map.Entry<String, JsonElement> test : ((JsonObject) element.getValue()).entrySet()) {
                subTests.add(new Testers.Test(
                        test.getKey(),
                        test.getValue().getAsString()
                ));
            }

            tests.put(element.getKey(), subTests);
        }

        return tests;
    }
}
