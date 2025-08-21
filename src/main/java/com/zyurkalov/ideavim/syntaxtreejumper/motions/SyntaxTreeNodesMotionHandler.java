package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.NotNull;

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
            case FORWARD -> goForward(currentElement);
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

    private Optional<SyntaxNode> goForward(SyntaxNode currentElement) {
        SyntaxNode nextNonWhitespaceSibling = syntaxTree.findNextNonWhitespaceSibling(currentElement);
        return Optional.ofNullable(nextNonWhitespaceSibling == null ?
                syntaxTree.findFirstChildOfItsParent(currentElement) : nextNonWhitespaceSibling);
    }

    private Optional<SyntaxNode> goBackward(SyntaxNode currentElement) {
        SyntaxNode nextNonWhitespaceSibling = syntaxTree.findPreviousNonWhitespaceSibling(currentElement);
        return Optional.ofNullable(nextNonWhitespaceSibling == null ?
                syntaxTree.findLastChildOfItsParent(currentElement) : nextNonWhitespaceSibling);
    }


    /**
     * Expands the selection to the parent syntax node (Alt-o behaviour)
     */
    Optional<SyntaxNode> expandSelection(SyntaxNode currentElement, Offsets initialOffsets) {
        SyntaxNode targetElement;
        targetElement = currentElement;
        while (targetElement != null && !doesTargetFollowRequirements(currentElement, targetElement, initialOffsets)) {
            targetElement = syntaxTree.findParentThatIsNotEqualToTheNode(targetElement);
        }
        ;
        if (targetElement == null || targetElement.isPsiFile()) {
            targetElement = null;
        }
        return Optional.ofNullable(targetElement);

    }

    public boolean doesTargetFollowRequirements(SyntaxNode initialElement, SyntaxNode targetElement, Offsets initialOffsets) {
        return ( initialOffsets.leftOffset() == initialOffsets.rightOffset() || !targetElement.isEquivalentTo(initialElement) ) &&
                !targetElement.isWhitespace() && !targetElement.isBracket();
    }

    /**
     * Shrinks the selection to the largest meaningful child (Alt-i behaviour)
     */
    Optional<SyntaxNode> shrinkSelection(SyntaxNode currentElement, Offsets initialOffsets) {
        // Find the largest meaningful child that fits within the current selection
        List<SyntaxNode> candidateChildren = currentElement.getChildren();
        while (candidateChildren.size() == 1 &&
                candidateChildren.getFirst().getTextRange().equals(currentElement.getTextRange())) {
            candidateChildren = candidateChildren.getFirst().getChildren();
        }

        SyntaxNode foundElement  = null;
        for (SyntaxNode child : candidateChildren) {
            if (doesTargetFollowRequirements(currentElement, child, initialOffsets)) {
                foundElement = child;
                break;
            }
            Optional<SyntaxNode> found;
            if (shallGoDeeper(currentElement, child, initialOffsets)) {
                found = shrinkSelection(child, initialOffsets);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        if (foundElement == null || foundElement.isEquivalentTo(currentElement)) {
            return Optional.empty();
        }
        return Optional.of(foundElement);

    }

    public boolean shallGoDeeper(SyntaxNode initialElement, SyntaxNode currentElement, Offsets initialOffsets) {
        return false;
    }

}