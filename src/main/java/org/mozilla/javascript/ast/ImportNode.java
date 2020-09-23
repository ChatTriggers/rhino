package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * Node used for import statements, as well as
 * a base class for the ExportNode.
 */
public class ImportNode extends AstNode {
    private List<ModuleMember> namedMembers = new ArrayList<>();
    private ModuleMember defaultMember = null;
    private ModuleMember moduleMember = null;

    // ex: "import * as myModule from '...';"
    // or  "export * from '...';"
    private String filePath = null;

    {
        type = Token.IMPORT;
    }

    public List<ModuleMember> getNamedMembers() {
        return namedMembers;
    }

    public ModuleMember getDefaultMember() {
        return defaultMember;
    }

    public ModuleMember getModuleImport() {
        return moduleMember;
    }

    public void addNamedMember(String targetName, String scopeName) {
        namedMembers.add(new ModuleMember(targetName, scopeName));
    }

    public void setDefaultMember(String scopeName) {
        defaultMember = new ModuleMember(null, scopeName);
    }

    public boolean hasDefaultMember() {
        return defaultMember != null;
    }

    public void setModuleMember(String scopeName) {
        moduleMember = new ModuleMember(null, scopeName);
    }

    public boolean getModuleMember() {
        return moduleMember != null;
    }

    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder("import ");

        if (getModuleMember()) {
            sb.append("* ");

            if (defaultMember.scopeName != null) {
                sb.append(" as ").append(defaultMember.scopeName);
            }
        } else if (hasDefaultMember()) {
            sb.append(defaultMember.scopeName);
        }

        if (!getModuleMember()) {
            if (hasDefaultMember()) {
                sb.append(",");
            }

            sb.append(" {");

            for (int i = 0, namedMemberSize = namedMembers.size(); i < namedMemberSize; i++) {
                ModuleMember imp = namedMembers.get(i);

                sb.append(' ').append(imp.targetName);

                if (imp.scopeName != null) {
                    sb.append(" as ").append(imp.scopeName);
                }

                if (i != namedMemberSize - 1) {
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

    public static class ModuleMember {
        private String targetName;
        private String scopeName;

        public ModuleMember(String targetName, String scopeName) {
            this.targetName = targetName;
            this.scopeName = scopeName;
        }

        public String getTargetName() {
            return targetName;
        }

        public String getScopeName() {
            return scopeName;
        }
    }
}
