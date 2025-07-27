package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * PSI-based implementation of SyntaxNode.
 */
public record CppSyntaxNode(PsiElement psiElement) implements SyntaxTreeAdapter.SyntaxNode {
    public CppSyntaxNode(@NotNull PsiElement psiElement) {
        this.psiElement = psiElement;
    }

    @Override
    @NotNull
    public TextRange getTextRange() {
        return psiElement.getTextRange();
    }

    @Override
    @NotNull
    public String getText() {
        return psiElement.getText();
    }

    @Override
    @Nullable
    public SyntaxTreeAdapter.SyntaxNode getParent() {
        PsiElement parent = psiElement.getParent();
        return parent != null ? new CppSyntaxNode(parent) : null;
    }

    @Override
    @NotNull
    public List<SyntaxTreeAdapter.SyntaxNode> getChildren() {
        PsiElement[] children = psiElement.getChildren();
        if (children.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(children)
                .map(CppSyntaxNode::new)
                .map(node -> (SyntaxTreeAdapter.SyntaxNode) node)
                .toList();
    }

    @Override
    @Nullable
    public SyntaxTreeAdapter.SyntaxNode getPreviousSibling() {
        PsiElement sibling = psiElement.getPrevSibling();
        return sibling != null ? new CppSyntaxNode(sibling) : null;
    }

    @Override
    @Nullable
    public SyntaxTreeAdapter.SyntaxNode getNextSibling() {
        PsiElement sibling = psiElement.getNextSibling();
        return sibling != null ? new CppSyntaxNode(sibling) : null;
    }

    @Override
    public boolean isWhitespace() {
        return psiElement instanceof PsiWhiteSpace || psiElement.getText().trim().isEmpty();
    }

    @Override
    public boolean isEquivalentTo(@Nullable SyntaxTreeAdapter.SyntaxNode other) {
        if (!(other instanceof CppSyntaxNode(PsiElement element))) {
            return false;
        }
        return psiElement.isEquivalentTo(element);
    }

    @Override
    @NotNull
    public String getNodeTypeName() {
        return psiElement.getClass().getSimpleName();
    }

    /**
     * Gets the underlying PSI element. This method is provided for cases where
     * PSI-specific operations are needed, but should be used sparingly to maintain
     * the abstraction.
     */
    @Override
    @NotNull
    public PsiElement psiElement() {
        return psiElement;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CppSyntaxNode(PsiElement element))) return false;
        return Objects.equals(psiElement, element);
    }

    @Override
    public @NotNull String toString() {
        return "PsiSyntaxNode{" + psiElement.getClass().getSimpleName() +
                ", text='" + getText() + "'}";
    }
}
