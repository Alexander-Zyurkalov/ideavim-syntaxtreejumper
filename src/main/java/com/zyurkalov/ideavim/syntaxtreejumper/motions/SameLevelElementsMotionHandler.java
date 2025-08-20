package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.ElementWithSiblings;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//TODO: finish combining with ShrinkExpandMotionHandler
public class SameLevelElementsMotionHandler implements MotionHandler {

    public final SyntaxTreeAdapter syntaxTree;
    private final MotionDirection direction;

    public SameLevelElementsMotionHandler(SyntaxTreeAdapter syntaxTree, MotionDirection direction) {
        this.syntaxTree = syntaxTree;
        this.direction = direction;
    }

    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {
        return switch (direction) {
            case BACKWARD, FORWARD -> getBackwardOrForward(initialOffsets);
            case EXPAND -> expandSelection(initialOffsets);
            case SHRINK -> shrinkSelection(initialOffsets);
        };
    }

    private @NotNull Optional<Offsets> getBackwardOrForward(Offsets initialOffsets) {
        ElementWithSiblings elementWithSiblings = syntaxTree.findElementWithSiblings(initialOffsets, direction);
        if (elementWithSiblings.currentElement() == null) {
            return Optional.of(initialOffsets);
        }

        SyntaxNode nextElement = getNextElementFromSiblings(elementWithSiblings);
        if (nextElement == null) {
            return Optional.of(initialOffsets);
        }

        TextRange nextElementTextRange = nextElement.getTextRange();
        return Optional.of(new Offsets(nextElementTextRange.getStartOffset(), nextElementTextRange.getEndOffset()));
    }

    /**
     * Gets the next element based on a direction from the ElementWithSiblings.
     */
    private @Nullable SyntaxNode getNextElementFromSiblings(ElementWithSiblings elementWithSiblings) {
        return switch (direction) {
            case BACKWARD -> elementWithSiblings.previousSibling() == null ?
                    syntaxTree.findLastChildOfItsParent(elementWithSiblings.currentElement()) :
                    elementWithSiblings.previousSibling();
            case FORWARD -> elementWithSiblings.nextSibling() == null ?
                    syntaxTree.findFirstChildOfItsParent(elementWithSiblings.currentElement()) :
                    elementWithSiblings.nextSibling();
            case EXPAND -> null; //TODO: come up with better options
            case SHRINK -> null;
        };
    }

    /**
     * Expands the selection to the parent syntax node (Alt-o behaviour)
     */
    Optional<Offsets> expandSelection(Offsets initialOffsets) {
        // Find the current selection boundaries
        var currentElement = syntaxTree.findCurrentElement(initialOffsets, direction);
        if (currentElement == null) {
            return Optional.of(initialOffsets);
        }
        // If we have a selection, find the common parent that encompasses it
        SyntaxNode targetElement;
        if (initialOffsets.leftOffset() == initialOffsets.rightOffset()) {
            targetElement = currentElement;
        } else {
            // Find the smallest common parent that encompasses the current selection
            targetElement = syntaxTree.findParentThatIsNotEqualToTheNode(currentElement);
        }

        if (targetElement == null || targetElement.isPsiFile()) {
            return Optional.of(initialOffsets);
        }

        TextRange parentRange = targetElement.getTextRange();
        return Optional.of(new Offsets(parentRange.getStartOffset(), parentRange.getEndOffset()));
    }

    /**
     * Shrinks the selection to the largest meaningful child (Alt-i behaviour)
     */
    Optional<Offsets> shrinkSelection(Offsets initialOffsets) {
        SyntaxNode currentElement = syntaxTree.findCurrentElement(initialOffsets, MotionDirection.FORWARD);
        if (currentElement == null) {
            return Optional.of(initialOffsets);
        }

        // Find the largest meaningful child that fits within the current selection
        SyntaxNode childElement = findChildWithinSelection(currentElement, initialOffsets);
        if (childElement == null || childElement.isEquivalentTo(currentElement)) {
            return Optional.of(initialOffsets);
        }

        TextRange childRange = childElement.getTextRange();
        return Optional.of(new Offsets(childRange.getStartOffset(), childRange.getEndOffset()));
    }

    /**
     * Finds the largest meaningful child that fits within the current selection
     */
    @Nullable SyntaxNode findChildWithinSelection(SyntaxNode parent, Offsets selection) {
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
    void collectChildrenWithinSelection(SyntaxNode element, Offsets selection, List<SyntaxNode> candidates) {
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

            }
        }
    }
}