package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;

import java.util.Optional;

public class SubWordMotionHandler implements MotionHandler {

    private final PsiFile psiFile;
    private final MotionDirection direction;
    private final SubWordFinder navigation;

    public SubWordMotionHandler(PsiFile psiFile, MotionDirection direction) {
        this.psiFile = psiFile;
        this.direction = direction;
        this.navigation = new SubWordFinder(direction);
    }

    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {
        PsiElement elementAtLeft = psiFile.findElementAt(initialOffsets.leftOffset());
        if (elementAtLeft == null) {
            return Optional.empty();
        }
        if (initialOffsets.leftOffset() != initialOffsets.rightOffset()) {
            PsiElement elementAtRight = psiFile.findElementAt(initialOffsets.rightOffset() - 1);
            if (!elementAtLeft.isEquivalentTo(elementAtRight)) {
                return Optional.empty();
            }
        }

        Offsets nextSubWordOffsets = findNextSubWord(initialOffsets, elementAtLeft);
        if (nextSubWordOffsets.equals(initialOffsets)) {
            int startOffset = switch (direction) {
                case FORWARD ->  initialOffsets.rightOffset();
                case BACKWARD -> initialOffsets.leftOffset() - 1;
                case EXPAND -> 0; //TODO: come up with what shall I do here
                case SHRINK -> 0;
            };
            return findNext(new Offsets(startOffset, startOffset ));
        }

        return Optional.of(nextSubWordOffsets);

    }

    private Offsets findNextSubWord(Offsets initialOffsets, PsiElement elementAtLeft) {
        int leftElementBorder = elementAtLeft.getTextRange().getStartOffset();
        int relativeLeftTextPosition = initialOffsets.leftOffset() - leftElementBorder;
        int relativeRightTextPosition = initialOffsets.rightOffset() - leftElementBorder;

        Offsets relativeOffset = new Offsets(relativeLeftTextPosition, relativeRightTextPosition);
        Offsets newRelativeOffset = navigation.findNext(relativeOffset, elementAtLeft.getText());

        return new Offsets(newRelativeOffset.leftOffset() + leftElementBorder,
                newRelativeOffset.rightOffset() + leftElementBorder);
    }
}
