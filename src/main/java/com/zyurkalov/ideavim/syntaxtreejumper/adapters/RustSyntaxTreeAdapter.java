package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default PSI-based implementation of SyntaxTreeAdapter.
 * This wraps the standard IntelliJ PSI tree operations.
 */
public class RustSyntaxTreeAdapter extends SyntaxTreeAdapter {
    private final RustPsiTree rustPsiTree;

    public RustSyntaxTreeAdapter(@NotNull PsiFile psiFile) {
        rustPsiTree = new RustPsiTree(psiFile);
    }

    @Override
    public @Nullable PsiFile getPsiFile() {
        return rustPsiTree.getPsiFile();
    }

    @Override
    @Nullable
    public RustSyntaxNode findNodeAt(int offset) {
        return rustPsiTree.findNodeAt(offset);
    }

    @Override
    @Nullable
    public RustSyntaxNode findCommonParent(@NotNull SyntaxNode node1, @NotNull SyntaxNode node2) {
        return rustPsiTree.findCommonParent(node1, node2);
    }

    @Override
    public int getDocumentLength() {
        return rustPsiTree.getDocumentLength();
    }

}