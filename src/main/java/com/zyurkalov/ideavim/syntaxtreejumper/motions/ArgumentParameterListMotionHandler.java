package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

/**
 * MotionHandler that finds PsiElements of type PARAMETER_LIST or ARGUMENT_LIST
 * in accordance to the given Direction from the caret, then places the caret
 * at the first child of that element.
 */
public class ArgumentParameterListMotionHandler implements MotionHandler {

    private final SyntaxTreeAdapter syntaxTree;
    private final Direction direction;

    public ArgumentParameterListMotionHandler(SyntaxTreeAdapter syntaxTree, Direction direction) {
        this.syntaxTree = syntaxTree;
        this.direction = direction;
    }

    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {
        // 1. Get the current element at the caret position
        SyntaxNode leftNode = syntaxTree.findNodeAt(initialOffsets.leftOffset());
        SyntaxNode rightNode = syntaxTree.findNodeAt(Math.max(initialOffsets.leftOffset(), initialOffsets.rightOffset() - 1));
        if (leftNode == null || rightNode == null) {
            return Optional.empty();
        }
        if (initialOffsets.leftOffset() == initialOffsets.rightOffset()) {
            initialOffsets = new Offsets(leftNode.getTextRange().getStartOffset(), leftNode.getTextRange().getEndOffset());
        }
        SyntaxNode currentNode = syntaxTree.findLargestParentWithinSelection(leftNode, rightNode, initialOffsets);
        if (currentNode == null) {
            return Optional.empty();
        }

        // 2. Search for PARAMETER_LIST or ARGUMENT_LIST based on a direction
        Optional<SyntaxNode> targetListNode = syntaxTree.findNodeByDirection(
                currentNode, direction, initialOffsets, createFunctionToFindNode(direction, initialOffsets));
        if (targetListNode.isPresent()) {
            // 3. Place caret at the first child
            SyntaxNode syntaxNode = targetListNode.get();
            if (syntaxNode.getTextRange() == null) {
                return Optional.empty();
            }
            return Optional.of(new Offsets(
                    syntaxNode.getTextRange().getStartOffset(),
                    syntaxNode.getTextRange().getEndOffset()
            ));
        }
        return Optional.empty();
    }

    @NotNull
    public Function<SyntaxNode, Optional<SyntaxNode>> createFunctionToFindNode(Direction direction, Offsets initialSelection) {
        return node -> {
            if (node.isFunctionParameter() || node.isFunctionArgument() || node.isTypeParameter()) {
                return Optional.of(node);
            }
            return Optional.empty();
        };
    }

}
