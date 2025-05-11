package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;

import java.util.Optional;

public class SubWordNavigator implements Navigator {

    private final PsiFile psiFile;
    private final Direction direction;
    private final SubWordNavigation navigation;

    public SubWordNavigator(PsiFile psiFile, Direction direction) {
        this.psiFile = psiFile;
        this.direction = direction;
        this.navigation = new SubWordNavigation(direction);
    }

    @Override
    public Optional<Offsets> findNextObjectsOffsets(Offsets initialOffsets) {
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
                case Direction.FORWARD ->  initialOffsets.rightOffset();
                case Direction.BACKWARD -> initialOffsets.leftOffset() - 1;
            };
            return findNextObjectsOffsets(new Offsets(startOffset, startOffset ));
        }

        return Optional.of(nextSubWordOffsets);

    }

    private Offsets findNextSubWord(Offsets initialOffsets, PsiElement elementAtLeft) {
        int leftElementBorder = elementAtLeft.getTextRange().getStartOffset();
        int relativeLeftTextPosition = initialOffsets.leftOffset() - leftElementBorder;
        int relativeRightTextPosition = initialOffsets.rightOffset() - leftElementBorder;

        Offsets relativeOffset = new Offsets(relativeLeftTextPosition, relativeRightTextPosition);
        Offsets newRelativeOffset = navigation.findNextSubWord(relativeOffset, elementAtLeft.getText());

        return new Offsets(newRelativeOffset.leftOffset() + leftElementBorder,
                newRelativeOffset.rightOffset() + leftElementBorder);
    }
}
