package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;

/**
 * MotionHandler that finds operator nodes within compound expressions
 * in accordance to the given Direction from the caret.
 */
public class OperatorMotionHandler extends AbstractSyntaxTreeNodesMotionHandler {

    public OperatorMotionHandler(SyntaxTreeAdapter syntaxTree, MotionDirection direction) {
        super(syntaxTree, direction);
    }

    @Override
    protected boolean doesTargetFollowRequirements(SyntaxNode startingPoint, SyntaxNode targetElement, Offsets initialOffsets) {
        var parent = targetElement.getParent();
        if (parent == null || !parent.isCompoundExpression()) {
            return false;
        }
        return targetElement.isOperator();
    }

    @Override
    protected boolean shallGoDeeper() {
        return true;
    }
}