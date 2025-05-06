package com.zyurkalov.ideavim.syntaxtreejumper;
import com.github.weisj.jsvg.ca;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SameLevelElementsNavigator {

    public enum Direction {
        BACKWARD,
        FORWARD,
    }

    private final PsiFile psiFile;
    private final Direction direction;

    public SameLevelElementsNavigator(PsiFile psiFile, Direction direction) {
        this.psiFile = psiFile;
        this.direction = direction;
    }

    public Optional<Offsets> findNextObjectsOffsets(Offsets initialOffsets) {

        PsiElement initialElement;
        boolean isOnlyCaretButNoSelection = initialOffsets.leftOffset() >= initialOffsets.rightOffset() - 1;
        if (isOnlyCaretButNoSelection) {
            initialElement = psiFile.findElementAt(initialOffsets.leftOffset());
            Optional<Offsets> nextSubWordOffsets = findNextSubWord(initialOffsets.leftOffset(), initialElement);
            if ( nextSubWordOffsets.isPresent() ) {
                return nextSubWordOffsets;
            }
        }
        else {
            PsiElement initElementAtLeft = psiFile.findElementAt(initialOffsets.leftOffset());
            PsiElement initElementAtRight = psiFile.findElementAt(initialOffsets.rightOffset() - 1);
            if (initElementAtLeft == null || initElementAtRight == null) {
                return Optional.of(initialOffsets);
            }
            if (initElementAtLeft.equals(initElementAtRight)) {
                initialElement = initElementAtLeft;
            } else {
                initialElement = findParentElementIfInitialElementsAreAtEdgesOrChoseOne(initElementAtLeft, initElementAtRight);
            }
        }

        initialElement = replaceWithParentIfParentEqualsTheElement(initialElement);
        PsiElement nextElement = findNextNoneEmptyElement(initialElement);
        if (nextElement == null) {
            return Optional.of(initialOffsets);
        }

        TextRange nextElementTextRange = nextElement.getTextRange();
        return Optional.of(new Offsets(nextElementTextRange.getStartOffset(), nextElementTextRange.getEndOffset()));
    }

    public static Optional<Offsets> findNextSubWord(int position, PsiElement lowestLevelElement) {
        if (lowestLevelElement == null)
            return Optional.empty();
        String elementText = lowestLevelElement.getText();
        int strPosition = position - lowestLevelElement.getTextOffset();
        boolean IsPrevCharUpperCase = Character.isUpperCase(elementText.charAt(0));
        List<Integer> caseSwitchers = new ArrayList<>();
        for (int i = 1; i < elementText.length(); i++) {
            boolean isCurCharUpperCase = Character.isUpperCase(elementText.charAt(i));
            if (isCurCharUpperCase != IsPrevCharUpperCase) {
                caseSwitchers.add(1);
            }
            else {
                caseSwitchers.set(caseSwitchers.size() - 1, caseSwitchers.getLast() + 1);
            }

        }
        return Optional.empty();
    }

    private @NotNull PsiElement findParentElementIfInitialElementsAreAtEdgesOrChoseOne(PsiElement initElementAtLeft, PsiElement initElementAtRight) {
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
        }
        else {
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
