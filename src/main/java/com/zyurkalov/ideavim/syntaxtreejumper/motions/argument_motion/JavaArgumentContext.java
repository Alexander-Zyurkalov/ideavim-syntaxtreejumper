package com.zyurkalov.ideavim.syntaxtreejumper.motions.argument_motion;

import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Context information about an argument list
 */
public class JavaArgumentContext implements ArgumentContext {
    private final PsiElement argumentList;

    public JavaArgumentContext(PsiElement argumentList) {
        this.argumentList = argumentList;
    }

    public List<PsiElement> getArguments() {
        List<PsiElement> arguments = new ArrayList<PsiElement>();

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


}
