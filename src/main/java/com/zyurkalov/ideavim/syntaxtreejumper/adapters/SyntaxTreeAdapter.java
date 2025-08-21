
package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract adapter class for syntax tree operations.
 * This abstraction allows different implementations for various language parsers,
 * particularly useful for languages like C++ where the default PSI tree might be inconvenient.
 */
public abstract class SyntaxTreeAdapter {

    public static SyntaxNode getChild(@NotNull SyntaxNode currentNode, MotionDirection direction) {
        return switch (direction) {
            case FORWARD, EXPAND -> currentNode.getFirstChild();
            case BACKWARD, SHRINK -> currentNode.getLastChild();
        };
    }

    @Nullable
    public abstract PsiFile getPsiFile();

    /**
     * Finds the syntax node at the specified offset in the file.
     *
     * @param offset The character offset in the file
     * @return The node at the offset, or null if not found
     */
    @Nullable
    public abstract SyntaxNode findNodeAt(int offset);

    /**
     * Finds the smallest common parent of two nodes.
     *
     * @param node1 First node
     * @param node2 Second node
     * @return The common parent, or null if no common parent exists
     */
    @Nullable
    public abstract SyntaxNode findCommonParent(@NotNull SyntaxNode node1, @NotNull SyntaxNode node2);

    /**
     * Gets the total length of the document/file.
     */
    public abstract int getDocumentLength();

    /**
     * Finds the previous non-whitespace sibling of the given node.
     */
    @Nullable
    public SyntaxNode findPreviousNonWhitespaceSibling(@NotNull SyntaxNode node) {
        SyntaxNode sibling = node.getPreviousSibling();
        while (sibling != null && isASymbolToSkip(sibling)) {
            sibling = sibling.getPreviousSibling();
        }
        return sibling;
    }

    public static boolean isASymbolToSkip(SyntaxNode sibling) {
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
    public SyntaxNode findNextNonWhitespaceSibling(SyntaxNode node) {
        if (node == null) {
            return null;
        }
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
    public SyntaxNode findFirstChildOfItsParent(SyntaxNode node) {
        if (node == null) {
            return null;
        }
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
    public SyntaxNode findLastChildOfItsParent(SyntaxNode node) {
        if (node == null) {
            return null;
        }
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
    @NotNull
    public SyntaxNode replaceWithParentIfParentEqualsTheNode(@NotNull SyntaxNode node) {
        SyntaxNode parent = node.getParent();
        while (parent != null && parent.getText().equals(node.getText())) {
            node = parent;
            parent = node.getParent();
        }
        return node;
    }
    @Nullable
    public SyntaxNode findParentThatIsNotEqualToTheNode(@Nullable SyntaxNode node) {
        if (node == null || node.isPsiFile()) {
            return null;
        }
        SyntaxNode parent = node.getParent();
        while (parent != null && parent.getText().equals(node.getText())) {
            node = parent;
            parent = node.getParent();
        }
        return parent;
    }

    /**
     * Finds the smallest common parent that fully encompasses the current selection
     */
    @Nullable
    public SyntaxNode findSmallestCommonParent(SyntaxNode leftElement, SyntaxNode rightElement, Offsets selection) {
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

    /**
     * Finds the largest parent node that is still completely within the selection boundaries.
     *
     * @param node1     First node
     * @param node2     Second node
     * @param selection The current selection boundaries
     * @return The largest parent node within the selection, or null if no such node exists
     */
    @Nullable
    public SyntaxNode findLargestParentWithinSelection(@NotNull SyntaxNode node1, @NotNull SyntaxNode node2, Offsets selection) {
        SyntaxNode commonParent = findCommonParent(node1, node2);
        if (commonParent == null) {
            return null;
        }

        SyntaxNode currentNode = commonParent;
        SyntaxNode result = null;

        while (currentNode != null) {
            TextRange range = currentNode.getTextRange();
            if (range.getStartOffset() >= selection.leftOffset() &&
                    range.getEndOffset() <= selection.rightOffset()) {
                result = currentNode;
                currentNode = currentNode.getParent();
            } else {
                break;
            }
        }

        return result;
    }

    @NotNull
    public SyntaxNode findParentElementIfInitialElementsAreAtEdgesOrChooseOne(
            SyntaxNode initElementAtLeft, SyntaxNode initElementAtRight, MotionDirection direction) {
        SyntaxNode initialElement = initElementAtLeft;
        SyntaxNode commonParent = findCommonParent(initElementAtLeft, initElementAtRight);
        if (commonParent == null) {
            return initialElement;
        }
        boolean areOurElementsAtTheEdges =
                commonParent.getTextRange().getStartOffset() == initElementAtLeft.getTextRange().getStartOffset() &&
                        commonParent.getTextRange().getEndOffset() == initElementAtRight.getTextRange().getEndOffset();
        if (areOurElementsAtTheEdges) {
            initialElement = commonParent;
        } else {
            initialElement = switch (direction) {
                case BACKWARD, SHRINK -> initElementAtLeft;
                case FORWARD -> initElementAtRight;
                case EXPAND -> initElementAtLeft; //TODO what shall we do here?
            };
        }
        return initialElement;
    }

    /**
     * Finds the current syntax node based on the given offsets.
     * Extracted from the original findNext method for reuse.
     *
     * @param initialOffsets
     * @param direction
     */
    @Nullable
    public SyntaxNode findCurrentElement(Offsets initialOffsets, MotionDirection direction) {
        boolean isOnlyCaretButNoSelection = initialOffsets.leftOffset() >= initialOffsets.rightOffset() - 1;
        if (isOnlyCaretButNoSelection) {
            SyntaxNode nodeAt = findNodeAt(initialOffsets.leftOffset());
            if (nodeAt == null) {
                return null;
            }
            return replaceWithParentIfParentEqualsTheNode(nodeAt);
        } else {
            SyntaxNode initElementAtLeft = findNodeAt(initialOffsets.leftOffset());
            SyntaxNode initElementAtRight = findNodeAt(initialOffsets.rightOffset() - 1);
            if (initElementAtLeft == null || initElementAtRight == null) {
                return null;
            }
            SyntaxNode target = null;
            if (initElementAtLeft.isEquivalentTo(initElementAtRight)) {
                target = initElementAtLeft;
            } else {
                target =  findParentElementIfInitialElementsAreAtEdgesOrChooseOne(initElementAtLeft, initElementAtRight, direction);
            }
            return replaceWithParentIfParentEqualsTheNode(target);
        }
    }

    /**
     * Public method to find the current element and its siblings based on the given offsets.
     * This method can be reused by other classes like PsiElementHighlighter.
     *
     * @param initialOffsets
     * @param direction
     */
    public ElementWithSiblings findElementWithSiblings(Offsets initialOffsets, MotionDirection direction) {
        SyntaxNode currentElement = findCurrentElement(initialOffsets, direction);
        if (currentElement == null || currentElement.isPsiFile()) {
            return new ElementWithSiblings(null, null, null);
        }

        currentElement = replaceWithParentIfParentEqualsTheNode(currentElement);
        if (currentElement == null) {
            return new ElementWithSiblings(null, null, null);
        }

        SyntaxNode previousSibling = findPreviousNonWhitespaceSibling(currentElement);
        SyntaxNode nextSibling = findNextNonWhitespaceSibling(currentElement);

        return new ElementWithSiblings(currentElement, previousSibling, nextSibling);
    }


}