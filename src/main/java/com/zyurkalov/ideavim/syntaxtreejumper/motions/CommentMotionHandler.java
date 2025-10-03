package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;

import java.util.Optional;

public class CommentMotionHandler extends AbstractSyntaxTreeNodesMotionHandler {
    @Override
    protected boolean doesTargetFollowRequirements(SyntaxNode startingPoint, SyntaxNode targetElement, Offsets initialOffsets) {
        return targetElement.isComment(); // is it a comment?
    }

    @Override
    protected Optional<SyntaxNode> expandSelection(SyntaxNode initialElement, Offsets initialOffsets) {
        if (initialElement.isComment() && (
                initialElement.getTextRange().getStartOffset() <= initialOffsets.leftOffset() &&
                initialElement.getTextRange().getEndOffset() >= initialOffsets.rightOffset()
        )) {
            return Optional.of(initialElement);
        }
        return super.expandSelection(initialElement, initialOffsets);
    }

    @Override
    protected boolean shallGoDeeper() {
        return true;
    }

    public CommentMotionHandler(SyntaxTreeAdapter syntaxTree, MotionDirection direction) {
        super(syntaxTree, direction);
    }
}
