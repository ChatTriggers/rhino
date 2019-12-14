package org.mozilla.javascript.tools.compattable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.mozilla.javascript.*;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.Main;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class CompatTableMain extends Main {
    private static final CompatTableContextFactory cxFactory = new CompatTableContextFactory();
    private static final File testersFile = new File("./docs/testers.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Testers.TYPE, new TestersDeserializer()).create();
    private static final String RHINO_VERSION = "master";

    public static void main(String[] args) {
        Global global = getGlobal();

        ScriptableObject.defineProperty(global, "asyncTestPassed", BaseFunction.wrap((_cx, _scope, _thisObj, _args) -> {
            System.out.println("asyncTestPassed");
            return null;
        }), ScriptableObject.NOT_ENUMERABLE);

        ScriptableObject.defineProperty(global, "asyncTestFailed", BaseFunction.wrap((_cx, _scope, _thisObj, _args) -> {
            System.out.println("asyncTestFailed");
            return null;
        }), ScriptableObject.NOT_ENUMERABLE);

        BaseFunction createIterableObject = BaseFunction.wrap((_cx, _scope, _thisObj, _args) -> {
            NativeArray arr = (NativeArray) _args[0];
            NativeObject methods = _args.length > 1 ? (NativeObject) _args[1] : new NativeObject();

            ScriptableObject.putProperty(arr, "length", ScriptRuntime.add(ScriptableObject.getProperty(arr, "length"), 1, _cx));
            NativeObject iterator = new NativeObject();
            ScriptableObject.putProperty(iterator, "next", BaseFunction.wrap((_cx2, _scope2, _thisObj2, _args2) -> {
                NativeObject ret = new NativeObject();
                ScriptableObject.putProperty(ret, "value", ScriptableObject.callMethod(arr, "shift", new Object[]{}));
                ScriptableObject.putProperty(ret, "done", ScriptRuntime.toInteger(ScriptableObject.getProperty(arr, "length")) <= 0);
                return ret;
            }));
            Object returnMethod = ScriptableObject.getProperty(methods, "return");
            Object throwMethod = ScriptableObject.getProperty(methods, "throw");

            if (returnMethod == UniqueTag.NOT_FOUND)
                returnMethod = Undefined.instance;
            if (throwMethod == UniqueTag.NOT_FOUND)
                throwMethod = Undefined.instance;

            ScriptableObject.putProperty(iterator, "return", returnMethod);
            ScriptableObject.putProperty(iterator, "throw", throwMethod);

            NativeObject iterable = new NativeObject();
            ScriptableObject.putProperty(iterable, SymbolKey.ITERATOR, BaseFunction.wrap((_cx2, _scope2, _thisObj2, _args2) -> iterator));

            return iterable;
        });

        ScriptableObject.putProperty(global, "__createIterableObject", createIterableObject);

        cxFactory.call(cx -> {
            try {
                global.init(cx);
                Map<String, List<Testers.Test>> tests = compileTests();
                runTests(cx, tests);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    private static Map<String, List<Testers.Test>> compileTests() throws IOException {
        StringBuilder source = new StringBuilder();
        List<String> testerLines = Files.readAllLines(testersFile.toPath());

        for (String line : testerLines) {
            source.append(line);
        }

        return gson.fromJson(source.toString(), Testers.TYPE);
    }

    private static void runTests(Context cx, Map<String, List<Testers.Test>> tests) throws IOException {
        JsonObject results = new JsonObject();
        results.addProperty("_version", "UNKNOWN");
        results.addProperty("_engine", "Rhino");

        for (Map.Entry<String, List<Testers.Test>> testSuite : tests.entrySet()) {
            JsonObject suiteResult = new JsonObject();
            int successful = 0;
            int count = 0;

            for (Testers.Test test : testSuite.getValue()) {
                String testName = test.testName;
                count++;

                Object testResult;

                try {
                    Script script = cx.compileString(test.testSource, "<test>", 0, null);
                    Object result = script.exec(cx, getGlobal());
                    testResult = ScriptRuntime.toBoolean(result);
                } catch (Exception e) {
                    testResult = e.getMessage();
                }

                if (testResult instanceof Boolean) {
                    suiteResult.addProperty(testName, (Boolean) testResult);
                    if ((boolean) testResult)
                        successful++;
                } else if (testResult != null) {
                    suiteResult.addProperty(testName, (String) testResult);
                } else {
                    throw new Error();
                }
            }

            suiteResult.addProperty("_successful", successful);
            suiteResult.addProperty("_count", count);
            suiteResult.addProperty("_percent", ((double) successful) / ((double) count));
            results.add(testSuite.getKey(), suiteResult);
        }

        File resultsFile = new File("./docs/rhino-results/" + RHINO_VERSION + ".json");
        try (FileOutputStream fos = new FileOutputStream(resultsFile)) {
            fos.write(gson.toJson(results).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        resultsFile = new File("./docs/rhino-results/" + RHINO_VERSION + "-es6.json");
        try (FileOutputStream fos = new FileOutputStream(resultsFile)) {
            fos.write(gson.toJson(results).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Process process = Runtime.getRuntime().exec("node ./docs/buildrhino.js");

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        System.out.println("Here is the standard output of the command:\n");
        String s;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }
    }
}
