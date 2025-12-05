package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class for TidalMidi PSI tree operations.
 */
public class TidalMidiPsiTree {
    private final PsiFile psiFile;

    public TidalMidiPsiTree(PsiFile file) {
        this.psiFile = file;
    }

    public @Nullable PsiFile getPsiFile() {
        return psiFile;
    }

    @Nullable
    public TidalMidiSyntaxNode findNodeAt(int offset) {
        PsiElement element = psiFile.findElementAt(offset);
        return element != null ? new TidalMidiSyntaxNode(element) : null;
    }

    @Nullable
    public TidalMidiSyntaxNode findCommonParent(@NotNull SyntaxNode node1, @NotNull SyntaxNode node2) {
        if (!(node1 instanceof TidalMidiSyntaxNode) || !(node2 instanceof TidalMidiSyntaxNode)) {
            return null;
        }

        TidalMidiSyntaxNode tidalNode1 = (TidalMidiSyntaxNode) node1;
        TidalMidiSyntaxNode tidalNode2 = (TidalMidiSyntaxNode) node2;

        PsiElement commonParent = PsiTreeUtil.findCommonParent(
            tidalNode1.getPsiElement(), 
            tidalNode2.getPsiElement()
        );
        
        return commonParent != null ? new TidalMidiSyntaxNode(commonParent) : null;
    }

    public int getDocumentLength() {
        return psiFile.getTextLength();
    }
}