package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.ElementWithSiblings;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.Nullable;

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
        SyntaxNode currentElement = syntaxTree.findCurrentElement(initialOffsets, MotionDirection.FORWARD);
        if (currentElement == null) {
            return Optional.of(initialOffsets);
        }
        Optional<SyntaxNode> foundElement = switch (direction) {
            case BACKWARD, FORWARD -> getBackwardOrForward(currentElement);
            case EXPAND -> expandSelection(currentElement, initialOffsets);
            case SHRINK -> shrinkSelection(currentElement, initialOffsets);
        };
        if (foundElement.isPresent()) {
            TextRange textRange = foundElement.get().getTextRange();
            return Optional.of(new Offsets(textRange.getStartOffset(), textRange.getEndOffset()));

        } else {
            return Optional.of(initialOffsets);
        }

    }

    private Optional<SyntaxNode> getBackwardOrForward(SyntaxNode currentElement) {
        TextRange textRange = currentElement.getTextRange();
        Offsets initialOffsets = new Offsets(textRange.getStartOffset(), textRange.getEndOffset());
        ElementWithSiblings elementWithSiblings = syntaxTree.findElementWithSiblings(initialOffsets, direction);
        if (elementWithSiblings.currentElement() == null) {
            return Optional.empty();
        }
        SyntaxNode nextElement = getNextElementFromSiblings(elementWithSiblings);
        return Optional.ofNullable(nextElement);

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
    Optional<SyntaxNode> expandSelection(SyntaxNode currentElement, Offsets initialOffsets) {

        SyntaxNode targetElement;
        if (initialOffsets.leftOffset() == initialOffsets.rightOffset()) {
            targetElement = currentElement;
        } else {
            targetElement = syntaxTree.findParentThatIsNotEqualToTheNode(currentElement);
        }
        if (targetElement == null || targetElement.isPsiFile()) {
            targetElement = null;
        }
        return Optional.ofNullable(targetElement);

    }

    /**
     * Shrinks the selection to the largest meaningful child (Alt-i behaviour)
     */
    Optional<SyntaxNode> shrinkSelection(SyntaxNode currentElement, Offsets initialOffsets) {
        // Find the largest meaningful child that fits within the current selection
        List<SyntaxNode> candidateChildren = currentElement.getChildren();
        while (candidateChildren.size() == 1 && candidateChildren.getFirst().getTextRange().equals(currentElement.getTextRange())) {
            candidateChildren = candidateChildren.getFirst().getChildren();
        }

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

        SyntaxNode foundElement = largestChild;
        if (foundElement == null || foundElement.isEquivalentTo(currentElement)) {
            return Optional.empty();
        }
        return Optional.of(foundElement);

    }

}