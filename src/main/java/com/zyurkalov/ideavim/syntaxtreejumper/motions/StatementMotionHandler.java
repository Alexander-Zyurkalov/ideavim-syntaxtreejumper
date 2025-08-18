package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

/**
 * MotionHandler that finds PsiElements of type DECLARATION_STATEMENT, EXPRESSION_STATEMENT, or RETURN_STATEMENT
 * in accordance to the given Direction from the caret, then places the caret
 * at the statement element.
 */
public class StatementMotionHandler extends AbstractFindNodeMotionHandler {

    public StatementMotionHandler(SyntaxTreeAdapter syntaxTree, Direction direction) {
        super(syntaxTree, direction, SyntaxTreeAdapter.WhileSearching.DO_NOT_SKIP_INITIAL_SELECTION);
    }

    @Override
    @NotNull
    public Function<SyntaxNode, Optional<SyntaxNode>> createFunctionToCheckSearchingCriteria(Direction direction, Offsets initialSelection, SyntaxTreeAdapter.WhileSearching whileSearching) {
        return node -> {
            if (node.isDeclarationStatement() || node.isExpressionStatement() || node.isReturnStatement()) {
                return Optional.of(node);
            }
            return Optional.empty();
        };
    }
}
