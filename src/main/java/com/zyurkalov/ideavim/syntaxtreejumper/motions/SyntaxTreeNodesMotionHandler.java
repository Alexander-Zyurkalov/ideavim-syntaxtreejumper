package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;

import java.util.Optional;

public class SyntaxTreeNodesMotionHandler extends AbstractSyntaxTreeNodesMotionHandler {
    @Override
    protected Optional<SyntaxNode> expandSelection(SyntaxNode initialElement, Offsets initialOffsets) {
        if (initialOffsets.rightOffset() == initialOffsets.leftOffset()) {
            return Optional.of(initialElement);
        }
        return super.expandSelection(initialElement, initialOffsets);
    }

    public SyntaxTreeNodesMotionHandler(SyntaxTreeAdapter syntaxTree, MotionDirection direction) {
        super(syntaxTree, direction);
    }
}
