package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lua PSI-based implementation of SyntaxTreeAdapter.
 * This wraps the Lua PSI tree operations using LuaPsiTree.
 */
public class LuaSyntaxTreeAdapter extends SyntaxTreeAdapter {
    private final LuaPsiTree luaPsiTree;

    public LuaSyntaxTreeAdapter(@NotNull PsiFile psiFile) {
        luaPsiTree = new LuaPsiTree(psiFile);
    }

    @Override
    public @Nullable PsiFile getPsiFile() {
        return luaPsiTree.getPsiFile();
    }

    @Override
    @Nullable
    public LuaSyntaxNode findNodeAt(int offset) {
        return luaPsiTree.findNodeAt(offset);
    }

    @Override
    @Nullable
    public LuaSyntaxNode findCommonParent(@NotNull SyntaxNode node1, @NotNull SyntaxNode node2) {
        return luaPsiTree.findCommonParent(node1, node2);
    }

    @Override
    public int getDocumentLength() {
        return luaPsiTree.getDocumentLength();
    }
}