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

public class SyntaxTreeNodesMotionHandler implements MotionHandler {

    public final SyntaxTreeAdapter syntaxTree;
    private final MotionDirection direction;

    public SyntaxTreeNodesMotionHandler(SyntaxTreeAdapter syntaxTree, MotionDirection direction) {
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

    // Factory methods for easier integration with your existing system
    public static SyntaxTreeNodesMotionHandler createExpandHandler(SyntaxTreeAdapter syntaxTree) {
        return new SyntaxTreeNodesMotionHandler(syntaxTree, MotionDirection.EXPAND);
    }

    public static SyntaxTreeNodesMotionHandler createShrinkHandler(SyntaxTreeAdapter syntaxTree) {
        return new SyntaxTreeNodesMotionHandler(syntaxTree, MotionDirection.SHRINK);
    }

    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {
        SyntaxNode currentElement = syntaxTree.findCurrentElement(initialOffsets, MotionDirection.FORWARD);
        if (currentElement == null) {
            return Optional.of(initialOffsets);
        }
        Optional<SyntaxNode> foundElement = switch (direction) {
            case BACKWARD -> goBackward(currentElement);
            case FORWARD -> goForward(currentElement, initialOffsets, true, currentElement);
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

    private Optional<SyntaxNode> goForward(SyntaxNode currentElement, Offsets initialOffsets, boolean skipFirstStep, SyntaxNode startingPoint) {
        SyntaxNode sibling = skipFirstStep ? getNextSibling(currentElement, startingPoint) : currentElement;
        while (sibling != null && !doesTargetFollowRequirements(currentElement, sibling, initialOffsets)) {
            if (shallGoDeeper() && !sibling.getChildren().isEmpty()) {
                var found = goForward(sibling.getFirstChild(), initialOffsets, false, startingPoint);
                if (found.isPresent()) {
                    return found;
                }
            }
            sibling = getNextSibling(sibling, startingPoint);
        }
        return Optional.ofNullable(sibling);
    }

    private @Nullable SyntaxNode getNextSibling(SyntaxNode element, SyntaxNode startingPoint) {
        SyntaxNode nextSibling = element.getNextSibling();
        if (!shallGoDeeper()) {
            if (nextSibling == null) {
                nextSibling = syntaxTree.findFirstChildOfItsParent(element);
            }
            if (nextSibling == null || nextSibling.isEquivalentTo(startingPoint)) {
                return null;
            }
        }
        return nextSibling;
    }

    private Optional<SyntaxNode> goBackward(SyntaxNode currentElement) {
        SyntaxNode nextNonWhitespaceSibling = syntaxTree.findPreviousNonWhitespaceSibling(currentElement);
        return Optional.ofNullable(nextNonWhitespaceSibling == null ?
                syntaxTree.findLastChildOfItsParent(currentElement) : nextNonWhitespaceSibling);
    }


    /**
     * Expands the selection to the parent syntax node (Alt-o behaviour)
     */
    private Optional<SyntaxNode> expandSelection(SyntaxNode initialElement, Offsets initialOffsets) {
        SyntaxNode targetElement;
        targetElement = initialElement;
        while (targetElement != null && !doesTargetFollowRequirements(initialElement, targetElement, initialOffsets)) {
            targetElement = syntaxTree.findParentThatIsNotEqualToTheNode(targetElement);
        }
        ;
        if (targetElement == null || targetElement.isPsiFile()) {
            targetElement = null;
        }
        return Optional.ofNullable(targetElement);

    }

    protected boolean doesTargetFollowRequirements(SyntaxNode initialElement, SyntaxNode targetElement, Offsets initialOffsets) {
        return (initialOffsets.leftOffset() == initialOffsets.rightOffset() || !targetElement.isEquivalentTo(initialElement)) &&
                !SyntaxTreeAdapter.isASymbolToSkip(targetElement);
    }

    /**
     * Shrinks the selection to the largest meaningful child (Alt-i behaviour)
     */
    private Optional<SyntaxNode> shrinkSelection(SyntaxNode initialElement, Offsets initialOffsets) {
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