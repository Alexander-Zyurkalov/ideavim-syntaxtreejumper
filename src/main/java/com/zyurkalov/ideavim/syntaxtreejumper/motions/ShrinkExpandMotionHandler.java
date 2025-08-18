package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.NotNull;

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
    private final SameLevelElementsMotionHandler sameLevelElementsMotionHandler;

    public ShrinkExpandMotionHandler(SyntaxTreeAdapter syntaxTree, SyntaxNoteMotionType motionType) {
        this.syntaxTree = syntaxTree;
        this.motionType = motionType;
        this.sameLevelElementsMotionHandler = new SameLevelElementsMotionHandler(syntaxTree, Direction.FORWARD);
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

        if (targetElement == null  || targetElement.isPsiFile()) {
            return Optional.of(initialOffsets);
        }

        TextRange parentRange = targetElement.getTextRange();
        return Optional.of(new Offsets(parentRange.getStartOffset(), parentRange.getEndOffset()));
    }

    //TODO: move to subwords
    private static @NotNull Optional<Offsets> findSubWordToExpand(Offsets initialOffsets, SyntaxNode targetElement) {
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
        SyntaxNode currentElement = syntaxTree.findCurrentElement(initialOffsets, Direction.FORWARD);
        if (currentElement == null) {
            return Optional.of(initialOffsets);
        }

        // Find the largest meaningful child that fits within the current selection
        SyntaxNode childElement = sameLevelElementsMotionHandler.findLargestChildWithinSelection(currentElement, initialOffsets);
        if (childElement == null || childElement.isEquivalentTo(currentElement)) {
            return Optional.of(initialOffsets);
        }

        TextRange childRange = childElement.getTextRange();
        return Optional.of(new Offsets(childRange.getStartOffset(), childRange.getEndOffset()));
    }

    // Factory methods for easier integration with your existing system
    public static ShrinkExpandMotionHandler createExpandHandler(SyntaxTreeAdapter syntaxTree) {
        return new ShrinkExpandMotionHandler(syntaxTree, SyntaxNoteMotionType.EXPAND);
    }

    public static ShrinkExpandMotionHandler createShrinkHandler(SyntaxTreeAdapter syntaxTree) {
        return new ShrinkExpandMotionHandler(syntaxTree, SyntaxNoteMotionType.SHRINK);
    }
}