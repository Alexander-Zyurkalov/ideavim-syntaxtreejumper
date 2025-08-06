package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a node in the syntax tree.
 */
public interface SyntaxNode {
    /**
     * Gets the text range of this node in the document.
     */
    TextRange getTextRange();

    /**
     * Gets the text content of this node.
     */
    @NotNull String getText();

    /**
     * Gets the parent node, or null if this is the root.
     */
    @Nullable SyntaxNode getParent();

    /**
     * Gets all direct children of this node.
     */
    @NotNull List<SyntaxNode> getChildren();

    /**
     * Gets the previous sibling node, or null if this is the first child.
     */
    @Nullable SyntaxNode getPreviousSibling();

    /**
     * Gets the next sibling node, or null if this is the last child.
     */
    @Nullable SyntaxNode getNextSibling();

    /**
     * Checks if this node represents whitespace only.
     */
    boolean isWhitespace();

    /**
     * Checks if this node is equivalent to another node.
     */
    boolean isEquivalentTo(@Nullable SyntaxNode other);

    /**
     * Gets a simple name for this node type (for debugging/tooltips).
     */
    @NotNull String getNodeTypeName();

    /**
     * Gets the specific type or category name of the node.
     */
    @NotNull String getTypeName();

    // Helper methods
    default boolean isCompoundExpression() {
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


    default boolean isOperator() {
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

    default boolean isBracket() {
        return switch (getText()) {
            case "(", ")", "[", "]", "{", "}" -> true;
            default -> false;
        };
    }


    SyntaxNode getFirstChild();

    SyntaxNode getLastChild();
}
