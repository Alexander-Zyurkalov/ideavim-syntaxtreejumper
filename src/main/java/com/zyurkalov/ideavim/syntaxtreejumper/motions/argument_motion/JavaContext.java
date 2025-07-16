package com.zyurkalov.ideavim.syntaxtreejumper.motions.argument_motion;

import com.intellij.psi.*;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import org.jetbrains.annotations.Nullable;

public class JavaContext extends LanguageContext {

    public JavaContext(Direction direction) {
        super(direction);
    }

    /**
     * Finds the argument context (method call, constructor, etc.) that contains the cursor
     */
    @Override
    public @Nullable ArgumentContext findArgumentContext(PsiElement element) {
        PsiElement current = element;

        while (current != null) {
            JavaArgumentContext context = createArgumentContext(current);
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
                    return new JavaArgumentContext(parent);
                }
            }

            current = current.getParent();
        }

        return null;
    }

    private JavaArgumentContext createArgumentContext(PsiElement element) {
        return switch (element) {
            case PsiMethodCallExpression methodCall -> new JavaArgumentContext(methodCall.getArgumentList());
            case PsiNewExpression newExpression when newExpression.getArgumentList() != null ->
                    new JavaArgumentContext(newExpression.getArgumentList());
            case PsiMethod method -> new JavaArgumentContext(method.getParameterList());
            case PsiLambdaExpression lambda -> new JavaArgumentContext(lambda.getParameterList());
            default -> null;
        };
    }

}