package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SameLevelElementsMotionHandler implements MotionHandler {

    private final SyntaxTreeAdapter syntaxTree;
    private final Direction direction;

    public SameLevelElementsMotionHandler(SyntaxTreeAdapter syntaxTree, Direction direction) {
        this.syntaxTree = syntaxTree;
        this.direction = direction;
    }

    /**
     * Data structure to hold an element and its siblings for reuse across classes.
     */
    public record ElementWithSiblings(SyntaxNode currentElement, SyntaxNode previousSibling, SyntaxNode nextSibling) {
    }

    /**
     * Public method to find the current element and its siblings based on the given offsets.
     * This method can be reused by other classes like PsiElementHighlighter.
     */
    public ElementWithSiblings findElementWithSiblings(Offsets initialOffsets) {
        SyntaxNode currentElement = syntaxTree.findCurrentElement(initialOffsets, direction);
        if (currentElement == null) {
            return new ElementWithSiblings(null, null, null);
        }

        currentElement = syntaxTree.replaceWithParentIfParentEqualsTheNode(currentElement);
        if (currentElement == null) {
            return new ElementWithSiblings(null, null, null);
        }

        SyntaxNode previousSibling = syntaxTree.findPreviousNonWhitespaceSibling(currentElement);
        SyntaxNode nextSibling = syntaxTree.findNextNonWhitespaceSibling(currentElement);

        return new ElementWithSiblings(currentElement, previousSibling, nextSibling);
    }

    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {
        ElementWithSiblings elementWithSiblings = findElementWithSiblings(initialOffsets);
        if (elementWithSiblings.currentElement == null) {
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
            case BACKWARD -> elementWithSiblings.previousSibling == null ?
                    syntaxTree.findLastChildOfItsParent(elementWithSiblings.currentElement) :
                    elementWithSiblings.previousSibling;
            case FORWARD -> elementWithSiblings.nextSibling == null ?
                    syntaxTree.findFirstChildOfItsParent(elementWithSiblings.currentElement) :
                    elementWithSiblings.nextSibling;
        };
    }

}