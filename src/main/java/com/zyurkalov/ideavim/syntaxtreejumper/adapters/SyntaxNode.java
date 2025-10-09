package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Represents a node in the syntax tree.
 */
public abstract class SyntaxNode {
    final protected PsiElement psiElement;

    public SyntaxNode(PsiElement psiElement) {
        this.psiElement = psiElement;
    }

    /**
     * Gets the text range of this node in the document.
     */
    public TextRange getTextRange() {
        return psiElement.getTextRange();
    }


    /**
     * Gets the text content of this node.
     */
    @NotNull
    public String getText() {
        return psiElement.getText();
    }

    /**
     * Gets the parent node, or null if this is the root.
     */
    @Nullable
    public abstract SyntaxNode getParent();

    /**
     * Gets all direct children of this node.
     */
    @NotNull
    public abstract List<SyntaxNode> getChildren();

    /**
     * Gets the previous sibling node, or null if this is the first child.
     */
    @Nullable
    public abstract SyntaxNode getPreviousSibling();

    /**
     * Gets the next sibling node, or null if this is the last child.
     */
    @Nullable
    public abstract SyntaxNode getNextSibling();

    /**
     * Checks if this node represents whitespace only.
     */
    public boolean isWhitespace() {
        return psiElement instanceof PsiWhiteSpace || psiElement.getText().trim().isEmpty();
    }

    /**
     * Checks if this node is equivalent to another node.
     */
    public boolean isEquivalentTo(@Nullable SyntaxNode other) {
        return false;
    }

    /**
     * Gets a simple name for this node type (for debugging/tooltips).
     */
    @NotNull
    public String getNodeTypeName() {
        return psiElement.getClass().getSimpleName();
    }

    /**
     * Gets the specific type or category name of the node.
     */
    public @NotNull String getTypeName() {
        if (psiElement.getNode() == null) {
            return "";
        }
        return psiElement.getNode().getElementType().toString();
    }

    public abstract SyntaxNode getFirstChild();

    public abstract SyntaxNode getLastChild();

    @NotNull
    public PsiElement getPsiElement() {
        return psiElement;
    }

    // Helper methods - these remain as concrete implementations
    public boolean isCompoundExpression() {
        return getTypeName().contains("BINARY") ||
                getTypeName().contains("ADDITIVE") ||
                getTypeName().contains("MULTIPLICATIVE") ||
                getTypeName().contains("RELATIONAL") ||
                getTypeName().contains("EQUALITY") ||
                getTypeName().contains("EXPRESSION") ||
                getTypeName().contains("EXPRESSION_LIST") ||
                getTypeName().contains("PARAMETER_LIST") ||
                getTypeName().contains("ARGUMENT_LIST") ||
                getTypeName().contains("ArgumentList") ||
                getTypeName().contains("PARAMETER_DECLARATION") ||
                getTypeName().contains("COMPOUND_INITIALIZER") ||
                getTypeName().contains("RECORD_HEADER") ||
                getTypeName().contains("LOGICAL");
    }

    public boolean isOperator() {
        String text = getText().trim();

        // Single character operators common across all languages
        switch (text) {
            case "+", "-", "*", "/", "%":           // Arithmetic operators
            case "=", "!", "<", ">":                // Assignment and comparison
            case "&", "|", "^", "~":                // Bitwise operators
            case "?", ":":                          // Ternary operator parts
            case ",", ";":                          // Separators/terminators
            case ".":                               // Member access
                return true;
        }

        // Multi-character operators common across languages
        switch (text) {
            case "==", "!=", "<=", ">=":            // Comparison operators
            case "&&", "||":                        // Logical operators
            case "++", "--":                        // Increment/decrement
            case "+=", "-=", "*=", "/=", "%=":      // Compound assignment
            case "&=", "|=", "^=":                  // Bitwise compound assignment
            case "<<", ">>":                        // Shift operators
            case "<<=", ">>=":                      // Shift compound assignment
            case "->":                              // Arrow operator (C++, Rust, JS)
            case "::":                              // Scope resolution (C++, Rust)
            case "=>":                              // Fat arrow (JS, Rust)
            case "**":                              // Exponentiation (Python, JS)
            case "//":                              // Integer division (Python)
            case "**=", "//=":                      // Python compound assignment
            case "===", "!==":                      // Strict equality (JS)
            case "??":                              // Nullish coalescing (JS)
            case "?.":                              // Optional chaining (JS)
            case "??=":                             // Nullish assignment (JS)
            case "...", "..=", "..":                // Range operators (Rust)
                return true;
        }

        // Check if it's a keyword operator (common in Python)
        switch (text) {
            case "and", "or", "not":                // Python logical operators
            case "is", "in":                        // Python identity/membership
            case "instanceof":                      // Java/JS instanceof
            case "typeof":                          // JS typeof
            case "as":                              // Rust/TypeScript casting
                return true;
        }

        return false;
    }

    public boolean isBracket() {
        return switch (getText()) {
            case "(", ")", "[", "]", "{", "}", ">", "<" -> true;
            default -> false;
        };
    }

    /**
     * Checks if a node represents a parameter list or argument list.
     */
    public boolean isFunctionParameter() {
        String typeName = getTypeName();
        return typeName.equals("PARAMETER") || typeName.equals("ARGUMENT");
    }

    public boolean isFunctionArgument() {
//        REFERENCE_EXPRESSION
        String typeName = getTypeName();
        boolean result = false;
        try {
            result = typeName.contains("EXPRESSION") &&
                    Objects.requireNonNull(getParent()).isExpressionList() &&
                    Objects.requireNonNull(getParent().getParent()).isMethodOrFunctionCallExpression();
        } catch (NullPointerException ignored) {
        }
        return result;
    }


    public boolean isMethodOrFunctionCallExpression() {
        String typeName = getTypeName();
        return typeName.equals("METHOD_CALL_EXPRESSION");
    }

    public boolean isExpressionList() {
        String typeName = getTypeName();
        return typeName.equals("EXPRESSION_LIST");
    }

    public boolean isTypeParameter() {
        return getTypeName().equals("TYPE_PARAMETER");
    }

    public boolean isInRightDirection(Offsets initialSelection, MotionDirection direction) {
        TextRange currentRange = getTextRange();
        if (currentRange == null) {
            return false;
        }
        return switch (direction) {
            case BACKWARD -> currentRange.getEndOffset() <= initialSelection.leftOffset() ||
                    currentRange.getStartOffset() < initialSelection.leftOffset();
            case FORWARD -> currentRange.getStartOffset() > initialSelection.leftOffset() ||
                    currentRange.getEndOffset() > initialSelection.rightOffset();
            case EXPAND -> false; //TODO: what shall I do here?
            case SHRINK -> false;
        };
    }

    public boolean isPsiFile() {
        return psiElement instanceof PsiFile;
    }

    public boolean isDeclarationStatement() {
        String typeName = getTypeName();
        return typeName.equals("DECLARATION_STATEMENT") || typeName.equals("ASSIGNMENT_EXPRESSION");
    }

    public boolean isExpressionStatement() {
        String typeName = getTypeName();
        return typeName.equals("EXPRESSION_STATEMENT");
    }

    public boolean isReturnStatement() {
        String typeName = getTypeName();
        return typeName.equals("RETURN_STATEMENT") ||
                typeName.equals("RETURN");
    }

    public boolean isLoopStatement() {
        String typeName = getTypeName();
        return typeName.equals("FOR_STATEMENT") ||
                typeName.equals("WHILE_STATEMENT") ||
                typeName.equals("DO_WHILE_STATEMENT") ||
                typeName.equals("FOREACH_STATEMENT") ||
                typeName.equals("FOR") ||
                typeName.equals("WHILE") ||
                typeName.equals("DO_WHILE") ||
                typeName.equals("FOREACH");
    }

    public boolean isConditionalStatement() {
        String typeName = getTypeName();
        return typeName.equals("IF_STATEMENT") ||
                typeName.equals("SWITCH_STATEMENT") ||
                typeName.equals("CASE_STATEMENT") ||
                typeName.equals("DEFAULT_CASE_STATEMENT") ||
                typeName.equals("SWITCH_EXPRESSION") ||
                typeName.equals("IF") ||
                typeName.equals("SWITCH") ||
                typeName.equals("CASE") ||
                typeName.equals("DEFAULT");
    }

    public boolean isLoopOrConditionalStatement() {
        return isLoopStatement() || isConditionalStatement();
    }

    public boolean isMethodDefinition() {
        String typeName = getTypeName();
        return typeName.equals("METHOD_DECLARATION") ||
                typeName.equals("METHOD") ||
                typeName.equals("METHOD_DEFINITION") ||
                typeName.equals("CONSTRUCTOR_DECLARATION") ||
                typeName.equals("DESTRUCTOR_DECLARATION") ||
                typeName.equals("CONVERSION_FUNCTION_DECLARATION") ||
                typeName.equals("CONSTRUCTOR");
    }

    public boolean isFunctionDefinition() {
        String typeName = getTypeName();
        return typeName.equals("FUNCTION_DECLARATION") ||
                typeName.equals("FUNCTION_DEFINITION") ||
                typeName.equals("TEMPLATE_FUNCTION_DECLARATION") ||
                typeName.equals("FUNCTION") ||
                typeName.equals("LAMBDA_EXPRESSION");
    }

    public boolean areBordersEqual(Offsets initialSelection) {
        TextRange currentRange = getTextRange();
        if (currentRange == null) {
            return false;
        }
        return currentRange.getStartOffset() == initialSelection.leftOffset() &&
                currentRange.getEndOffset() == initialSelection.rightOffset();
    }

    public boolean isVariable() {
        SyntaxNode parent = getParent();
        if (parent == null) {
            return false;
        }
        SyntaxNode grandParent = parent.getParent();
        if (grandParent == null) {
            return false;
        }
        return getTypeName().equals("IDENTIFIER") && (
                parent.getTypeName().equals("LOCAL_VARIABLE") ||
                        parent.getTypeName().equals("FIELD") ||
                        (parent.getTypeName().equals("REFERENCE_EXPRESSION") &&
                                parent.getTextRange().equals(getTextRange())
                        )
        );
    }

    public boolean isCodeBlock() {
        String typeName = getTypeName();
        return typeName.equals("CODE_BLOCK") ||
                typeName.equals("BLOCK_STATEMENT");
    }

    public boolean isAStatement() {
        String typeName = getTypeName();
        return typeName.contains("_STATEMENT");
    }


    public boolean isExpression() {
        String typeName = getTypeName();
        SyntaxNode parent = getParent();
        if (parent == null) {
            return false;
        }
        return typeName.endsWith("EXPRESSION") && !parent.isMethodOrFunctionCallExpression();
    }

    public boolean isEqualSymbol() {
        return getTypeName().equals("EQ");
    }

    public boolean isClassDefinition() {
        String typeName = getTypeName();
        return typeName.equals("CLASS");
    }

    public boolean isTemplate() {
        return false;
    }

    public boolean isComment() {
        return getTypeName().endsWith("_COMMENT");
    }

    public boolean isMacro() {
        return false;
    }

    public boolean isImport() {
        String typeName = getTypeName();
        return typeName.contains("IMPORT_STATEMENT");
    }
    public boolean isTypeUsage() {
        return false;
    }

}