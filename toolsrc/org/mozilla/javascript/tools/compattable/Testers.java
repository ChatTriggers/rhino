package org.mozilla.javascript.tools.compattable;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Testers {
    public static Type TYPE = new TypeToken<Map<String, List<Test>>>(){}.getType();
    public Map<String, List<Test>> tests = new HashMap<>();

    static class Test {
        public String testName;
        public String testSource;

        Test(String testName, String testSource) {
            this.testName = testName;
            this.testSource = "var fn = Function('asyncTestPassed', 'asyncTestFailed', `" + testSource + "`);" +
                              "fn(asyncTestPassed, asyncTestFailed);";
        }
    }
}
