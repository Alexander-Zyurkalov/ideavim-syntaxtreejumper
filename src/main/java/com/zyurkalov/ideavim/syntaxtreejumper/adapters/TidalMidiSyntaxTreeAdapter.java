package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TidalMidi PSI-based implementation of SyntaxTreeAdapter.
 * This wraps the standard IntelliJ PSI tree operations for TidalMidi language.
 */
public class TidalMidiSyntaxTreeAdapter extends SyntaxTreeAdapter {
    private final TidalMidiPsiTree tidalMidiPsiTree;

    public TidalMidiSyntaxTreeAdapter(@NotNull PsiFile psiFile) {
        tidalMidiPsiTree = new TidalMidiPsiTree(psiFile);
    }

    @Override
    public @Nullable PsiFile getPsiFile() {
        return tidalMidiPsiTree.getPsiFile();
    }

    @Override
    @Nullable
    public TidalMidiSyntaxNode findNodeAt(int offset) {
        return tidalMidiPsiTree.findNodeAt(offset);
    }

    @Override
    @Nullable
    public TidalMidiSyntaxNode findCommonParent(@NotNull SyntaxNode node1, @NotNull SyntaxNode node2) {
        return tidalMidiPsiTree.findCommonParent(node1, node2);
    }

    @Override
    public int getDocumentLength() {
        return tidalMidiPsiTree.getDocumentLength();
    }
}