package org.mozilla.javascript.ast;

public class ExportNode extends ImportNode {
    private AstNode exportedValue = null;
    private boolean defaultExport = false;

    public AstNode getExportedValue() {
        return exportedValue;
    }

    public void setExportedValue(AstNode exportedValue) {
        this.exportedValue = exportedValue;
    }

    public boolean hasExportedValue() {
        return exportedValue != null;
    }

    public boolean isDefaultExport() {
        return defaultExport;
    }

    public void setDefaultExport() {
        this.defaultExport = true;
    }
}
