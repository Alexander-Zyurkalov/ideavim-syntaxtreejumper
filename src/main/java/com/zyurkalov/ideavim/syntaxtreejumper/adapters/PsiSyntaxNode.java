
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
public class PsiSyntaxNode extends SyntaxNode {

    public PsiSyntaxNode(PsiElement psiElement) {
        super(psiElement);
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
    public boolean isEquivalentTo(@Nullable SyntaxNode other) {
        if (!(other instanceof PsiSyntaxNode otherNode)) {
            return false;
        }
        return psiElement.isEquivalentTo(otherNode.getPsiElement());
    }


    @Override
    public SyntaxNode getFirstChild() {
        PsiElement firstChild = psiElement.getFirstChild();
        if (firstChild == null) {
            return  null;
        }
        return new PsiSyntaxNode(firstChild);
    }

    @Override
    public SyntaxNode getLastChild() {
        PsiElement lastChild = psiElement.getLastChild();
        if (lastChild == null) {
            return  null;
        }
        return new PsiSyntaxNode(lastChild);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PsiSyntaxNode other)) return false;
        return Objects.equals(psiElement, other.psiElement);
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
}