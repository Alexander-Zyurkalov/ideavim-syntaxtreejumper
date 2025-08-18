package com.zyurkalov.ideavim.syntaxtreejumper.motions;

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

    public final SyntaxNoteMotionType motionType;
    public final SameLevelElementsMotionHandler sameLevelElementsMotionHandler;

    public ShrinkExpandMotionHandler(SyntaxTreeAdapter syntaxTree, SyntaxNoteMotionType motionType) {
        this.motionType = motionType;
        this.sameLevelElementsMotionHandler = new SameLevelElementsMotionHandler(syntaxTree, Direction.FORWARD);
    }

    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {
        return switch (motionType) {
            case EXPAND -> sameLevelElementsMotionHandler.expandSelection(initialOffsets);
            case SHRINK -> sameLevelElementsMotionHandler.shrinkSelection(initialOffsets);
        };
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

    // Factory methods for easier integration with your existing system
    public static ShrinkExpandMotionHandler createExpandHandler(SyntaxTreeAdapter syntaxTree) {
        return new ShrinkExpandMotionHandler(syntaxTree, SyntaxNoteMotionType.EXPAND);
    }

    public static ShrinkExpandMotionHandler createShrinkHandler(SyntaxTreeAdapter syntaxTree) {
        return new ShrinkExpandMotionHandler(syntaxTree, SyntaxNoteMotionType.SHRINK);
    }
}