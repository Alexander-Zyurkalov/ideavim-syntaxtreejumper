package com.zyurkalov.ideavim.syntaxtreejumper.motions.argument_motion;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class JavaContext {
    private final Direction direction;

    public JavaContext(Direction direction) {
        this.direction = direction;
    }

    /**
     * Finds the argument context (method call, constructor, etc.) that contains the cursor
     */
    public @Nullable ArgumentContext findArgumentContext(PsiElement element) {
        PsiElement current = element;

        while (current != null) {
            ArgumentContext context = createArgumentContext(current);
            if (context != null) {
                return context;
            }

            // Check if we're inside an argument list
            PsiElement parent = current.getParent();
            if (parent instanceof PsiExpressionList || parent instanceof PsiParameterList) {
                PsiElement grandParent = parent.getParent();
                if (grandParent instanceof PsiMethodCallExpression ||
                        grandParent instanceof PsiNewExpression ||
                        grandParent instanceof PsiMethod ||
                        grandParent instanceof PsiLambdaExpression) {
                    return new ArgumentContext(parent);
                }
            }

            current = current.getParent();
        }

        return null;
    }

    private ArgumentContext createArgumentContext(PsiElement element) {
        return switch (element) {
            case PsiMethodCallExpression methodCall -> new ArgumentContext(methodCall.getArgumentList());
            case PsiNewExpression newExpression when newExpression.getArgumentList() != null ->
                    new ArgumentContext(newExpression.getArgumentList());
            case PsiMethod method -> new ArgumentContext(method.getParameterList());
            case PsiLambdaExpression lambda -> new ArgumentContext(lambda.getParameterList());
            default -> null;
        };
    }

    /**
     * Finds the index of the current argument based on cursor position
     */
    public int findCurrentArgumentIndex(List<PsiElement> arguments, Offsets initialOffsets) {
        for (int i = 0; i < arguments.size(); i++) {
            PsiElement arg = arguments.get(i);
            TextRange range = arg.getTextRange();

            // Check if cursor is within this argument
            if (initialOffsets.leftOffset() >= range.getStartOffset() &&
                    initialOffsets.rightOffset() <= range.getEndOffset()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the closest argument when cursor is not currently in an argument
     */
    public Optional<Offsets> findClosestArgument(List<PsiElement> arguments, Offsets initialOffsets) {
        if (arguments.isEmpty()) {
            return Optional.of(initialOffsets);
        }

        PsiElement targetArgument = switch (direction) {
            case FORWARD -> {
                // Find first argument after cursor
                for (PsiElement arg : arguments) {
                    if (arg.getTextRange().getStartOffset() > initialOffsets.leftOffset()) {
                        yield arg;
                    }
                }
                yield arguments.get(0); // Wrap to first if none found
            }
            case BACKWARD -> {
                // Find last argument before cursor
                for (int i = arguments.size() - 1; i >= 0; i--) {
                    PsiElement arg = arguments.get(i);
                    if (arg.getTextRange().getEndOffset() <= initialOffsets.leftOffset()) {
                        yield arg;
                    }
                }
                yield arguments.get(arguments.size() - 1); // Wrap to last if none found
            }
        };

        TextRange range = targetArgument.getTextRange();
        return Optional.of(new Offsets(range.getStartOffset(), range.getEndOffset()));
    }

}