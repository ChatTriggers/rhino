package org.mozilla.javascript.tools.shell;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.provider.StrongCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScriptsRunner extends Main {
    public static void main(String[] _args) {
        File scriptIndex = new File("./scripts/index.js");

        ContextFactory cxFactory = new ScriptsRunnerContextFactory();
        Context cx = cxFactory.enterContext();

        Global global = new Global();
        global.init(cx);

        List<String> modulePath = new ArrayList<>();
        modulePath.add(scriptIndex.toURI().getPath());
        Require require = global.installRequire(cx, modulePath, false);

        require.getExportedModuleInterface(cx, "index", scriptIndex.toURI(), null, false);
        Context.exit();
    }

    private static class ScriptsRunnerContextFactory extends ContextFactory {
        @Override
        protected void onContextCreated(Context cx) {
            super.onContextCreated(cx);

            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setDebugOutputPath(new File(".", "DEBUG"));
        }

        @Override
        protected boolean hasFeature(Context cx, int featureIndex) {
            if (featureIndex == Context.EMIT_DEBUG_OUTPUT || featureIndex == Context.FEATURE_LOCATION_INFORMATION_IN_ERROR) {
                return true;
            }

            return super.hasFeature(cx, featureIndex);
        }
    }
}
