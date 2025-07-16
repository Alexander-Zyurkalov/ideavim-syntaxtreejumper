package com.zyurkalov.ideavim.syntaxtreejumper.motions.argument_motion;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;

import java.util.ArrayList;
import java.util.List;

/**
 * Rust-specific context information about an argument list
 */
public class RustArgumentContext implements ArgumentContext {
    private final PsiElement argumentList;

    public RustArgumentContext(PsiElement argumentList) {
        this.argumentList = argumentList;
    }

    @Override
    public List<PsiElement> getArguments() {
        List<PsiElement> arguments = new ArrayList<>();

        if (argumentList == null) {
            return arguments;
        }

        String elementType = argumentList.getNode().getElementType().toString();

        switch (elementType) {
            case "VALUE_ARGUMENT_LIST":
                // Function/method call arguments: foo(a, b, c)
                extractValueArguments(arguments);
                break;
                
            case "VALUE_PARAMETER_LIST":
                // Function parameter lists: fn foo(a: i32, b: String)
                extractParameters(arguments);
                break;
                
            case "STRUCT_LITERAL_BODY":
                // Struct literal fields: Foo { a: 1, b: 2 }
                extractStructFields(arguments);
                break;
                
            case "TUPLE_EXPR":
                // Tuple expressions: (a, b, c)
                extractTupleElements(arguments);
                break;
                
            case "PAREN_EXPR":
                // Parenthesized expressions that might contain arguments
                extractParenthesizedArguments(arguments);
                break;
                
            case "LAMBDA_EXPR":
                // Closure parameters: |a, b| a + b
                extractClosureParameters(arguments);
                break;
                
            default:
                // Generic extraction for unknown types
                extractGenericArguments(arguments);
                break;
        }

        return arguments;
    }

    private void extractValueArguments(List<PsiElement> arguments) {
        for (PsiElement child : argumentList.getChildren()) {
            String childType = child.getNode().getElementType().toString();
            
            // Skip commas, parentheses, and whitespace
            if (!"COMMA".equals(childType) && 
                !"LPAREN".equals(childType) && 
                !"RPAREN".equals(childType) &&
                !(child instanceof PsiWhiteSpace)) {
                arguments.add(child);
            }
        }
    }

    private void extractParameters(List<PsiElement> arguments) {
        for (PsiElement child : argumentList.getChildren()) {
            String childType = child.getNode().getElementType().toString();
            
            if ("VALUE_PARAMETER".equals(childType) || 
                "SELF_PARAMETER".equals(childType)) {
                arguments.add(child);
            }
        }
    }

    private void extractStructFields(List<PsiElement> arguments) {
        for (PsiElement child : argumentList.getChildren()) {
            String childType = child.getNode().getElementType().toString();
            
            if ("STRUCT_LITERAL_FIELD".equals(childType)) {
                arguments.add(child);
            }
        }
    }

    private void extractTupleElements(List<PsiElement> arguments) {
        for (PsiElement child : argumentList.getChildren()) {
            String childType = child.getNode().getElementType().toString();
            
            // Skip commas, parentheses, and whitespace
            if (!"COMMA".equals(childType) && 
                !"LPAREN".equals(childType) && 
                !"RPAREN".equals(childType) &&
                !(child instanceof PsiWhiteSpace)) {
                arguments.add(child);
            }
        }
    }

    private void extractParenthesizedArguments(List<PsiElement> arguments) {
        for (PsiElement child : argumentList.getChildren()) {
            // For parenthesized expressions, we typically want the inner expression
            if (!(child instanceof PsiWhiteSpace)) {
                String childType = child.getNode().getElementType().toString();
                if (!"LPAREN".equals(childType) && !"RPAREN".equals(childType)) {
                    arguments.add(child);
                }
            }
        }
    }

    private void extractClosureParameters(List<PsiElement> arguments) {
        // Look for the parameter list within the closure
        for (PsiElement child : argumentList.getChildren()) {
            if ("VALUE_PARAMETER_LIST".equals(child.getNode().getElementType().toString())) {
                extractParameters(arguments);
                return;
            }
        }
        
        // If no explicit parameter list, check for implicit parameters
        for (PsiElement child : argumentList.getChildren()) {
            String childType = child.getNode().getElementType().toString();
            if ("IDENTIFIER".equals(childType)) {
                arguments.add(child);
            }
        }
    }

    private void extractGenericArguments(List<PsiElement> arguments) {
        // Generic fallback: collect all non-whitespace, non-punctuation children
        for (PsiElement child : argumentList.getChildren()) {
            if (!(child instanceof PsiWhiteSpace)) {
                String childType = child.getNode().getElementType().toString();
                
                // Skip common punctuation tokens
                if (!"COMMA".equals(childType) && 
                    !"LPAREN".equals(childType) && 
                    !"RPAREN".equals(childType) &&
                    !"LBRACE".equals(childType) &&
                    !"RBRACE".equals(childType) &&
                    !"SEMICOLON".equals(childType)) {
                    
                    arguments.add(child);
                }
            }
        }
    }
}