package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
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
public class ArgumentParameterListMotionHandler extends AbstractFindNodeMotionHandler {

    public ArgumentParameterListMotionHandler(SyntaxTreeAdapter syntaxTree, MotionDirection direction) {
        super(syntaxTree, direction, WhileSearching.DO_NOT_SKIP_INITIAL_SELECTION);
    }
    @Override
    @NotNull
    public Function<SyntaxNode, Optional<SyntaxNode>> createFunctionToCheckSearchingCriteria(MotionDirection direction, Offsets initialSelection, WhileSearching whileSearching) {
        return node -> {
            if (node.isFunctionParameter() || node.isFunctionArgument() || node.isTypeParameter()) {
                return Optional.of(node);
            }
            return Optional.empty();
        };
    }

}
