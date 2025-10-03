package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;

/**
 * MotionHandler that finds PsiElements of type loop statements (FOR_STATEMENT, WHILE_STATEMENT, etc.)
 * and conditional statements (IF_STATEMENT, SWITCH_STATEMENT, CASE_STATEMENT, etc.)
 * in accordance to the given Direction from the caret, then places the caret
 * at the found element.
 */
public class LoopConditionalMotionHandler extends AbstractSyntaxTreeNodesMotionHandler {


    public LoopConditionalMotionHandler(SyntaxTreeAdapter syntaxTree, MotionDirection direction) {
        super(syntaxTree, direction);
    }

    @Override
    public boolean shallGoDeeper() {
        return true;
    }

    @Override
    public boolean doesTargetFollowRequirements(SyntaxNode startingPoint, SyntaxNode targetElement, Offsets initialOffsets) {
        boolean loopOrConditionalStatement = targetElement.isLoopOrConditionalStatement();
        boolean equivalent = !targetElement.isEquivalentTo(startingPoint);
        return loopOrConditionalStatement && equivalent;
    }
}