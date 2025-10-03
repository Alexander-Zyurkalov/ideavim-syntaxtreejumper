package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RustPsiTree {
    private final PsiFile psiFile;

    public @Nullable PsiFile getPsiFile() {
        return psiFile;
    }

    public RustPsiTree(PsiFile file) {
        this.psiFile = file;
    }

    @Nullable
    public RustSyntaxNode findNodeAt(int offset) {
        PsiElement element = psiFile.findElementAt(offset);
        return element != null ? new RustSyntaxNode(element) : null;
    }

    @Nullable
    public RustSyntaxNode findCommonParent(@NotNull SyntaxNode node1, @NotNull SyntaxNode node2) {
        if (!(node1 instanceof RustSyntaxNode) || !(node2 instanceof RustSyntaxNode)) {
            return null;
        }

        RustSyntaxNode rustNode1 = (RustSyntaxNode) node1;
        RustSyntaxNode rustNode2 = (RustSyntaxNode) node2;

        PsiElement commonParent = PsiTreeUtil.findCommonParent(rustNode1.getPsiElement(), rustNode2.getPsiElement());
        return commonParent != null ? new RustSyntaxNode(commonParent) : null;
    }

     public int getDocumentLength() {
        return psiFile.getTextLength();
    }

}
