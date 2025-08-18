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
public class CppSyntaxNode extends SyntaxNode {


    public CppSyntaxNode(PsiElement psiElement) {
        super(psiElement);
    }

    @Override
    @Nullable
    public SyntaxNode getParent() {
        PsiElement parent = psiElement.getParent();
        return parent != null ? new CppSyntaxNode(parent) : null;
    }

    @Override
    @NotNull
    public List<SyntaxNode> getChildren() {
        PsiElement[] children = psiElement.getChildren();
        if (children.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(children)
                .map(CppSyntaxNode::new)
                .map(node -> (SyntaxNode) node)
                .toList();
    }

    @Override
    @Nullable
    public SyntaxNode getPreviousSibling() {
        PsiElement sibling = psiElement.getPrevSibling();
        return sibling != null ? new CppSyntaxNode(sibling) : null;
    }

    @Override
    @Nullable
    public SyntaxNode getNextSibling() {
        PsiElement sibling = psiElement.getNextSibling();
        return sibling != null ? new CppSyntaxNode(sibling) : null;
    }


    @Override
    public boolean isEquivalentTo(@Nullable SyntaxNode other) {
        if (!(other instanceof CppSyntaxNode cppNode)) {
            return false;
        }
        return psiElement.isEquivalentTo(cppNode.psiElement);
    }

    @Override
    public SyntaxNode getFirstChild() {

        PsiElement firstChild = psiElement.getFirstChild();
        if (firstChild == null) {
            return  null;
        }
        return new CppSyntaxNode(firstChild);
    }

    @Override
    public SyntaxNode getLastChild() {
        PsiElement lastChild = psiElement.getLastChild();
        if (lastChild == null) {
            return  null;
        }
        return new CppSyntaxNode(lastChild);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CppSyntaxNode that)) return false;
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
    public boolean isFunctionParameter() {
        return getTypeName().contains("PARAMETER_DECLARATION");
    }

    @Override
    public boolean isFunctionArgument() {
        boolean result = false;
        try {
            result = getTypeName().contains("EXPRESSION") &&
                    Objects.requireNonNull(getParent()).getTypeName().equals("ARGUMENT_LIST")/* &&
                    Objects.requireNonNull(getParent().getParent()).getTypeName().equals("CALL_EXPRESSION")*/;
        } catch (NullPointerException ignored) {
        }
        return result;
    }

    @Override
    public boolean isTypeParameter() {
        boolean result = false;
        try {
            result = getTypeName().equals("TYPE_PARAMETER") ||
                    (getTypeName().equals("TYPE_ELEMENT") &&
                            Objects.requireNonNull(getParent()).getTypeName().equals("TEMPLATE_ARGUMENT_LIST"));
        } catch (NullPointerException ignored) {}
        return result;
    }

    @Override
    public boolean isMethodDefinition() {
        return isFunctionDefinition();
    }

    @Override
    public boolean isFunctionDefinition() {
        String typeName = getTypeName();
        return
                typeName.equals("FUNCTION_DEFINITION") ||
                typeName.equals("CPP_LAMBDA_EXPRESSION");
    }
}