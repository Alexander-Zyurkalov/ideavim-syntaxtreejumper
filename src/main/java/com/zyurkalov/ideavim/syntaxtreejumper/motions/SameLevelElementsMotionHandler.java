package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.ElementWithSiblings;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SameLevelElementsMotionHandler implements MotionHandler {

    public final SyntaxTreeAdapter syntaxTree;
    private final Direction direction;

    public SameLevelElementsMotionHandler(SyntaxTreeAdapter syntaxTree, Direction direction) {
        this.syntaxTree = syntaxTree;
        this.direction = direction;
    }

    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {
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
        };
    }

}