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
public class ArgumentParameterListMotionHandler extends SyntaxTreeNodesMotionHandler {

    public ArgumentParameterListMotionHandler(SyntaxTreeAdapter syntaxTree, MotionDirection direction) {
        super(syntaxTree, direction);
    }

    @Override
    protected boolean shallGoDeeper() {
        return true;
    }

    @Override
    protected boolean doesTargetFollowRequirements(SyntaxNode startingPoint, SyntaxNode targetElement, Offsets initialOffsets) {
        return targetElement.isFunctionParameter() ||
                targetElement.isFunctionArgument() ||
                targetElement.isTypeParameter();
    }

}
