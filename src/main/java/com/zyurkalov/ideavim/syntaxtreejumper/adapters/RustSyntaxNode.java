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
        return getTypeName().contains("CALL_EXPR");
    }

    @Override
    public boolean isFunctionArgument() {
        SyntaxNode parent = getParent();
        if (parent == null) {
            return false;
        }
        return parent.getTypeName().contains("VALUE_PARAMETER_LIST") &&
                getTypeName().contains("VALUE_PARAMETER");
    }


}