package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * PSI-based implementation of SyntaxNode.
 */
public class RustSyntaxNode extends SyntaxNode {


    public RustSyntaxNode(PsiElement psiElement) {
        super(psiElement);
    }

    @Override
    @Nullable
    public SyntaxNode getParent() {
        PsiElement parent = psiElement.getParent();
        return parent != null ? new RustSyntaxNode(parent) : null;
    }

    @Override
    @NotNull
    public List<SyntaxNode> getChildren() {
        PsiElement[] children = psiElement.getChildren();
        if (children.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(children)
                .map(RustSyntaxNode::new)
                .map(node -> (SyntaxNode) node)
                .toList();
    }

    @Override
    @Nullable
    public SyntaxNode getPreviousSibling() {
        PsiElement sibling = psiElement.getPrevSibling();
        return sibling != null ? new RustSyntaxNode(sibling) : null;
    }

    @Override
    @Nullable
    public SyntaxNode getNextSibling() {
        PsiElement sibling = psiElement.getNextSibling();
        return sibling != null ? new RustSyntaxNode(sibling) : null;
    }


    @Override
    public boolean isEquivalentTo(@Nullable SyntaxNode other) {
        if (!(other instanceof RustSyntaxNode rustNode)) {
            return false;
        }
        return psiElement.isEquivalentTo(rustNode.psiElement);
    }

    @Override
    public SyntaxNode getFirstChild() {

        PsiElement firstChild = psiElement.getFirstChild();
        if (firstChild == null) {
            return null;
        }
        return new RustSyntaxNode(firstChild);
    }

    @Override
    public SyntaxNode getLastChild() {
        PsiElement lastChild = psiElement.getLastChild();
        if (lastChild == null) {
            return null;
        }
        return new RustSyntaxNode(lastChild);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RustSyntaxNode that)) return false;
        return Objects.equals(psiElement, that.psiElement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(psiElement);
    }

    @Override
    public @NotNull String toString() {
        return "PsiSyntaxNode{" + psiElement.getClass().getSimpleName() +
                ", text='" + getText() + "'}";
    }

    @Override
    public boolean isMethodOrFunctionCallExpression() {
        String typeName = getTypeName();
        return typeName.equals("CALL_EXPR") || typeName.equals("METHOD_CALL") || typeName.equals("STRUCT_LITERAL");
    }

    @Override
    public boolean isFunctionArgument() {
        SyntaxNode parent = getParent();
        if (parent == null) {
            return false;
        }
        String parentTypeName = parent.getTypeName();
        String typeName = getTypeName();
        return (parentTypeName.equals("VALUE_PARAMETER_LIST") || parentTypeName.equals("VALUE_ARGUMENT_LIST") || parentTypeName.equals("STRUCT_LITERAL_BODY")) &&
                (typeName.equals("VALUE_PARAMETER") || typeName.equals("SELF_PARAMETER") ||
                        typeName.contains("EXPR") || typeName.equals("STRUCT_LITERAL_FIELD"));
    }

    @Override
    public boolean isExpressionStatement() {
        return getTypeName().contains("EXPR_STMT");
    }

    @Override
    public boolean isDeclarationStatement() {
        return getTypeName().equals("LET_DECL");
    }

    @Override
    public boolean isAStatement() {
        return false;
    }

    @Override
    public boolean isReturnStatement() {
        return false;
    }

    @Override
    public boolean isLoopStatement() {
        String typeName = getTypeName();
        if (isExpressionStatement() && getChildren().size() == 1) {
            typeName = getFirstChild().getTypeName();
        }
        return typeName.equals("FOR_EXPR") ||
                typeName.equals("LOOP_EXPR") ||
                typeName.equals("WHILE_EXPR") ||
                typeName.equals("CLASSIC_MATCH_EXPR") ||
                typeName.equals("IF_EXPR") ||
                typeName.equals("ELSE_BRANCH") ||
                typeName.equals("MATCH_ARM");
    }

    @Override
    public boolean isFunctionDefinition() {
        return getTypeName().equals("FUNCTION");
    }

    @Override
    public boolean isMethodDefinition() {
        return isFunctionDefinition();
    }

    @Override
    public boolean isVariable() {
        String typeName = getTypeName();
        SyntaxNode parent = getParent();

        if (parent == null) {
            return false;
        }
        String parentTypeName = parent.getTypeName();
        boolean isParentMatchesRequirement = parentTypeName.equals("NAMED_FIELD_DECL") ||
                parentTypeName.equals("FIELD_LOOKUP");
        return typeName.equals("PAT_IDENT") || typeName.equals("PAT_BINDING") || typeName.equals("PATH_EXPR") ||
                isParentMatchesRequirement && typeName.equals("identifier");
    }

    @Override
    public boolean isBody() {
        String typeName = getTypeName();
        return typeName.equals("BLOCK") ||
                typeName.equals("MEMBERS") ||
                typeName.equals("BLOCK_FIELDS") ||
                typeName.equals("MACRO_EXPANSION") ||
                typeName.equals("MATCH_BODY") ||
                typeName.equals("BLOCK_EXPR") ||
                typeName.equals("MACRO_BODY");

    }

    @Override
    public boolean isEqualSymbol() {
        return getTypeName().equals("=");
    }

    @Override
    public boolean isExpression() {
        return getTypeName().endsWith("_EXPR");
    }

    @Override
    public boolean isClassDefinition() {
        String typeName = getTypeName();
        return typeName.equals("STRUCT_ITEM") ||
                typeName.equals("IMPL_ITEM") ||
                typeName.equals("TRAIT_ITEM");
    }

    @Override
    public boolean isTemplate() {
        var children = getChildren();
        if (children.isEmpty()) {
            return false;
        }
        boolean hasTypeParameters = false;
        for (var child : children) {
            if (child.getTypeName().equals("TYPE_PARAMETER_LIST")) {
                hasTypeParameters = true;
                break;
            }
        }
        return hasTypeParameters && (isClassDefinition() || isFunctionDefinition());
    }

    @Override
    public boolean isComment() {
        String typeName = getTypeName();
        return typeName.equals("<EOL_COMMENT>") ||
                typeName.equals("<BLOCK_COMMENT>") ||
                typeName.equals("<INNER_EOL_DOC_COMMENT>") ||
                typeName.equals("<OUTER_EOL_DOC_COMMENT>");
    }

    @Override
    public boolean isMacro() {
        String typeName = getTypeName();
        return typeName.contains("MACRO") ||
                typeName.contains("MACRO_CALL");
    }

    @Override
    public boolean isImport() {
        return getTypeName().equals("USE_ITEM");
    }

    @Override
    public boolean isTypeUsage() {
        String typeName = getTypeName();
        return typeName.equals("PATH_TYPE") || typeName.equals("TRAIT_REF");
    }
}