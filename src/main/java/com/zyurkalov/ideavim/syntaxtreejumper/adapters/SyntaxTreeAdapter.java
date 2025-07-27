package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Adapter interface for syntax tree operations.
 * This abstraction allows different implementations for various language parsers,
 * particularly useful for languages like C++ where the default PSI tree might be inconvenient.
 */
public interface SyntaxTreeAdapter {

    /**
     * Represents a node in the syntax tree.
     */
    interface SyntaxNode {
        /**
         * Gets the text range of this node in the document.
         */
        @NotNull TextRange getTextRange();

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
    }

    /**
     * Finds the syntax node at the specified offset in the file.
     *
     * @param offset The character offset in the file
     * @return The node at the offset, or null if not found
     */
    @Nullable SyntaxNode findNodeAt(int offset);

    /**
     * Finds the smallest common parent of two nodes.
     *
     * @param node1 First node
     * @param node2 Second node
     * @return The common parent, or null if no common parent exists
     */
    @Nullable SyntaxNode findCommonParent(@NotNull SyntaxNode node1, @NotNull SyntaxNode node2);

    /**
     * Gets the total length of the document/file.
     */
    int getDocumentLength();

    /**
     * Finds the previous non-whitespace sibling of the given node.
     */
    @Nullable
    default SyntaxNode findPreviousNonWhitespaceSibling(@NotNull SyntaxNode node) {
        SyntaxNode sibling = node.getPreviousSibling();
        while (sibling != null && isASymbolToSkip(sibling)) {
            sibling = sibling.getPreviousSibling();
        }
        return sibling;
    }

    private static boolean isASymbolToSkip(SyntaxTreeAdapter.SyntaxNode sibling) {
        return sibling.isWhitespace() ||
                sibling.getText().trim().isEmpty() ||
                sibling.getText().equals("=") ||
                sibling.getText().equals(",") ||
                sibling.getText().equals("+") ||
                sibling.getText().equals("-") ||
                sibling.getText().equals("/") ||
                sibling.getText().equals("*") ||
                sibling.getText().equals("(") ||
                sibling.getText().equals(")") ||
                sibling.getText().equals("[") ||
                sibling.getText().equals("]") ||
                sibling.getText().equals("{") ||
                sibling.getText().equals("}") ||
                sibling.getText().equals("'") ||
                sibling.getText().equals("\"") ||
                sibling.getText().equals(";");
    }


    /**
     * Finds the next non-whitespace sibling of the given node.
     */
    @Nullable
    default SyntaxNode findNextNonWhitespaceSibling(@NotNull SyntaxNode node) {
        SyntaxNode sibling = node.getNextSibling();
        while (sibling != null && isASymbolToSkip(sibling)) {
            sibling = sibling.getNextSibling();
        }
        return sibling;
    }


    /**
     * Helper method to replace a node with its parent if they have the same text content.
     * This is useful for handling cases where leaf nodes and their parents represent the same construct.
     */
    @Nullable
    default SyntaxNode replaceWithParentIfParentEqualsTheNode(@Nullable SyntaxNode node) {
        if (node == null) {
            return null;
        }
        SyntaxNode parent = node.getParent();
        while (parent != null && parent.getText().equals(node.getText())) {
            node = parent;
            parent = node.getParent();
        }
        return node;
    }
}