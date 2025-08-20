package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
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

    public OperatorMotionHandler(SyntaxTreeAdapter syntaxTree, MotionDirection direction) {
        super(syntaxTree, direction, WhileSearching.DO_NOT_SKIP_INITIAL_SELECTION);
    }

    @Override
    @NotNull
    public Function<SyntaxNode, Optional<SyntaxNode>> createFunctionToCheckSearchingCriteria(
            MotionDirection direction, Offsets initialSelection,
            WhileSearching whileSearching
    ) {
        return node -> {
            var parent = node.getParent();
            if (parent == null) {
                return Optional.empty();
            }
            if (parent.isCompoundExpression()) {
                var found = findWithinNeighbours(node, direction, initialSelection,
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