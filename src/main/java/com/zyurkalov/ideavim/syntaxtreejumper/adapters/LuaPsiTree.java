package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LuaPsiTree {
    private final PsiFile psiFile;

    public @Nullable PsiFile getPsiFile() {
        return psiFile;
    }

    public LuaPsiTree(PsiFile file) {
        this.psiFile = file;
    }

    @Nullable
    public LuaSyntaxNode findNodeAt(int offset) {
        PsiElement element = psiFile.findElementAt(offset);
        return element != null ? new LuaSyntaxNode(element) : null;
    }

    @Nullable
    public LuaSyntaxNode findCommonParent(@NotNull SyntaxNode node1, @NotNull SyntaxNode node2) {
        if (!(node1 instanceof LuaSyntaxNode) || !(node2 instanceof LuaSyntaxNode)) {
            return null;
        }

        LuaSyntaxNode luaNode1 = (LuaSyntaxNode) node1;
        LuaSyntaxNode luaNode2 = (LuaSyntaxNode) node2;

        PsiElement commonParent = PsiTreeUtil.findCommonParent(luaNode1.getPsiElement(), luaNode2.getPsiElement());
        return commonParent != null ? new LuaSyntaxNode(commonParent) : null;
    }

    public int getDocumentLength() {
        return psiFile.getTextLength();
    }
}