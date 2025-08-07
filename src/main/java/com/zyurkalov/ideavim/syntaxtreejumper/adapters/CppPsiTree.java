package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CppPsiTree {
    private final PsiFile psiFile;

    public @Nullable PsiFile getPsiFile() {
        return psiFile;
    }

    public CppPsiTree(PsiFile file) {
        this.psiFile = file;
    }

    @Nullable
    public CppSyntaxNode findNodeAt(int offset) {
        PsiElement element = psiFile.findElementAt(offset);
        return element != null ? new CppSyntaxNode(element) : null;
    }

    @Nullable
    public CppSyntaxNode findCommonParent(@NotNull SyntaxNode node1, @NotNull SyntaxNode node2) {
        if (!(node1 instanceof CppSyntaxNode) || !(node2 instanceof CppSyntaxNode)) {
            return null;
        }

        CppSyntaxNode cppNode1 = (CppSyntaxNode) node1;
        CppSyntaxNode cppNode2 = (CppSyntaxNode) node2;

        PsiElement commonParent = PsiTreeUtil.findCommonParent(cppNode1.getPsiElement(), cppNode2.getPsiElement());
        return commonParent != null ? new CppSyntaxNode(commonParent) : null;
    }

     public int getDocumentLength() {
        return psiFile.getTextLength();
    }

}
