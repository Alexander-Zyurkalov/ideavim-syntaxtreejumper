package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adapter interface for syntax tree operations.
 * This abstraction allows different implementations for various language parsers,
 * particularly useful for languages like C++ where the default PSI tree might be inconvenient.
 */
public interface SyntaxTreeAdapter {
    @Nullable PsiFile getPsiFile();

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

    private static boolean isASymbolToSkip(SyntaxNode sibling) {
        if (sibling.isOperator()) {
            return true;
        }
        return sibling.isWhitespace() ||
                sibling.getText().trim().isEmpty() ||
                sibling.getText().equals("=") ||
                sibling.getText().equals("||") ||
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
                sibling.getText().equals(";") ||
                sibling.getTypeName().equals("OPERATION_SIGN") ||
                sibling.getTypeName().equals("COMMA") ||
                sibling.getTypeName().equals("OROR") ||
                sibling.getTypeName().equals("SEMICOLON") ||
                sibling.getTypeName().equals("RPAR") ||
                sibling.getTypeName().equals("LPAR") ||
                sibling.getTypeName().equals("RBRACE") ||
                sibling.getTypeName().equals("LBRACE") ||
                sibling.getTypeName().equals("RBRACK") ||
                sibling.getTypeName().equals("LBRACK");
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
     * Finds the first non-whitespace child of the parent node.
     *
     * @param node The node whose parent's first child to find
     * @return The first non-whitespace child, or null if not found
     */
    @Nullable
    default SyntaxNode findFirstChildOfItsParent(@NotNull SyntaxNode node) {
        SyntaxNode parent = node.getParent();
        if (parent == null) return null;
        SyntaxNode firstChild = parent.getFirstChild();
        while (firstChild != null && isASymbolToSkip(firstChild)) {
            firstChild = firstChild.getNextSibling();
        }
        return firstChild;
    }

    /**
     * Finds the last non-whitespace child of the parent node.
     *
     * @param node The node whose parent's last child to find
     * @return The last non-whitespace child, or null if not found
     */
    @Nullable
    default SyntaxNode findLastChildOfItsParent(@NotNull SyntaxNode node) {
        SyntaxNode parent = node.getParent();
        if (parent == null) return null;
        SyntaxNode lastChild = parent.getLastChild();
        while (lastChild != null && isASymbolToSkip(lastChild)) {
            lastChild = lastChild.getPreviousSibling();
        }
        return lastChild;
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

    /**
     * Finds the smallest common parent that fully encompasses the current selection
     */
    @Nullable
    default SyntaxNode findSmallestCommonParent(SyntaxNode leftElement, SyntaxNode rightElement, Offsets selection) {
        if (rightElement == null) {
            rightElement = leftElement;
        }

        SyntaxNode commonParent = findCommonParent(leftElement, rightElement);

        // Walk up the tree until we find an element that fully encompasses our selection
        while (commonParent != null) {
            TextRange range = commonParent.getTextRange();
            if (range.getStartOffset() <= selection.leftOffset() &&
                    range.getEndOffset() >= selection.rightOffset()) {

                // Check if this parent is actually larger than our current selection
                if (range.getStartOffset() < selection.leftOffset() ||
                        range.getEndOffset() > selection.rightOffset()) {
                    return commonParent;
                }
            }
            commonParent = commonParent.getParent();
        }

        return null;
    }
}