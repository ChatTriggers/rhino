package org.mozilla.javascript.tools.shell;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScriptsRunner extends Main {
    public static void main(String[] _args) {
        File scriptsFiles = new File("./out/scripts");
        File[] scripts = scriptsFiles.listFiles();

        String[] args = new String[(scripts == null ? 0 : scripts.length) + 5];
        args[0] = "-debug";
        args[1] = "-version";
        args[2] = "200";
        args[3] = "-require";
        args[4] = "-f";

        for (int i = 0, scriptsLength = scripts.length; i < scriptsLength; i++) {
            File file = scripts[i];
            args[i + 5] = file.getAbsolutePath();
        }

        Main.main(args);
    }
}
