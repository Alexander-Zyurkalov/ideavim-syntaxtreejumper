package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection.BACKWARD;
import static com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection.FORWARD;
import static com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter.getChild;

public abstract class AbstractSyntaxTreeNodesMotionHandler implements MotionHandler {

    public final SyntaxTreeAdapter syntaxTree;
    private final MotionDirection direction;

    public AbstractSyntaxTreeNodesMotionHandler(SyntaxTreeAdapter syntaxTree, MotionDirection direction) {
        this.syntaxTree = syntaxTree;
        this.direction = direction;
    }

    //TODO: move to subwords
    private static @NotNull Optional<Offsets> findSubWordToExpand(Offsets initialOffsets, SyntaxNode targetElement) {
        int elementOffset = targetElement.getTextRange().getStartOffset();
        int offsetInParent = initialOffsets.leftOffset() - elementOffset;
        Offsets elementRelativeOffset = new Offsets(offsetInParent, offsetInParent);
        SubWordFinder finderBackward = new SubWordFinder(BACKWARD);
        SubWordFinder finderForward = new SubWordFinder(FORWARD);
        Offsets left = finderBackward.findNext(elementRelativeOffset, targetElement.getText());
        Offsets right = finderForward.findNext(elementRelativeOffset, targetElement.getText());
        return Optional.of(new Offsets(left.leftOffset() + elementOffset, right.rightOffset() + elementOffset));
    }


    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {
        SyntaxNode currentElement = syntaxTree.findCurrentElement(initialOffsets, MotionDirection.FORWARD);
        if (currentElement == null) {
            return Optional.of(initialOffsets);
        }
        Optional<SyntaxNode> foundElement = switch (direction) {
            case BACKWARD -> goBackwardOrForward(currentElement, initialOffsets, true, currentElement, BACKWARD);
            case FORWARD -> goBackwardOrForward(currentElement, initialOffsets, true, currentElement, FORWARD);
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

    protected Optional<SyntaxNode> goBackwardOrForward(SyntaxNode currentElement, Offsets initialOffsets,
                                                       boolean skipFirstStep, SyntaxNode startingPoint,
                                                       MotionDirection motionDirection
    ) {
        SyntaxNode found = findWithinNeighbours(
                currentElement, initialOffsets, skipFirstStep, startingPoint, motionDirection);
        while (shallGoDeeper() && found == null && currentElement != null) {
            currentElement = currentElement.getParent();
            if (currentElement == null || currentElement.isPsiFile()) {
                break;
            }
            found = findWithinNeighbours(currentElement, initialOffsets, true, startingPoint, motionDirection);
        }
        return Optional.ofNullable(found);
    }

    private @Nullable SyntaxNode findWithinNeighbours(SyntaxNode currentElement, Offsets initialOffsets,
                                                      boolean skipFirstStep, SyntaxNode startingPoint,
                                                      MotionDirection motionDirection
    ) {
        SyntaxNode sibling = skipFirstStep ?
                getNextSibling(currentElement, startingPoint, motionDirection) :
                currentElement;
        boolean isFirstStep = true;
        while (
                sibling != null && !doesTargetFollowRequirements(startingPoint, sibling, initialOffsets)
        ) {
            if (shallGoDeeper() && !sibling.getChildren().isEmpty()) {
                var found = findWithinNeighbours(
                        getChild(sibling, motionDirection), initialOffsets, false, startingPoint, motionDirection);
                if (found != null) {
                    sibling = found;
                    break;
                }
            }
            sibling = getNextSibling(sibling, startingPoint, motionDirection);
            if (!isFirstStep && sibling != null && sibling.areBordersEqual(initialOffsets)) {
                break;
            }
            isFirstStep = false;
        }
        return sibling;
    }

    private @Nullable SyntaxNode getNextSibling(SyntaxNode element, SyntaxNode startingPoint,
                                                MotionDirection motionDirection) {
        SyntaxNode nextSibling = motionDirection == FORWARD ? element.getNextSibling() : element.getPreviousSibling();
        if (!shallGoDeeper()) {
            if (nextSibling == null) {
                nextSibling = motionDirection == FORWARD ?
                        syntaxTree.findFirstChildOfItsParent(element) :
                        syntaxTree.findLastChildOfItsParent(element);
            }
            if (nextSibling == null || nextSibling.isEquivalentTo(startingPoint)) {
                return null;
            }
        }
        return nextSibling;
    }


    /**
     * Expands the selection to the parent syntax node (Alt-o behaviour)
     */
    protected Optional<SyntaxNode> expandSelection(SyntaxNode initialElement, Offsets initialOffsets) {
        SyntaxNode targetElement;
        targetElement = initialElement;
        if (initialElement == null) {
            return Optional.empty();
        }
        while (((targetElement.getTextRange() != null && targetElement.getTextRange().equals(initialElement.getTextRange())) ||
                (!doesTargetFollowRequirements(initialElement, targetElement, initialOffsets)))
        ) {
            targetElement = targetElement.getParent();
            if (targetElement == null || targetElement.areBordersEqual(initialOffsets)) {
                break;
            }
        }
        if (targetElement == null || targetElement.isPsiFile()) {
            targetElement = null;
        }
        return Optional.ofNullable(targetElement);
    }

    protected boolean doesTargetFollowRequirements(SyntaxNode startingPoint, SyntaxNode targetElement, Offsets initialOffsets) {
        return (initialOffsets.leftOffset() == initialOffsets.rightOffset() || !targetElement.isEquivalentTo(startingPoint)) &&
                !SyntaxTreeAdapter.isASymbolToSkip(targetElement);
    }

    /**
     * Shrinks the selection to the largest meaningful child (Alt-i behaviour)
     */
    protected Optional<SyntaxNode> shrinkSelection(SyntaxNode initialElement, Offsets initialOffsets) {
        // Find the largest meaningful child that fits within the current selection
        List<SyntaxNode> candidateChildren = initialElement.getChildren();
        while (candidateChildren.size() == 1 &&
                candidateChildren.getFirst().getTextRange().equals(initialElement.getTextRange())) {
            candidateChildren = candidateChildren.getFirst().getChildren();
        }

        SyntaxNode foundElement = null;
        for (SyntaxNode child : candidateChildren) {
            if (doesTargetFollowRequirements(initialElement, child, initialOffsets)) {
                foundElement = child;
                break;
            }
            Optional<SyntaxNode> found;
            if (shallGoDeeper()) {
                found = shrinkSelection(child, initialOffsets);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        if (foundElement == null || foundElement.isEquivalentTo(initialElement)) {
            return Optional.empty();
        }
        return Optional.of(foundElement);

    }

    protected boolean shallGoDeeper() {
        return false;
    }

}