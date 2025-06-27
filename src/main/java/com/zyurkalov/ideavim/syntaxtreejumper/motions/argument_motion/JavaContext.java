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
            // Check for method calls
            if (current instanceof PsiMethodCallExpression methodCall) {
                PsiExpressionList argList = methodCall.getArgumentList();
                return new ArgumentContext(argList, ArgumentContextType.METHOD_CALL);
            }

            // Check for constructor calls
            if (current instanceof PsiNewExpression newExpression) {
                PsiExpressionList argList = newExpression.getArgumentList();
                if (argList != null) {
                    return new ArgumentContext(argList,ArgumentContextType.CONSTRUCTOR_CALL);
                }
            }

            // Check for method declarations
            if (current instanceof PsiMethod method) {
                PsiParameterList paramList = method.getParameterList();
                return new ArgumentContext(paramList,ArgumentContextType.METHOD_DECLARATION);
            }

            // Check for lambda expressions
            if (current instanceof PsiLambdaExpression lambda) {
                PsiParameterList paramList = lambda.getParameterList();
                return new ArgumentContext(paramList,ArgumentContextType.LAMBDA_PARAMETERS);
            }

            // Check if we're inside an argument list
            PsiElement parent = current.getParent();
            if (parent instanceof PsiExpressionList || parent instanceof PsiParameterList) {
                PsiElement grandParent = parent.getParent();
                if (grandParent instanceof PsiMethodCallExpression) {
                    return new ArgumentContext((PsiExpressionList) parent,ArgumentContextType.METHOD_CALL);
                } else if (grandParent instanceof PsiNewExpression) {
                    return new ArgumentContext((PsiExpressionList) parent,ArgumentContextType.CONSTRUCTOR_CALL);
                } else if (grandParent instanceof PsiMethod) {
                    return new ArgumentContext((PsiParameterList) parent,ArgumentContextType.METHOD_DECLARATION);
                } else if (grandParent instanceof PsiLambdaExpression) {
                    return new ArgumentContext((PsiParameterList) parent,ArgumentContextType.LAMBDA_PARAMETERS);
                }
            }

            current = current.getParent();
        }

        return null;
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