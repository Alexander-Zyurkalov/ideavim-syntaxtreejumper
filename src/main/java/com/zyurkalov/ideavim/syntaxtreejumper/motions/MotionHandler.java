package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;

import java.util.Optional;

public abstract class MotionHandler {
    public abstract Optional<Offsets> findNext(Offsets initialOffsets);

    /**
     * Finds the smallest common parent that fully encompasses the current selection
     */
    PsiElement findSmallestCommonParent(PsiElement leftElement, PsiElement rightElement, Offsets selection) {
        if (rightElement == null) {
            rightElement = leftElement;
        }

        PsiElement commonParent = PsiTreeUtil.findCommonParent(leftElement, rightElement);

        // Walk up the tree until we find an element that fully encompasses our selection
        while (commonParent != null) {
            TextRange range = commonParent.getTextRange();
            if (range.getStartOffset() <= selection.leftOffset() &&
                    range.getEndOffset() >= selection.rightOffset()) {

                // Check if this parent is actually larger than our current selection
                if (range.getStartOffset() < selection.leftOffset() ||
                        range.getEndOffset() > selection.rightOffset()) {
                    return commonParent;
                }
            }
            commonParent = commonParent.getParent();
        }

        return commonParent;
    }
}
