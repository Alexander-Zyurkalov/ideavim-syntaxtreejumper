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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implements Helix editor's Alt-o (expand selection) and Alt-i (shrink selection) motions.
 * <p>
 * Alt-o (EXPAND): Expands the current selection to encompass the parent syntax node
 * Alt-i (SHRINK): Shrinks the current selection to the largest child node that fits within the selection
 */
public class SyntaxNodeTreeHandler implements MotionHandler {

    public enum SyntaxNoteMotionType {
        EXPAND,  // Alt-o: expand selection to parent
        SHRINK   // Alt-i: shrink selection to children
    }

    private final PsiFile psiFile;
    private final SyntaxNoteMotionType motionType;

    public SyntaxNodeTreeHandler(PsiFile psiFile, SyntaxNoteMotionType motionType) {
        this.psiFile = psiFile;
        this.motionType = motionType;
    }

    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {
        return switch (motionType) {
            case EXPAND -> expandSelection(initialOffsets);
            case SHRINK -> shrinkSelection(initialOffsets);
        };
    }

    /**
     * Expands the selection to the parent syntax node (Alt-o behaviour)
     */
    private Optional<Offsets> expandSelection(Offsets initialOffsets) {
        // Find the current selection boundaries
        PsiElement leftElement = psiFile.findElementAt(initialOffsets.leftOffset());
        PsiElement rightElement = psiFile.findElementAt(Math.max(0, initialOffsets.rightOffset() - 1));

        if (leftElement == null || initialOffsets.rightOffset() >= psiFile.getFileDocument().getTextLength()) {
            return Optional.of(initialOffsets);
        }

        // If we have a selection, find the common parent that encompasses it
        PsiElement targetElement;
        if (initialOffsets.leftOffset() == initialOffsets.rightOffset()) {
            targetElement = leftElement;
            return findSubWordToExpand(initialOffsets, targetElement);
        } else {
            // Find the smallest common parent that encompasses the current selection
            targetElement = findSmallestCommonParent(leftElement, rightElement, initialOffsets);
        }

        if (targetElement == null) {
            return Optional.of(initialOffsets);
        }

        TextRange parentRange = targetElement.getTextRange();
        return Optional.of(new Offsets(parentRange.getStartOffset(), parentRange.getEndOffset()));
    }

    private static @NotNull Optional<Offsets> findSubWordToExpand(Offsets initialOffsets, PsiElement targetElement) {
        int elementOffset = targetElement.getTextRange().getStartOffset();
        int offsetInParent = initialOffsets.leftOffset() - elementOffset;
        Offsets elementRelativeOffset = new Offsets(offsetInParent, offsetInParent);
        SubWordFinder finderBackward = new SubWordFinder(Direction.BACKWARD);
        SubWordFinder finderForward = new SubWordFinder(Direction.FORWARD);
        Offsets left = finderBackward.findNext(elementRelativeOffset, targetElement.getText());
        Offsets right = finderForward.findNext(elementRelativeOffset, targetElement.getText());
        return Optional.of(new Offsets(left.leftOffset() + elementOffset, right.rightOffset() + elementOffset));
    }

    /**
     * Shrinks the selection to the largest meaningful child (Alt-i behavior)
     */
    private Optional<Offsets> shrinkSelection(Offsets initialOffsets) {
        // If there's no selection, can't shrink
        if (initialOffsets.leftOffset() >= initialOffsets.rightOffset()) {
            return Optional.of(initialOffsets);
        }

        // Find the element that encompasses the current selection
        PsiElement leftElement = psiFile.findElementAt(initialOffsets.leftOffset());
        PsiElement rightElement = psiFile.findElementAt(initialOffsets.rightOffset() - 1);

        if (leftElement == null) {
            return Optional.of(initialOffsets);
        }

        PsiElement encompassingElement = findSmallestCommonParent(leftElement, rightElement, initialOffsets);
        if (encompassingElement == null) {
            return Optional.of(initialOffsets);
        }

        // Find the largest meaningful child that fits within the current selection
        PsiElement childElement = findLargestChildWithinSelection(encompassingElement, initialOffsets);
        if (childElement == null || childElement.equals(encompassingElement)) {
            return findSubWordToShrink(initialOffsets, encompassingElement);
        }

        TextRange childRange = childElement.getTextRange();
        return Optional.of(new Offsets(childRange.getStartOffset(), childRange.getEndOffset()));
    }

    private static @NotNull Optional<Offsets> findSubWordToShrink(Offsets initialOffsets, PsiElement encompassingElement) {
        int elementOffset = encompassingElement.getTextRange().getStartOffset();
        int offsetInParent = initialOffsets.rightOffset() - elementOffset - 1;
        SubWordFinder finderBackward = new SubWordFinder(Direction.BACKWARD);
        Offsets elementRelativeOffset = new Offsets(offsetInParent, offsetInParent);
        Offsets left = finderBackward.findNext(elementRelativeOffset, encompassingElement.getText());
        return Optional.of(new Offsets(left.leftOffset() + elementOffset, left.rightOffset() + elementOffset));
    }

    /**
     * Finds the smallest common parent that fully encompasses the current selection
     */
    private @Nullable PsiElement findSmallestCommonParent(PsiElement leftElement, PsiElement rightElement, Offsets selection) {
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


    /**
     * Finds the largest meaningful child that fits within the current selection
     */
    private @Nullable PsiElement findLargestChildWithinSelection(PsiElement parent, Offsets selection) {
        List<PsiElement> candidateChildren = new ArrayList<>();

        // Collect all meaningful children that fit within the selection
        collectChildrenWithinSelection(parent, selection, candidateChildren);

        // Find the largest child by text range
        PsiElement largestChild = null;
        int largestSize = 0;

        for (PsiElement child : candidateChildren) {
            TextRange range = child.getTextRange();
            int size = range.getLength();

            if (size > largestSize) {
                largestSize = size;
                largestChild = child;
            }
        }

        return largestChild;
    }

    /**
     * Recursively collects children that fit within the selection
     */
    private void collectChildrenWithinSelection(PsiElement element, Offsets selection, List<PsiElement> candidates) {
        for (PsiElement child : element.getChildren()) {
            if (child instanceof PsiWhiteSpace) {
                continue;
            }

            TextRange childRange = child.getTextRange();

            // Check if the child fits completely within the selection
            if (childRange.getStartOffset() >= selection.leftOffset() &&
                    childRange.getEndOffset() <= selection.rightOffset()) {

                // If the child is smaller than the current selection, it's a candidate
                if (childRange.getStartOffset() > selection.leftOffset() ||
                        childRange.getEndOffset() < selection.rightOffset()) {
                    candidates.add(child);
                }

                // Also check this child's children
                collectChildrenWithinSelection(child, selection, candidates);
            }
        }
    }

    /**
     * Checks if an element contains only whitespace
     */
    private boolean isWhitespaceOnly(PsiElement element) {
        String text = element.getText();
        return text.trim().isEmpty();
    }

    // Factory methods for easier integration with your existing system
    public static SyntaxNodeTreeHandler createExpandHandler(PsiFile psiFile) {
        return new SyntaxNodeTreeHandler(psiFile, SyntaxNoteMotionType.EXPAND);
    }

    public static SyntaxNodeTreeHandler createShrinkHandler(PsiFile psiFile) {
        return new SyntaxNodeTreeHandler(psiFile, SyntaxNoteMotionType.SHRINK);
    }
}