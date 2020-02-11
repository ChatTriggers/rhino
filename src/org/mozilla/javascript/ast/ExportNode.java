package org.mozilla.javascript.ast;


public class ExportNode extends ImportNode {
    private AstNode exportedValue = null;
    private String identifier = null;
    private boolean defaultExport = false;

    public AstNode getExportedValue() {
        return exportedValue;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setExportedValue(AstNode node, String identifier) {
        this.exportedValue = node;
        this.identifier = identifier;
    }

    public void setExportedValue(AstNode node) {
        this.exportedValue = node;
        this.defaultExport = true;
    }

    public boolean isDefaultExport() {
        return defaultExport;
    }
}
