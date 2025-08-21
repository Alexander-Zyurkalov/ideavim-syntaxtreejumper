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
public class LoopConditionalMotionHandler extends SyntaxTreeNodesMotionHandler {

    private final AbstractFindNodeMotionHandler.WhileSearching whileSearching;

    public LoopConditionalMotionHandler(SyntaxTreeAdapter syntaxTree, MotionDirection direction, AbstractFindNodeMotionHandler.WhileSearching whileSearching) {
        super(syntaxTree, direction);
        this.whileSearching = whileSearching;
    }

    @Override
    public boolean shallGoDeeper() {
        return true;
    }

    @Override
    public boolean doesTargetFollowRequirements(SyntaxNode initialElement, SyntaxNode targetElement, Offsets initialOffsets) {
    boolean loopOrConditionalStatement = targetElement.isLoopOrConditionalStatement();
    boolean equivalent = !targetElement.isEquivalentTo(initialElement);
    return loopOrConditionalStatement && equivalent;
    }
}