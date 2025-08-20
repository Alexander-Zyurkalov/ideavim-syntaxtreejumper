package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

import static com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter.getChild;

public abstract class AbstractFindNodeMotionHandler implements MotionHandler {
    protected final SyntaxTreeAdapter syntaxTree;
    protected final MotionDirection direction;
    private final WhileSearching whileSearching;

    public AbstractFindNodeMotionHandler(SyntaxTreeAdapter syntaxTree, MotionDirection direction,
                                         WhileSearching whileSearching) {
        this.syntaxTree = syntaxTree;
        this.direction = direction;
        this.whileSearching = whileSearching;
    }

    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {
        SyntaxNode currentNode = syntaxTree.findCurrentElement(initialOffsets, direction);
        if (currentNode == null) {
            return Optional.empty();
        }
        if (initialOffsets.leftOffset() == initialOffsets.rightOffset()) {
            initialOffsets = new Offsets(currentNode.getTextRange().getStartOffset(), currentNode.getTextRange().getEndOffset());
        }

        // 2. Search for nodes based on a direction and searching criteria
        Optional<SyntaxNode> targetListNode = findNodeByDirection(
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
    public abstract Function<SyntaxNode, Optional<SyntaxNode>> createFunctionToCheckSearchingCriteria(
            MotionDirection direction, Offsets initialSelection, WhileSearching whileSearching);

    private static boolean isNodeReallyFound(MotionDirection direction, Offsets initialSelection, Optional<SyntaxNode> found) {
        return found.isPresent() &&
                found.get().isInRightDirection(initialSelection, direction) &&
                !found.get().areBordersEqual(initialSelection);
    }

    /**
     * Finds a PARAMETER_LIST or ARGUMENT_LIST node based on the direction from the current node.
     *
     * @param currentNode                   The starting node
     * @param direction                     The direction to search
     * @param initialSelection              The initial selection
     * @param functionToCheckSearchCriteria The function to find parameter nodes based on specific criteria
     * @return The found parameter/argument list node or null if not found
     */
    public Optional<SyntaxNode> findNodeByDirection(
            SyntaxNode currentNode, MotionDirection direction, Offsets initialSelection,
            @NotNull Function<SyntaxNode, Optional<SyntaxNode>> functionToCheckSearchCriteria, WhileSearching whileSearching) {
        Optional<SyntaxNode> found = findWithinNeighbours(
                currentNode, direction, initialSelection, functionToCheckSearchCriteria, whileSearching);

        while (found.isEmpty()) {
            currentNode = currentNode.getParent();
            if (currentNode == null || currentNode.isPsiFile()) {
                break;
            }
            found = findWithinNeighbours(currentNode, direction, initialSelection, functionToCheckSearchCriteria, whileSearching);
        }
        return found;
    }

    public enum WhileSearching {
        SKIP_INITIAL_SELECTION, DO_NOT_SKIP_INITIAL_SELECTION
    }

    public Optional<SyntaxNode> findWithinNeighbours(
            @NotNull SyntaxNode currentNode, MotionDirection direction, Offsets initialSelection,
            Function<SyntaxNode, Optional<SyntaxNode>> findNodeType,
            WhileSearching whileSearching) {
        Optional<SyntaxNode> found = Optional.empty();
        var next = Optional.of(currentNode);
        while (next.isPresent()) {
            currentNode = next.get();
            if (whileSearching == WhileSearching.SKIP_INITIAL_SELECTION && !currentNode.isInRightDirection(initialSelection, direction)) {
                next = nextNeighbour(currentNode, direction); //TODO: I think we need to move it here as a virtual method
                continue;
            }
            found = findNodeType.apply(currentNode);
            if (isNodeReallyFound(direction, initialSelection, found)
            ) {
                return found;
            }
            if (!currentNode.getChildren().isEmpty()) {
                found = findWithinNeighbours(
                        getChild(currentNode, direction), direction, initialSelection, findNodeType, WhileSearching.DO_NOT_SKIP_INITIAL_SELECTION);
                if (isNodeReallyFound(direction, initialSelection, found)
                ) {
                    return found;
                }
            }
            next = nextNeighbour(currentNode, direction);
        }
        return found;
    }

    // TODO: rename this method and combine with get child and getParent
    public Optional<SyntaxNode> nextNeighbour(SyntaxNode node, MotionDirection direction) {
        return switch (direction) {
            case FORWARD -> Optional.ofNullable(node.getNextSibling());
            case BACKWARD -> Optional.ofNullable(node.getPreviousSibling());
            case EXPAND -> Optional.ofNullable(node.getParent());
            case SHRINK -> Optional.ofNullable(node.getFirstChild());
        };
    }
}
