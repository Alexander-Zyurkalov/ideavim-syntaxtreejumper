package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.NotNull;
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
        SyntaxNode currentElement = findCurrentElement(initialOffsets);
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
     * Finds the current syntax node based on the given offsets.
     * Extracted from the original findNext method for reuse.
     */
    private @Nullable SyntaxNode findCurrentElement(Offsets initialOffsets) {
        boolean isOnlyCaretButNoSelection = initialOffsets.leftOffset() >= initialOffsets.rightOffset() - 1;

        if (isOnlyCaretButNoSelection) {
            return syntaxTree.findNodeAt(initialOffsets.leftOffset());
        } else {
            SyntaxNode initElementAtLeft = syntaxTree.findNodeAt(initialOffsets.leftOffset());
            SyntaxNode initElementAtRight = syntaxTree.findNodeAt(initialOffsets.rightOffset() - 1);
            if (initElementAtLeft == null || initElementAtRight == null) {
                return null;
            }
            if (initElementAtLeft.isEquivalentTo(initElementAtRight)) {
                return initElementAtLeft;
            } else {
                return findParentElementIfInitialElementsAreAtEdgesOrChooseOne(initElementAtLeft, initElementAtRight);
            }
        }
    }

    /**
     * Gets the next element based on a direction from the ElementWithSiblings.
     */
    private @Nullable SyntaxNode getNextElementFromSiblings(ElementWithSiblings elementWithSiblings) {
        return switch (direction) {
            case BACKWARD -> elementWithSiblings.previousSibling;
            case FORWARD -> elementWithSiblings.nextSibling;
        };
    }

    private @NotNull SyntaxNode findParentElementIfInitialElementsAreAtEdgesOrChooseOne(
            SyntaxNode initElementAtLeft, SyntaxNode initElementAtRight) {
        SyntaxNode initialElement = initElementAtLeft;
        SyntaxNode commonParent = syntaxTree.findCommonParent(initElementAtLeft, initElementAtRight);
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
}