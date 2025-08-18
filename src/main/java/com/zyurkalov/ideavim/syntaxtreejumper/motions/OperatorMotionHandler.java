package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

/**
 * MotionHandler that finds operator nodes within compound expressions
 * in accordance to the given Direction from the caret.
 */
public class OperatorMotionHandler extends AbstractFindNodeMotionHandler {

    public OperatorMotionHandler(SyntaxTreeAdapter syntaxTree, Direction direction) {
        super(syntaxTree, direction, SyntaxTreeAdapter.WhileSearching.DO_NOT_SKIP_INITIAL_SELECTION);
    }

    @Override
    @NotNull
    public Function<SyntaxNode, Optional<SyntaxNode>> createFunctionToCheckSearchingCriteria(
            Direction direction, Offsets initialSelection,
            SyntaxTreeAdapter.WhileSearching whileSearching
    ) {
        return node -> {
            var parent = node.getParent();
            if (parent == null) {
                return Optional.empty();
            }
            if (parent.isCompoundExpression()) {
                var found = syntaxTree.findWithinNeighbours(node, direction, initialSelection,
                        neighbour -> {
                            if (neighbour.isOperator()) {
                                return Optional.of(neighbour);
                            }
                            return Optional.empty();
                        }, whileSearching
                );
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        };
    }
}