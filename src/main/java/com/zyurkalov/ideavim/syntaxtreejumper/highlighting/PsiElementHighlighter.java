package com.zyurkalov.ideavim.syntaxtreejumper.highlighting;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.JBColor;
import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.ElementWithSiblings;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages highlighting of syntax elements with transparent background colors.
 * Highlights the current element, previous sibling, and next sibling.
 */
public class PsiElementHighlighter {

    private static final String HIGHLIGHTER_GROUP = "SyntaxTreeJumper";

    // Color definitions with transparency
    private static final Color CURRENT_ELEMENT_COLOR = new JBColor(
            new Color(0, 150, 255, 18),    // Light blue with transparency (normal theme)
            new Color(100, 180, 255, 18)   // Brighter blue with transparency (dark theme)
    );

    private static final Color PREVIOUS_SIBLING_COLOR = new JBColor(
            new Color(255, 150, 0, 18),    // Orange with transparency (normal theme)
            new Color(255, 180, 100, 18)   // Brighter orange with transparency (dark theme)
    );

    private static final Color NEXT_SIBLING_COLOR = new JBColor(
            new Color(0, 200, 100, 18),    // Green with transparency (normal theme)
            new Color(100, 220, 150, 18)   // Brighter green with transparency (dark theme)
    );

    private final MarkupModel markupModel;
    private final List<RangeHighlighter> activeHighlighters;

    public PsiElementHighlighter(@NotNull Editor editor) {
        this.markupModel = editor.getMarkupModel();
        this.activeHighlighters = new ArrayList<>();
    }

    /**
     * Highlights the current syntax element and its siblings based on the selection range.
     */
    public void highlightElementAndSiblings(@NotNull SyntaxTreeAdapter syntaxTree, int startOffset, int endOffset) {
        HighlightingConfig config = HighlightingConfig.getInstance();

        // Check if highlighting is enabled
        if (!config.isHighlightingEnabled()) {
            return;
        }

        clearHighlights();

        // Use SameLevelElementsMotionHandler to find the current element and its siblings
        Offsets offsets = new Offsets(startOffset, endOffset);
        ElementWithSiblings elementWithSiblings = syntaxTree.findElementWithSiblings(offsets, MotionDirection.BACKWARD);

        if (elementWithSiblings.currentElement() == null) {
            return;
        }

        // Highlight current element
        if (config.showCurrentElement) {
            highlightElement(elementWithSiblings.currentElement(), CURRENT_ELEMENT_COLOR, "Current Element");
        }

        // Highlight previous sibling
        if (config.showPreviousSibling && elementWithSiblings.previousSibling() != null &&
                !elementWithSiblings.previousSibling().isPsiFile()
        ) {
            highlightElement(elementWithSiblings.previousSibling(), PREVIOUS_SIBLING_COLOR, "Previous Sibling");
        }

        // Highlight next sibling
        if (config.showNextSibling && elementWithSiblings.nextSibling() != null &&
                !elementWithSiblings.nextSibling().isPsiFile()
        ) {
            highlightElement(elementWithSiblings.nextSibling(), NEXT_SIBLING_COLOR, "Next Sibling");
        }
    }

    /**
     * Clears all active highlights.
     */
    public void clearHighlights() {
        for (RangeHighlighter highlighter : activeHighlighters) {
            markupModel.removeHighlighter(highlighter);
        }
        activeHighlighters.clear();
    }

    /**
     * Highlights a single syntax element with the specified color.
     */
    private void highlightElement(@NotNull SyntaxNode element, @NotNull Color backgroundColor, @NotNull String tooltip) {
        HighlightingConfig config = HighlightingConfig.getInstance();

        TextAttributes attributes = new TextAttributes();
        attributes.setBackgroundColor(backgroundColor);

        RangeHighlighter highlighter = markupModel.addRangeHighlighter(
                element.getTextRange().getStartOffset(),
                element.getTextRange().getEndOffset(),
                HighlighterLayer.SELECTION - 1, // Layer below selection but above syntax highlighting
                attributes,
                com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
        );

        if (config.showTooltips) {
            highlighter.setErrorStripeTooltip(tooltip + ": " + element.getNodeTypeName());
        }
        highlighter.setGutterIconRenderer(null); // No gutter icons

        activeHighlighters.add(highlighter);
    }
}