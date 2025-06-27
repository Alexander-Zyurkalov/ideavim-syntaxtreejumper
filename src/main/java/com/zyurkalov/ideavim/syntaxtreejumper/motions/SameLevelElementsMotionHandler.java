package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SameLevelElementsMotionHandler extends MotionHandler {

    private final PsiFile psiFile;
    private final Direction direction;

    public SameLevelElementsMotionHandler(PsiFile psiFile, Direction direction) {
        this.psiFile = psiFile;
        this.direction = direction;
    }

    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {

        PsiElement initialElement;
        boolean isOnlyCaretButNoSelection = initialOffsets.leftOffset() >= initialOffsets.rightOffset() - 1;
        if (isOnlyCaretButNoSelection) {
            initialElement = psiFile.findElementAt(initialOffsets.leftOffset());

        } else {
            PsiElement initElementAtLeft = psiFile.findElementAt(initialOffsets.leftOffset());
            PsiElement initElementAtRight = psiFile.findElementAt(initialOffsets.rightOffset() - 1);
            if (initElementAtLeft == null || initElementAtRight == null) {
                return Optional.of(initialOffsets);
            }
            if (initElementAtLeft.equals(initElementAtRight)) {
                initialElement = initElementAtLeft;
            } else {
                initialElement = findParentElementIfInitialElementsAreAtEdgesOrChooseOne(initElementAtLeft, initElementAtRight);
            }
        }
        if (initialElement != null && initialElement.getChildren().length == 0 &&
                (initialElement.getTextRange().getStartOffset() != initialOffsets.leftOffset() ||
                        initialElement.getTextRange().getEndOffset() != initialOffsets.rightOffset())) {
            Optional<Offsets> subWordOffset = findNextSubWord(initialOffsets, initialElement);
            if (subWordOffset.isPresent()) {
                return subWordOffset;
            }
        }
        initialElement = replaceWithParentIfParentEqualsTheElement(initialElement);
        if (initialElement == null) {
            return Optional.of(initialOffsets);
        }


        PsiElement nextElement = findNextNoneEmptyElement(initialElement);
        if (nextElement == null) {
            return Optional.of(initialOffsets);
        }

        TextRange nextElementTextRange = nextElement.getTextRange();
        return Optional.of(new Offsets(nextElementTextRange.getStartOffset(), nextElementTextRange.getEndOffset()));
    }


    private Optional<Offsets> findNextSubWord(Offsets initialOffsets, PsiElement initialElement) {
        int elementOffset = initialElement.getTextRange().getStartOffset();
        var subWordFinder = new SubWordFinder(direction);
        int offsetInParent = switch (direction) {
            case FORWARD -> initialOffsets.leftOffset() - elementOffset;
            case BACKWARD -> initialOffsets.rightOffset() - elementOffset - 1;
        };
        Offsets elementRelativeOffset = new Offsets(offsetInParent, offsetInParent);
        Offsets subWordLocalOffset = subWordFinder.findNext(elementRelativeOffset, initialElement.getText());
        Offsets nextSubWordLocalOffset = subWordFinder.findNext(subWordLocalOffset, initialElement.getText());

        boolean selectedTheSame = subWordLocalOffset.leftOffset() == nextSubWordLocalOffset.leftOffset() &&
                subWordLocalOffset.rightOffset() == nextSubWordLocalOffset.rightOffset() - 1 &&
                subWordLocalOffset.leftOffset() == subWordLocalOffset.rightOffset();
        if (!(subWordLocalOffset.equals(nextSubWordLocalOffset) || selectedTheSame) ||
                SubWordFinder.areThereSubwords(initialElement.getText())) {

            var subWordOffset = getPreviousSubWord(initialElement, nextSubWordLocalOffset);

            subWordOffset = new Offsets(
                    subWordOffset.leftOffset() + elementOffset,
                    subWordOffset.rightOffset() + elementOffset);
            return Optional.of(subWordOffset);
        }

        return Optional.empty();
    }

    private Offsets getPreviousSubWord(PsiElement initialElement, Offsets nextSubWordLocalOffset) {
        return switch (direction) {
            case BACKWARD -> {
                var subWordOppositeDirection = new SubWordFinder(Direction.FORWARD);
                Offsets prevSubWord = subWordOppositeDirection.findNext(
                        new Offsets(nextSubWordLocalOffset.leftOffset(), nextSubWordLocalOffset.leftOffset()),
                        initialElement.getText());
                yield new Offsets(
                        prevSubWord.leftOffset(),
                        nextSubWordLocalOffset.rightOffset());
            }
            case Direction.FORWARD -> {
                var subWordOppositeDirection = new SubWordFinder(Direction.BACKWARD);
                Offsets prevSubWord = subWordOppositeDirection.findNext(
                        new Offsets(nextSubWordLocalOffset.rightOffset() - 1, nextSubWordLocalOffset.rightOffset()),
                        initialElement.getText());
                yield new Offsets(
                        prevSubWord.rightOffset(),
                        nextSubWordLocalOffset.rightOffset());
            }

        };
    }

    private @NotNull PsiElement findParentElementIfInitialElementsAreAtEdgesOrChooseOne(
                                                    PsiElement initElementAtLeft, PsiElement initElementAtRight) {
        PsiElement initialElement = initElementAtLeft;
        PsiElement commonParent = PsiTreeUtil.findCommonParent(initElementAtLeft, initElementAtRight);
        if (commonParent == null) {
            return initialElement;
        }
        boolean areOurElementsAtTheEdges =
                commonParent.getTextRange().getStartOffset() == initElementAtLeft.getTextRange().getStartOffset() &&
                        commonParent.getTextRange().getEndOffset() == initElementAtRight.getTextRange().getEndOffset();
        if (areOurElementsAtTheEdges) {
            initialElement = commonParent;
        } else {
            initialElement = switch (direction) {
                case BACKWARD -> initElementAtLeft;
                case FORWARD -> initElementAtRight;
            };
        }
        return initialElement;
    }

    private @Nullable PsiElement replaceWithParentIfParentEqualsTheElement(PsiElement initialElement) {
        if (initialElement == null) {
            return null;
        }
        PsiElement parent = initialElement.getParent();
        while (parent != null && parent.getText().equals(initialElement.getText())) {
            initialElement = parent;
            parent = initialElement.getParent();
        }
        return initialElement;
    }

    private @Nullable PsiElement findNextNoneEmptyElement(PsiElement initialElement) {
        if (initialElement == null) {
            return null;
        }
        PsiElement nextElement = initialElement;
        do {
            nextElement = switch (direction) {
                case BACKWARD -> PsiTreeUtil.skipSiblingsBackward(nextElement, PsiWhiteSpace.class);
                case FORWARD -> PsiTreeUtil.skipSiblingsForward(nextElement, PsiWhiteSpace.class);
            };
        } while (nextElement != null && nextElement.getText().isEmpty());
        return nextElement;
    }


}
