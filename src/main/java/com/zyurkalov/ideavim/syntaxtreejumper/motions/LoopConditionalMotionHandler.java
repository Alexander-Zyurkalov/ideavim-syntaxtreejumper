package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

/**
 * MotionHandler that finds PsiElements of type loop statements (FOR_STATEMENT, WHILE_STATEMENT, etc.)
 * and conditional statements (IF_STATEMENT, SWITCH_STATEMENT, CASE_STATEMENT, etc.)
 * in accordance to the given Direction from the caret, then places the caret
 * at the found element.
 */
public class LoopConditionalMotionHandler extends AbstractFindNodeMotionHandler {

    public LoopConditionalMotionHandler(SyntaxTreeAdapter syntaxTree, Direction direction) {
        super(syntaxTree, direction);
    }

    @Override
    @NotNull
    public Function<SyntaxNode, Optional<SyntaxNode>> createFunctionToFindNode(Direction direction, Offsets initialSelection) {
        return node -> {
            if (node.isLoopOrConditionalStatement()) {
                return Optional.of(node);
            }
            return Optional.empty();
        };
    }
}