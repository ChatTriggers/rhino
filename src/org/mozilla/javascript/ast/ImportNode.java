package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

import java.util.ArrayList;
import java.util.List;

public class ImportNode extends AstNode {
    private List<Import> namedImports = new ArrayList<>();
    private Import defaultImport = null;

    // ex: import * as myModule from '...';
    private Import moduleImport = null;

    private String filePath = null;

    {
        type = Token.IMPORT;
    }

    public List<Import> getNamedImports() {
        return namedImports;
    }

    public Import getDefaultImport() {
        return defaultImport;
    }

    public Import getModuleImport() {
        return moduleImport;
    }

    public void addNamedImport(Name targetName, Name scopeName) {
        namedImports.add(new Import(targetName, scopeName));
    }

    public void setDefaultImport(Name scopeName) {
        defaultImport = new Import(null, scopeName);
    }

    public boolean hasDefaultImport() {
        return defaultImport != null;
    }

    public void setModuleImport(Name scopeName) {
        moduleImport = new Import(null, scopeName);
    }

    public boolean isModuleImport() {
        return moduleImport != null;
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder("import ");

        if (isModuleImport()) {
            sb.append("* ");

            if (defaultImport.scopeName != null) {
                sb.append(" as ").append(defaultImport.scopeName.getIdentifier());
            }
        } else if (hasDefaultImport()) {
            sb.append(defaultImport.scopeName);
        }

        if (!isModuleImport()) {
            if (hasDefaultImport()) {
                sb.append(",");
            }

            sb.append(" {");

            for (int i = 0, namedImportsSize = namedImports.size(); i < namedImportsSize; i++) {
                Import imp = namedImports.get(i);

                sb.append(' ').append(imp.targetName);

                if (imp.scopeName != null) {
                    sb.append(" as ").append(imp.scopeName.getIdentifier());
                }

                if (i != namedImportsSize - 1) {
                    sb.append(',');
                }
            }

            sb.append(" } ");
        }

        sb.append("from '").append(filePath).append("';");

        return sb.toString();
    }

    @Override
    public void visit(NodeVisitor v) {
        v.visit(this);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public static class Import {
        private Name targetName;
        private Name scopeName;

        public Import(Name targetName, Name scopeName) {
            this.targetName = targetName;
            this.scopeName = scopeName;
        }

        public String getTargetIdentifier() {
            return targetName != null ? targetName.getIdentifier() : null;
        }

        public String getScopeIdentifier() {
            return scopeName != null ? scopeName.getIdentifier() : null;
        }
    }
}
