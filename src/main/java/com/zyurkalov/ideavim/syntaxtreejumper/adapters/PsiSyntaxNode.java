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
public record PsiSyntaxNode(PsiElement psiElement) implements SyntaxNode {
    public PsiSyntaxNode(@NotNull PsiElement psiElement) {
        this.psiElement = psiElement;
    }

    @Override
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
    public SyntaxNode getParent() {
        PsiElement parent = psiElement.getParent();
        return parent != null ? new PsiSyntaxNode(parent) : null;
    }

    @Override
    @NotNull
    public List<SyntaxNode> getChildren() {
        PsiElement[] children = psiElement.getChildren();
        if (children.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(children)
                .map(PsiSyntaxNode::new)
                .map(node -> (SyntaxNode) node)
                .toList();
    }

    @Override
    @Nullable
    public SyntaxNode getPreviousSibling() {
        PsiElement sibling = psiElement.getPrevSibling();
        return sibling != null ? new PsiSyntaxNode(sibling) : null;
    }

    @Override
    @Nullable
    public SyntaxNode getNextSibling() {
        PsiElement sibling = psiElement.getNextSibling();
        return sibling != null ? new PsiSyntaxNode(sibling) : null;
    }

    @Override
    public boolean isWhitespace() {
        return psiElement instanceof PsiWhiteSpace || psiElement.getText().trim().isEmpty();
    }

    @Override
    public boolean isEquivalentTo(@Nullable SyntaxNode other) {
        if (!(other instanceof PsiSyntaxNode(PsiElement element))) {
            return false;
        }
        return psiElement.isEquivalentTo(element);
    }

    @Override
    @NotNull
    public String getNodeTypeName() {
        return psiElement.getClass().getSimpleName();
    }

    @Override
    public @NotNull String getTypeName() {
        return psiElement.getNode().getElementType().toString();
    }



    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PsiSyntaxNode(PsiElement element))) return false;
        return Objects.equals(psiElement, element);
    }

    @Override
    public @NotNull String toString() {
        return "PsiSyntaxNode{" + psiElement.getClass().getSimpleName() +
                ", text='" + getText() + "'}";
    }
}
