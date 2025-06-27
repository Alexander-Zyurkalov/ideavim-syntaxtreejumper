package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import org.jetbrains.annotations.Nullable;

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
    private final JavaContext context;

    public ArgumentMotionHandler(PsiFile psiFile, Direction direction) {
        this.psiFile = psiFile;
        this.direction = direction;
        this.context = new JavaContext(direction);
    }

    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {

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

    /**
     * Finds the argument context (method call, constructor, etc.) that contains the cursor
     */
    private @Nullable ArgumentContext findArgumentContext(PsiElement element) {

        return context.findArgumentContext(element);
    }

    /**
     * Finds the index of the current argument based on cursor position
     */
    private int findCurrentArgumentIndex(List<PsiElement> arguments, Offsets initialOffsets) {
        return context.findCurrentArgumentIndex(arguments, initialOffsets);
    }

    /**
     * Finds the closest argument when cursor is not currently in an argument
     */
    private Optional<Offsets> findClosestArgument(List<PsiElement> arguments, Offsets initialOffsets) {

        return context.findClosestArgument(arguments, initialOffsets);
    }

}