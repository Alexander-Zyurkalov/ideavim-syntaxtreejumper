package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;

import java.util.Optional;

/**
 * MotionHandler that finds PsiElements of type DECLARATION_STATEMENT, EXPRESSION_STATEMENT, or RETURN_STATEMENT
 * in accordance to the given Direction from the caret, then places the caret
 * at the statement element.
 */
public class StatementMotionHandler extends AbstractSyntaxTreeNodesMotionHandler {

    public StatementMotionHandler(SyntaxTreeAdapter syntaxTree, MotionDirection direction) {
        super(syntaxTree, direction);
    }

    @Override
    protected Optional<SyntaxNode> expandSelection(SyntaxNode initialElement, Offsets initialOffsets) {
        return super.expandSelection(initialElement, initialOffsets);
    }

    @Override
    protected boolean shallGoDeeper() {
        return true;
    }

    @Override
    protected boolean doesTargetFollowRequirements(SyntaxNode startingPoint, SyntaxNode targetElement, Offsets initialOffsets) {
        return targetElement.isDeclarationStatement() || targetElement.isExpressionStatement() ||
                targetElement.isReturnStatement() || targetElement.isAStatement();
    }

}
