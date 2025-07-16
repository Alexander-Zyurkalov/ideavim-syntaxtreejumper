package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.argument_motion.ArgumentContext;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.argument_motion.LanguageContext;

import java.util.List;
import java.util.Optional;

/**
 * Handles navigation between function arguments.
 * Supports jumping to next/previous arguments in method calls, constructor calls,
 * method parameters, and lambda parameters.
 */
public class ArgumentMotionHandler extends MotionHandler {

    private final PsiFile psiFile;
    private final Direction direction;
    private final LanguageContext context;


    public ArgumentMotionHandler(PsiFile psiFile, Direction direction,
                                 LanguageContext context) {
        this.psiFile = psiFile;
        this.direction = direction;
        this.context = context;
    }

    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {
        if (this.context == null) {
            return Optional.of(initialOffsets);
        }

        PsiElement leftElement = psiFile.findElementAt(initialOffsets.leftOffset());
        PsiElement rightElement = psiFile.findElementAt(initialOffsets.rightOffset() - 1);

        PsiElement elementAtCursor = findSmallestCommonParent(leftElement, rightElement, initialOffsets);
        if (elementAtCursor == null) {
            return Optional.of(initialOffsets);
        }

        // Find the closest argument list container
        ArgumentContext context = this.context.findArgumentContext(elementAtCursor);
        if (context == null) {
            return Optional.of(initialOffsets);
        }

        // Get all arguments in the context
        List<PsiElement> arguments = context.getArguments();
        if (arguments.isEmpty()) {
            return Optional.of(initialOffsets);
        }

        // Find current argument index
        int currentIndex = this.context.findCurrentArgumentIndex(arguments, initialOffsets);
        if (currentIndex == -1) {
            // If not currently in an argument, find the closest one
            return this.context.findClosestArgument(arguments, initialOffsets);
        }

        // Navigate to next/previous argument
        int targetIndex = switch (direction) {
            case FORWARD -> (currentIndex + 1) % arguments.size();
            case BACKWARD -> (currentIndex - 1 + arguments.size()) % arguments.size();
        };

        // Don't move if we're already at the boundary and would wrap around
        if ((direction == Direction.FORWARD && currentIndex == arguments.size() - 1) ||
                (direction == Direction.BACKWARD && currentIndex == 0)) {
            return Optional.of(initialOffsets);
        }

        PsiElement targetArgument = arguments.get(targetIndex);
        TextRange range = targetArgument.getTextRange();
        return Optional.of(new Offsets(range.getStartOffset(), range.getEndOffset()));
    }


}