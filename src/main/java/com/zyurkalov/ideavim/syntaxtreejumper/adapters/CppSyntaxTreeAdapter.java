package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default PSI-based implementation of SyntaxTreeAdapter.
 * This wraps the standard IntelliJ PSI tree operations.
 */
public class CppSyntaxTreeAdapter implements SyntaxTreeAdapter {
    private final CppPsiTree cppPsiTree;

    public CppSyntaxTreeAdapter(@NotNull PsiFile psiFile) {
        cppPsiTree = new CppPsiTree(psiFile);
    }

    @Override
    public @Nullable PsiFile getPsiFile() {
        return cppPsiTree.getPsiFile();
    }

    @Override
    @Nullable
    public CppSyntaxNode findNodeAt(int offset) {
        return cppPsiTree.findNodeAt(offset);
    }

    @Override
    @Nullable
    public CppSyntaxNode findCommonParent(@NotNull SyntaxNode node1, @NotNull SyntaxNode node2) {
        return cppPsiTree.findCommonParent(node1, node2);
    }

    @Override
    public int getDocumentLength() {
        return cppPsiTree.getDocumentLength();
    }


}