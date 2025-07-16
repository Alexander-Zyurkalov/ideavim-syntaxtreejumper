package com.zyurkalov.ideavim.syntaxtreejumper.motions.argument_motion;

import com.intellij.psi.PsiElement;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import org.jetbrains.annotations.Nullable;

public class RustContext extends LanguageContext {

    public RustContext(Direction direction) {
        super(direction);
    }

    /**
     * Finds the argument context (function call, method call, macro call, etc.) that contains the cursor
     */
    @Override
    public @Nullable ArgumentContext findArgumentContext(PsiElement element) {
        PsiElement current = element;

        while (current != null) {
            RustArgumentContext context = createArgumentContext(current);
            if (context != null) {
                return context;
            }

            // Check if we're inside an argument list
            PsiElement parent = current.getParent();
            if (parent != null && isRustArgumentList(parent)) {
                PsiElement grandParent = parent.getParent();
                if (grandParent != null && isRustCallExpression(grandParent)) {
                    return new RustArgumentContext(parent);
                }
            }

            current = current.getParent();
        }

        return null;
    }

    private RustArgumentContext createArgumentContext(PsiElement element) {
        if (element == null) {
            return null;
        }

        String elementType = element.getNode().getElementType().toString();
        
        // Handle different types of Rust expressions that can have arguments
        switch (elementType) {
            case "CALL_EXPR":
                // Function calls: foo(a, b, c)
                return findArgumentListInChildren(element);
                
            case "METHOD_CALL_EXPR":
                // Method calls: obj.method(a, b, c)
                return findArgumentListInChildren(element);
                
            case "MACRO_CALL":
                // Macro calls: println!(a, b, c)
                return findArgumentListInChildren(element);
                
            case "STRUCT_LITERAL":
                // Struct literals: Foo { a: 1, b: 2 }
                return findStructFieldsInChildren(element);
                
            case "TUPLE_EXPR":
                // Tuple expressions: (a, b, c)
                return new RustArgumentContext(element);
                
            case "FN":
                // Function definitions: fn foo(a: i32, b: String)
                return findParameterListInChildren(element);
                
            case "IMPL_METHOD":
                // Method definitions in impl blocks
                return findParameterListInChildren(element);
                
            case "CLOSURE_EXPR":
                // Closures: |a, b| a + b
                return findClosureParametersInChildren(element);
                
            default:
                return null;
        }
    }

    private RustArgumentContext findArgumentListInChildren(PsiElement element) {
        for (PsiElement child : element.getChildren()) {
            if (isRustArgumentList(child)) {
                return new RustArgumentContext(child);
            }
        }
        return null;
    }

    private RustArgumentContext findParameterListInChildren(PsiElement element) {
        for (PsiElement child : element.getChildren()) {
            if (isRustParameterList(child)) {
                return new RustArgumentContext(child);
            }
        }
        return null;
    }

    private RustArgumentContext findStructFieldsInChildren(PsiElement element) {
        for (PsiElement child : element.getChildren()) {
            if (isRustStructLiteralBody(child)) {
                return new RustArgumentContext(child);
            }
        }
        return null;
    }

    private RustArgumentContext findClosureParametersInChildren(PsiElement element) {
        for (PsiElement child : element.getChildren()) {
            if (isRustClosureParameterList(child)) {
                return new RustArgumentContext(child);
            }
        }
        return null;
    }

    private boolean isRustArgumentList(PsiElement element) {
        if (element == null) return false;
        String elementType = element.getNode().getElementType().toString();
        return "VALUE_ARGUMENT_LIST".equals(elementType) || 
               "PAREN_EXPR".equals(elementType);
    }

    private boolean isRustParameterList(PsiElement element) {
        if (element == null) return false;
        String elementType = element.getNode().getElementType().toString();
        return "VALUE_PARAMETER_LIST".equals(elementType) ||
               "SELF_PARAMETER".equals(elementType);
    }

    private boolean isRustStructLiteralBody(PsiElement element) {
        if (element == null) return false;
        String elementType = element.getNode().getElementType().toString();
        return "STRUCT_LITERAL_BODY".equals(elementType);
    }

    private boolean isRustClosureParameterList(PsiElement element) {
        if (element == null) return false;
        String elementType = element.getNode().getElementType().toString();
        return "LAMBDA_EXPR".equals(elementType) ||
               "VALUE_PARAMETER_LIST".equals(elementType);
    }

    private boolean isRustCallExpression(PsiElement element) {
        if (element == null) return false;
        String elementType = element.getNode().getElementType().toString();
        return "CALL_EXPR".equals(elementType) || 
               "METHOD_CALL_EXPR".equals(elementType) ||
               "MACRO_CALL".equals(elementType) ||
               "STRUCT_LITERAL".equals(elementType) ||
               "TUPLE_EXPR".equals(elementType) ||
               "FN".equals(elementType) ||
               "IMPL_METHOD".equals(elementType) ||
               "CLOSURE_EXPR".equals(elementType);
    }
}