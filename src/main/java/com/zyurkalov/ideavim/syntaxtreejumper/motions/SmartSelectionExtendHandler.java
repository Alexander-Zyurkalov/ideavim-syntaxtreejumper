package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Handler for smart selection extending (A-e shortcut).
 * When a syntax node is selected, extends the selection to include syntactically related
 * neighbours to form correct code. For example, selecting "2" in "1 + 2" will extend
 * to select "+ " as well to form "2 + " or "+ 2" depending on context.
 */
public class SmartSelectionExtendHandler implements MotionHandler {

    private final SyntaxTreeAdapter syntaxTree;

    public SmartSelectionExtendHandler(SyntaxTreeAdapter syntaxTree) {
        this.syntaxTree = syntaxTree;
    }

    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {
        // If no selection, start by selecting the current node
        if (initialOffsets.leftOffset() == initialOffsets.rightOffset()) {
            SyntaxNode nodeAtCursor = syntaxTree.findNodeAt(initialOffsets.leftOffset());
            if (nodeAtCursor != null) {
                initialOffsets = new Offsets(
                        nodeAtCursor.getTextRange().getStartOffset(),
                        nodeAtCursor.getTextRange().getEndOffset()
                );
            } else
                return Optional.of(initialOffsets);
        }

        // Find the currently selected node
        SelectedNodeAndDirection selectedNodeAndDirection = findSelectedNodeAndDirection(initialOffsets);
        SyntaxNode selectedNode = selectedNodeAndDirection.node;
        if (selectedNode == null) {
            return Optional.of(initialOffsets);
        }
        initialOffsets = new Offsets(
                selectedNode.getTextRange().getStartOffset(), selectedNode.getTextRange().getEndOffset());

        // Try to extend selection to include syntactically related elements
        Offsets extendedOffsets = extendSelectionSyntactically(
                selectedNode, initialOffsets, selectedNodeAndDirection.direction);
        return Optional.of(extendedOffsets);
    }

    record SelectedNodeAndDirection(SyntaxNode node, Direction direction) {
    }


    private @NotNull SelectedNodeAndDirection findSelectedNodeAndDirection(Offsets selection) {
        SyntaxNode leftNode = syntaxTree.findNodeAt(selection.leftOffset());
        SyntaxNode rightNode = syntaxTree.findNodeAt(selection.rightOffset() - 1);

        if (leftNode == null) {
            return new SelectedNodeAndDirection(null, Direction.FORWARD);
        }

        // Check if the selection exactly matches a node
        SyntaxNode current = leftNode;
        while (current != null && current.getTextRange() != null) {
            if (current.getTextRange().getStartOffset() == selection.leftOffset() &&
                    current.getTextRange().getEndOffset() == selection.rightOffset()) {
                return new SelectedNodeAndDirection(current, Direction.FORWARD);
            }
            current = current.getParent();
            // TODO: if current range wider than section, stop
        }
        if (rightNode == null || current == null) {
            return new SelectedNodeAndDirection(leftNode, Direction.FORWARD);
        }
        leftNode = syntaxTree.replaceWithParentIfParentEqualsTheNode(leftNode);

        Direction direction = Direction.FORWARD;
        while (leftNode != null && (leftNode.isOperator() || leftNode.isWhitespace() || leftNode.isBracket())) {
            leftNode = leftNode.getNextSibling();
        }

        rightNode = syntaxTree.replaceWithParentIfParentEqualsTheNode(rightNode);
        while (rightNode != null && (rightNode.isOperator() || rightNode.isWhitespace() || rightNode.isBracket())) {
            if (rightNode.isOperator()) {
                direction = Direction.BACKWARD;
            }
            rightNode = rightNode.getPreviousSibling();
        }


        if (leftNode == null || rightNode == null) {
            return new SelectedNodeAndDirection(null, Direction.FORWARD);
        }
        leftNode = combineIfInRange(leftNode, rightNode);
        rightNode = combineIfInRange(rightNode, leftNode);

        SyntaxNode parent = syntaxTree.findCommonParent(leftNode, rightNode);


        return new SelectedNodeAndDirection(parent, direction);
    }

    private static @NotNull SyntaxNode combineIfInRange(SyntaxNode leftNode, SyntaxNode rightNode) {
        if (leftNode.getTextRange().getStartOffset() >= rightNode.getTextRange().getStartOffset() &&
                leftNode.getTextRange().getEndOffset() <= rightNode.getTextRange().getEndOffset()
        ) {
           leftNode = rightNode;
        }
        return leftNode;
    }

    private static boolean isFindResultWithinSelection(Offsets selection, SyntaxNode syntaxNode) {
        return syntaxNode.getTextRange().getStartOffset() >= selection.leftOffset() &&
                syntaxNode.getTextRange().getEndOffset() <= selection.rightOffset();
    }

    @NotNull
    private Offsets extendSelectionSyntactically(@NotNull SyntaxNode selectedNode,
                                                 @NotNull Offsets currentSelectionOffset,
                                                 @NotNull Direction direction
    ) {
        selectedNode = syntaxTree.replaceWithParentIfParentEqualsTheNode(selectedNode);
        if (selectedNode == null) {
            return currentSelectionOffset;
        }
        SyntaxNode parent = selectedNode.getParent();
        if (parent == null) {
            return currentSelectionOffset;
        }

        Offsets compoundExpressionOffset = tryExtendCompoundExpression(selectedNode, currentSelectionOffset, direction);
        if (!compoundExpressionOffset.equals(currentSelectionOffset)) {
            return compoundExpressionOffset;
        }

        return currentSelectionOffset;
    }


    @NotNull
    private Offsets tryExtendCompoundExpression(@NotNull SyntaxNode selectedNode,
                                                @NotNull Offsets currentSelection,
                                                @NotNull Direction direction
    ) {
        SyntaxNode parent = selectedNode.getParent();
        if (parent == null) {
            return currentSelection;
        }

        if (parent.isCompoundExpression()) {
            return extendCompoundExpression(selectedNode, currentSelection, direction);
        }

        return currentSelection;
    }

    private record FindResult(SyntaxNode whiteSpace, SyntaxNode syntaxNode, boolean isWhiteSpaceFound,
                              boolean isNodeFound) {
    }

    @NotNull
    private Offsets extendCompoundExpression(
            @NotNull SyntaxNode selectedNode, @NotNull Offsets currentSelection, @NotNull Direction direction) {

        Offsets newSelection = currentSelection;

        // Extend forward: include the next operator
        FindResult nextOperatorResult = findSyntaxNodeSkippingWhitespacesOnly(selectedNode, direction, false);
        if (nextOperatorResult.isNodeFound) {
            newSelection = updateOffsetsFromNode(newSelection, nextOperatorResult.syntaxNode, direction);

            FindResult nextWhiteSpaceResult = findSyntaxNodeSkippingWhitespacesOnly(
                    nextOperatorResult.syntaxNode, direction, false);
            if (nextWhiteSpaceResult.isWhiteSpaceFound) {
                newSelection = updateOffsetsFromNode(newSelection, nextWhiteSpaceResult.whiteSpace, direction);
            }
        }

        // Extend backward: include the previous operator
        Direction oppositeDirection = getOppositeDirection(direction);
        FindResult prevOperatorResult = findSyntaxNodeSkippingWhitespacesOnly(
                selectedNode, oppositeDirection, false);
        if (prevOperatorResult.isNodeFound && !nextOperatorResult.isNodeFound) {
            // Note: Here we extend in the opposite direction relative to what we found
            newSelection = updateOffsetsFromNode(newSelection, prevOperatorResult.syntaxNode, oppositeDirection);

            FindResult prevWhiteSpaceResult = findSyntaxNodeSkippingWhitespacesOnly(
                    prevOperatorResult.syntaxNode(), oppositeDirection, false);
            if (prevWhiteSpaceResult.isWhiteSpaceFound) {
                newSelection = updateOffsetsFromNode(newSelection, prevWhiteSpaceResult.whiteSpace, oppositeDirection);
            }
        }

        return newSelection;
    }

    /**
     * Updates the selection offsets based on the given syntax node and direction.
     *
     * @param currentSelection Current selection offsets
     * @param node             The syntax node to incorporate
     * @param direction        The direction of extension (FORWARD extends right boundary, BACKWARD extends left boundary)
     * @return Updated selection with the node incorporated
     */
    private @NotNull Offsets updateOffsetsFromNode(@NotNull Offsets currentSelection,
                                                   @NotNull SyntaxNode node,
                                                   @NotNull Direction direction) {
        return switch (direction) {
            case Direction.FORWARD -> new Offsets(currentSelection.leftOffset(), node.getTextRange().getEndOffset());
            case Direction.BACKWARD ->
                    new Offsets(node.getTextRange().getStartOffset(), currentSelection.rightOffset());
        };
    }

    private static @NotNull Direction getOppositeDirection(Direction direction) {
        return direction == Direction.FORWARD ? Direction.BACKWARD : Direction.FORWARD;
    }

    private @NotNull FindResult findSyntaxNodeSkippingWhitespacesOnly(
            @NotNull SyntaxNode startingNode, @NotNull Direction direction,
            boolean searchIncludingTheFirstElement) {
        UnaryOperator<SyntaxNode> nextSyntaxNode = switch (direction) {
            case Direction.BACKWARD -> SyntaxNode::getPreviousSibling;
            case Direction.FORWARD -> SyntaxNode::getNextSibling;
        };
        boolean isWhiteSpaceFound = false;
        boolean isOperatorFound = true;
        SyntaxNode current = searchIncludingTheFirstElement ? startingNode : nextSyntaxNode.apply(startingNode);
        SyntaxNode whiteSpace = null;
        while (true) {
            if (current == null || isTheLastElementABracket(current, nextSyntaxNode)) {
                isOperatorFound = false;
                break;
            }
            if (!current.isWhitespace()) {
                break; // we found an operator
            }
            isWhiteSpaceFound = true;
            whiteSpace = current;
            current = nextSyntaxNode.apply(current);
        }
        current = syntaxTree.replaceWithParentIfParentEqualsTheNode(current);
        return new FindResult(whiteSpace, current, isWhiteSpaceFound, isOperatorFound);
    }

    private static boolean isTheLastElementABracket(SyntaxNode current, UnaryOperator<SyntaxNode> nextSyntaxNode) {
        return current.isBracket() && nextSyntaxNode.apply(current) == null;
    }

}
