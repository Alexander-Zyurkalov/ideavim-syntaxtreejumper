package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

/**
 * Data structure to hold an element and its siblings for reuse across classes.
 */
public record ElementWithSiblings(SyntaxNode currentElement, SyntaxNode previousSibling, SyntaxNode nextSibling) {
}
