package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
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
public class ShrinkExpandMotionHandler implements MotionHandler {

    public enum SyntaxNoteMotionType {
        EXPAND,  // Alt-o: expand selection to parent
        SHRINK   // Alt-i: shrink selection to children
    }

    private final SyntaxTreeAdapter syntaxTree;
    private final SyntaxNoteMotionType motionType;

    public ShrinkExpandMotionHandler(SyntaxTreeAdapter syntaxTree, SyntaxNoteMotionType motionType) {
        this.syntaxTree = syntaxTree;
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
     * Expands the selection to the parent syntax node (Alt-o behavior)
     */
    private Optional<Offsets> expandSelection(Offsets initialOffsets) {
        // Find the current selection boundaries
        SyntaxNode leftElement = syntaxTree.findNodeAt(initialOffsets.leftOffset());
        SyntaxNode rightElement = syntaxTree.findNodeAt(Math.max(0, initialOffsets.rightOffset() - 1));

        if (leftElement == null || initialOffsets.rightOffset() >= syntaxTree.getDocumentLength()) {
            return Optional.of(initialOffsets);
        }

        // If we have a selection, find the common parent that encompasses it
        SyntaxNode targetElement;
        if (initialOffsets.leftOffset() == initialOffsets.rightOffset()) {
            targetElement = leftElement;
        } else {
            // Find the smallest common parent that encompasses the current selection
            targetElement = syntaxTree.findSmallestCommonParent(leftElement, rightElement, initialOffsets);
        }

        if (targetElement == null) {
            return Optional.of(initialOffsets);
        }

        TextRange parentRange = targetElement.getTextRange();
        return Optional.of(new Offsets(parentRange.getStartOffset(), parentRange.getEndOffset()));
    }

    private static @NotNull Optional<Offsets> findSubWordToExpand(Offsets initialOffsets, SyntaxNode targetElement) { //TODO: move to sub
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
        SyntaxNode leftElement = syntaxTree.findNodeAt(initialOffsets.leftOffset());
        SyntaxNode rightElement = syntaxTree.findNodeAt(initialOffsets.rightOffset() - 1);

        if (leftElement == null) {
            return Optional.of(initialOffsets);
        }

        SyntaxNode encompassingElement = syntaxTree.findSmallestCommonParent(leftElement, rightElement, initialOffsets);
        if (encompassingElement == null) {
            return Optional.of(initialOffsets);
        }

        // Find the largest meaningful child that fits within the current selection
        SyntaxNode childElement = findLargestChildWithinSelection(encompassingElement, initialOffsets);
        if (childElement == null || childElement.isEquivalentTo(encompassingElement)) {
            return Optional.of(initialOffsets);
        }

        TextRange childRange = childElement.getTextRange();
        return Optional.of(new Offsets(childRange.getStartOffset(), childRange.getEndOffset()));
    }

    /**
     * Finds the largest meaningful child that fits within the current selection
     */
    private @Nullable SyntaxNode findLargestChildWithinSelection(SyntaxNode parent, Offsets selection) {
        List<SyntaxNode> candidateChildren = new ArrayList<>();

        // Collect all meaningful children that fit within the selection
        collectChildrenWithinSelection(parent, selection, candidateChildren);

        // Find the largest child by text range
        SyntaxNode largestChild = null;
        int largestSize = 0;

        for (SyntaxNode child : candidateChildren) {
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
    private void collectChildrenWithinSelection(SyntaxNode element, Offsets selection, List<SyntaxNode> candidates) {
        for (SyntaxNode child : element.getChildren()) {
            if (child.isWhitespace()) {
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

    // Factory methods for easier integration with your existing system
    public static ShrinkExpandMotionHandler createExpandHandler(SyntaxTreeAdapter syntaxTree) {
        return new ShrinkExpandMotionHandler(syntaxTree, SyntaxNoteMotionType.EXPAND);
    }

    public static ShrinkExpandMotionHandler createShrinkHandler(SyntaxTreeAdapter syntaxTree) {
        return new ShrinkExpandMotionHandler(syntaxTree, SyntaxNoteMotionType.SHRINK);
    }
}