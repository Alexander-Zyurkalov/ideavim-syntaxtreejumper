
package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/**
 * Abstract adapter class for syntax tree operations.
 * This abstraction allows different implementations for various language parsers,
 * particularly useful for languages like C++ where the default PSI tree might be inconvenient.
 */
public abstract class SyntaxTreeAdapter {
    public static Optional<SyntaxNode> nextNeighbour(SyntaxNode node, Direction direction) {
        return switch (direction) {
            case Direction.FORWARD -> Optional.ofNullable(node.getNextSibling());
            case Direction.BACKWARD -> Optional.ofNullable(node.getPreviousSibling());
        };
    }

    public static SyntaxNode getChild(@NotNull SyntaxNode currentNode, Direction direction) {
        return switch (direction) {
            case Direction.FORWARD -> currentNode.getFirstChild();
            case Direction.BACKWARD -> currentNode.getLastChild();
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
    @Nullable
    public SyntaxNode findLastChildOfItsParent(@NotNull SyntaxNode node) {
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
    public SyntaxNode replaceWithParentIfParentEqualsTheNode(@Nullable SyntaxNode node) {
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
            SyntaxNode initElementAtLeft, SyntaxNode initElementAtRight, Direction direction) {
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
                case BACKWARD -> initElementAtLeft;
                case FORWARD -> initElementAtRight;
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
    public SyntaxNode findCurrentElement(Offsets initialOffsets, Direction direction) {
        boolean isOnlyCaretButNoSelection = initialOffsets.leftOffset() >= initialOffsets.rightOffset() - 1;

        if (isOnlyCaretButNoSelection) {
            return findNodeAt(initialOffsets.leftOffset());
        } else {
            SyntaxNode initElementAtLeft = findNodeAt(initialOffsets.leftOffset());
            SyntaxNode initElementAtRight = findNodeAt(initialOffsets.rightOffset() - 1);
            if (initElementAtLeft == null || initElementAtRight == null) {
                return null;
            }
            if (initElementAtLeft.isEquivalentTo(initElementAtRight)) {
                return initElementAtLeft;
            } else {
                return findParentElementIfInitialElementsAreAtEdgesOrChooseOne(initElementAtLeft, initElementAtRight, direction);
            }
        }
    }

    /**
     * Public method to find the current element and its siblings based on the given offsets.
     * This method can be reused by other classes like PsiElementHighlighter.
     *
     * @param initialOffsets
     * @param direction
     */
    public ElementWithSiblings findElementWithSiblings(Offsets initialOffsets, Direction direction) {
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

    public enum WhileSearching {
        SKIP_INITIAL_SELECTION, DO_NOT_SKIP_INITIAL_SELECTION
    }

    public Optional<SyntaxNode> findWithinNeighbours(
            @NotNull SyntaxNode currentNode, Direction direction, Offsets initialSelection,
            Function<SyntaxNode, Optional<SyntaxNode>> findNodeType,
            WhileSearching whileSearching) {
        Optional<SyntaxNode> found = Optional.empty();
        var next = Optional.of(currentNode);
        while (next.isPresent()) {
            currentNode = next.get();
            if ( whileSearching == WhileSearching.SKIP_INITIAL_SELECTION && !currentNode.isInRightDirection(initialSelection, direction)) {
                next = nextNeighbour(currentNode, direction);
                continue;
            }
            found = findNodeType.apply(currentNode);
            if (isNodeFound(direction, initialSelection, found)
            ) {
                return found;
            }
            if (!currentNode.getChildren().isEmpty()) {
                found = findWithinNeighbours(
                        getChild(currentNode, direction), direction, initialSelection, findNodeType, WhileSearching.DO_NOT_SKIP_INITIAL_SELECTION);
                if (isNodeFound(direction, initialSelection, found)
                ) {
                    return found;
                }
            }
            next = nextNeighbour(currentNode, direction);
        }
        return found;
    }

    private static boolean isNodeFound(Direction direction, Offsets initialSelection, Optional<SyntaxNode> found) {
        return found.isPresent() &&
                found.get().isInRightDirection(initialSelection, direction) &&
                !found.get().areBordersEqual(initialSelection);
    }

    /**
     * Finds a PARAMETER_LIST or ARGUMENT_LIST node based on the direction from the current node.
     *
     * @param currentNode                   The starting node
     * @param direction                     The direction to search
     * @param initialSelection              The initial selection
     * @param functionToCheckSearchCriteria The function to find parameter nodes based on specific criteria
     * @param whileSearching
     * @return The found parameter/argument list node or null if not found
     */
    public Optional<SyntaxNode> findNodeByDirection(
            SyntaxNode currentNode, Direction direction, Offsets initialSelection,
            @NotNull Function<SyntaxNode, Optional<SyntaxNode>> functionToCheckSearchCriteria, WhileSearching whileSearching) {
        Optional<SyntaxNode> found = findWithinNeighbours(
                currentNode, direction, initialSelection, functionToCheckSearchCriteria, whileSearching);

        while (found.isEmpty()) {
            currentNode = currentNode.getParent();
            if (currentNode == null || currentNode.isPsiFile()) {
                break;
            }
            found = findWithinNeighbours(currentNode, direction, initialSelection, functionToCheckSearchCriteria, whileSearching);
        }
        return found;
    }

}