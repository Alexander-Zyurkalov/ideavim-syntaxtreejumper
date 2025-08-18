package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

public abstract class AbstractFindNodeMotionHandler implements MotionHandler {
    protected final SyntaxTreeAdapter syntaxTree;
    protected final Direction direction;
    private SyntaxTreeAdapter.WhileSearching whileSearching;

    public AbstractFindNodeMotionHandler(SyntaxTreeAdapter syntaxTree, Direction direction,
                                         SyntaxTreeAdapter.WhileSearching whileSearching) {
        this.syntaxTree = syntaxTree;
        this.direction = direction;
        this.whileSearching = whileSearching;
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

        // 2. Search for nodes based on a direction and searching criteria
        Optional<SyntaxNode> targetListNode = syntaxTree.findNodeByDirection(
                currentNode, direction, initialOffsets,
                createFunctionToCheckSearchingCriteria(direction, initialOffsets, whileSearching), 
                whileSearching);
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
    public abstract Function<SyntaxNode, Optional<SyntaxNode>> createFunctionToCheckSearchingCriteria(Direction direction, Offsets initialSelection, SyntaxTreeAdapter.WhileSearching whileSearching);
}
