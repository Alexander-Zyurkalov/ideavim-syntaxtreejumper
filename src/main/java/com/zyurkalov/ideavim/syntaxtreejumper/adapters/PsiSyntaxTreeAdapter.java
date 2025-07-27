package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default PSI-based implementation of SyntaxTreeAdapter.
 * This wraps the standard IntelliJ PSI tree operations.
 */
public record PsiSyntaxTreeAdapter(PsiFile psiFile) implements SyntaxTreeAdapter {

    public PsiSyntaxTreeAdapter(@NotNull PsiFile psiFile) {
        this.psiFile = psiFile;
    }

    /**
     * Gets the underlying PSI file. This method is provided for cases where
     * PSI-specific operations are needed, but should be used sparingly to maintain
     * the abstraction.
     */
    @Override
    @NotNull
    public PsiFile psiFile() {
        return psiFile;
    }

    @Override
    @Nullable
    public SyntaxNode findNodeAt(int offset) {
        PsiElement element = psiFile.findElementAt(offset);
        return element != null ? new PsiSyntaxNode(element) : null;
    }

    @Override
    @Nullable
    public SyntaxNode findCommonParent(@NotNull SyntaxNode node1, @NotNull SyntaxNode node2) {
        if (!(node1 instanceof PsiSyntaxNode(PsiElement psiElement)) || !(node2 instanceof PsiSyntaxNode(
                PsiElement element
        ))) {
            return null;
        }

        PsiElement commonParent = PsiTreeUtil.findCommonParent(psiElement, element);
        return commonParent != null ? new PsiSyntaxNode(commonParent) : null;
    }

    @Override
    public int getDocumentLength() {
        return psiFile.getTextLength();
    }

}