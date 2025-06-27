package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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

    public ArgumentMotionHandler(PsiFile psiFile, Direction direction) {
        this.psiFile = psiFile;
        this.direction = direction;
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
        ArgumentContext context = findArgumentContext(elementAtCursor);
        if (context == null) {
            return Optional.of(initialOffsets);
        }

        // Get all arguments in the context
        List<PsiElement> arguments = context.getArguments();
        if (arguments.isEmpty()) {
            return Optional.of(initialOffsets);
        }

        // Find current argument index
        int currentIndex = findCurrentArgumentIndex(arguments, initialOffsets);
        if (currentIndex == -1) {
            // If not currently in an argument, find the closest one
            return findClosestArgument(arguments, initialOffsets);
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
                    return new ArgumentContext(argList, ArgumentContextType.CONSTRUCTOR_CALL);
                }
            }

            // Check for method declarations
            if (current instanceof PsiMethod method) {
                PsiParameterList paramList = method.getParameterList();
                return new ArgumentContext(paramList, ArgumentContextType.METHOD_DECLARATION);
            }

            // Check for lambda expressions
            if (current instanceof PsiLambdaExpression lambda) {
                PsiParameterList paramList = lambda.getParameterList();
                return new ArgumentContext(paramList, ArgumentContextType.LAMBDA_PARAMETERS);
            }

            // Check if we're inside an argument list
            PsiElement parent = current.getParent();
            if (parent instanceof PsiExpressionList || parent instanceof PsiParameterList) {
                PsiElement grandParent = parent.getParent();
                if (grandParent instanceof PsiMethodCallExpression) {
                    return new ArgumentContext((PsiExpressionList) parent, ArgumentContextType.METHOD_CALL);
                } else if (grandParent instanceof PsiNewExpression) {
                    return new ArgumentContext((PsiExpressionList) parent, ArgumentContextType.CONSTRUCTOR_CALL);
                } else if (grandParent instanceof PsiMethod) {
                    return new ArgumentContext((PsiParameterList) parent, ArgumentContextType.METHOD_DECLARATION);
                } else if (grandParent instanceof PsiLambdaExpression) {
                    return new ArgumentContext((PsiParameterList) parent, ArgumentContextType.LAMBDA_PARAMETERS);
                }
            }

            current = current.getParent();
        }

        return null;
    }

    /**
     * Finds the index of the current argument based on cursor position
     */
    private int findCurrentArgumentIndex(List<PsiElement> arguments, Offsets initialOffsets) {
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
    private Optional<Offsets> findClosestArgument(List<PsiElement> arguments, Offsets initialOffsets) {
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

    /**
     * Context information about an argument list
     */
    private static class ArgumentContext {
        private final PsiElement argumentList;
        private final ArgumentContextType type;

        public ArgumentContext(PsiElement argumentList, ArgumentContextType type) {
            this.argumentList = argumentList;
            this.type = type;
        }

        public List<PsiElement> getArguments() {
            List<PsiElement> arguments = new ArrayList<>();

            if (argumentList instanceof PsiExpressionList expressionList) {
                // Method call or constructor arguments
                for (PsiExpression expr : expressionList.getExpressions()) {
                    arguments.add(expr);
                }
            } else if (argumentList instanceof PsiParameterList parameterList) {
                // Method declaration or lambda parameters
                for (PsiParameter param : parameterList.getParameters()) {
                    arguments.add(param);
                }
            }

            return arguments;
        }

        public ArgumentContextType getType() {
            return type;
        }
    }

    /**
     * Types of argument contexts
     */
    private enum ArgumentContextType {
        METHOD_CALL,
        CONSTRUCTOR_CALL,
        METHOD_DECLARATION,
        LAMBDA_PARAMETERS
    }
}