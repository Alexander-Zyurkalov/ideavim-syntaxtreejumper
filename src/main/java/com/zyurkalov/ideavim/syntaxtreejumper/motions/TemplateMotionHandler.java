package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;

public class TemplateMotionHandler extends AbstractSyntaxTreeNodesMotionHandler {
    @Override
    protected boolean doesTargetFollowRequirements(SyntaxNode startingPoint, SyntaxNode targetElement, Offsets initialOffsets) {
        return targetElement.isTemplate();
    }

    @Override
    protected boolean shallGoDeeper() {
        return true;
    }

    public TemplateMotionHandler(SyntaxTreeAdapter syntaxTree, MotionDirection direction) {
        super(syntaxTree, direction);
    }
}
