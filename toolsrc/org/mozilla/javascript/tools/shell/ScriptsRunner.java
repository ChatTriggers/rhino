package org.mozilla.javascript.tools.shell;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScriptsRunner extends Main {
    public static void main(String[] _args) {
        File scriptIndex = new File("./out/scripts/index.js");

        String[] args = new String[6];
        args[0] = "-debug";
        args[1] = "-version";
        args[2] = "200";
        args[3] = "-require";
        args[4] = "-f";
        args[5] = scriptIndex.getAbsolutePath();

        Main.main(args);
    }
}
