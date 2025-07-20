// FILE: NumberedJumpOverlayManager.java
package com.zyurkalov.ideavim.syntaxtreejumper;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.PsiFile;
import com.intellij.ui.JBColor;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.NumberedElementJumpHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Manages visual overlays showing numbered labels for jump targets
 */
public class NumberedJumpOverlayManager {

    private final Editor editor;
    private final PsiFile psiFile;
    private final List<JLabel> overlayLabels = new ArrayList<>();
    private final List<RangeHighlighter> highlighters = new ArrayList<>();
    private final Map<Integer, Offsets> numberToOffsets = new HashMap<>();
    private KeyAdapter keyListener;
    private boolean isActive = false;
    private Consumer<Offsets> jumpCallback;

    public NumberedJumpOverlayManager(Editor editor, PsiFile psiFile) {
        this.editor = editor;
        this.psiFile = psiFile;
    }

    /**
     * Shows numbered overlays for all possible jump targets and waits for user input
     */
    public void showOverlaysAndWaitForInput(Offsets currentOffsets, Consumer<Offsets> onJumpSelected) {
        if (isActive) {
            hideOverlays();
            return;
        }

        this.jumpCallback = onJumpSelected;
        showOverlaysForPosition(currentOffsets);
    }

    /**
     * Shows overlays for a specific position (used both initially and for chaining)
     */
    private void showOverlaysForPosition(Offsets currentOffsets) {
        // Clear any existing overlays first
        clearOverlaysOnly();

        isActive = true;

        // Calculate all possible jump targets
        Map<Integer, Offsets> jumpTargets = calculateJumpTargets(currentOffsets);
        if (jumpTargets.isEmpty()) {
            isActive = false;
            return;
        }

        // Store the mapping for later use
        numberToOffsets.clear();
        numberToOffsets.putAll(jumpTargets);

        // Create visual overlays
        createOverlays(jumpTargets);

        // Set up a key listener (only once)
        if (keyListener == null) {
            setupKeyListener();

            // Add key listener to editor
            JComponent editorComponent = editor.getContentComponent();
            editorComponent.addKeyListener(keyListener);
            editorComponent.requestFocus();
        }
    }

    /**
     * Calculates all possible jump targets (siblings + parent)
     */
    private Map<Integer, Offsets> calculateJumpTargets(Offsets currentOffsets) {
        Map<Integer, Offsets> targets = new HashMap<>();

        // Get targets for numbers 0-9
        for (int i = 0; i <= 9; i++) {
            NumberedElementJumpHandler handler = new NumberedElementJumpHandler(psiFile, i);
            int finalI = i;
            handler.findNext(currentOffsets).ifPresent(offsets -> {
                // Only add if it's different from the current position
                if (!offsets.equals(currentOffsets)) {
                    targets.put(finalI, offsets);
                }
            });
        }

        return targets;
    }

    /**
     * Creates visual overlay labels for each jump target
     */
    private void createOverlays(Map<Integer, Offsets> jumpTargets) {
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();

        for (Map.Entry<Integer, Offsets> entry : jumpTargets.entrySet()) {
            int number = entry.getKey();
            Offsets offsets = entry.getValue();
            if (number == 0 || offsets.rightOffset() - offsets.leftOffset() <= 1) {
                continue;
            }

            // Create highlighting for the target range
            createHighlighter(offsets);

            // Create an overlay label
            createOverlayLabel(number, offsets);
        }
    }

    /**
     * Creates a range highlighter to highlight the target element
     */
    private void createHighlighter(Offsets offsets) {
        TextAttributes attributes = new TextAttributes();
        attributes.setBackgroundColor(JBColor.YELLOW.darker());
        attributes.setForegroundColor(JBColor.BLACK);

        RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(
                offsets.leftOffset(),
                offsets.rightOffset(),
                HighlighterLayer.SELECTION + 1,
                attributes,
                HighlighterTargetArea.EXACT_RANGE
        );

        highlighters.add(highlighter);
    }

    /**
     * Creates an overlay label showing the number
     */
    private void createOverlayLabel(int number, Offsets offsets) {
        // Convert text offset to screen coordinates
        LogicalPosition logicalPos = editor.offsetToLogicalPosition(offsets.leftOffset());
        Point screenPos = editor.logicalPositionToXY(logicalPos);

        // Create label
        JLabel label = getJLabel(number, screenPos);

        // Add to an editor component
        JComponent editorComponent = editor.getContentComponent();
        editorComponent.add(label);
        editorComponent.setComponentZOrder(label, 0); // Bring to the front

        overlayLabels.add(label);
    }

    private static @NotNull JLabel getJLabel(int number, Point screenPos) {
        JLabel label = new JLabel(String.valueOf(number));
        label.setOpaque(true);
        label.setBackground(JBColor.RED);
        label.setForeground(JBColor.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
        label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        label.setHorizontalAlignment(SwingConstants.CENTER);

        // Position the label
        Dimension labelSize = label.getPreferredSize();
        label.setBounds(
                screenPos.x - labelSize.width / 2,
                screenPos.y - labelSize.height - 2,
                labelSize.width,
                labelSize.height
        );
        return label;
    }

    /**
     * Sets up a key listener to handle number input and escape
     */
    private void setupKeyListener() {
        keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isActive) return;

                // Handle escape - cancel operation completely
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hideOverlays();
                    e.consume();
                    return;
                }

                char keyChar = e.getKeyChar();

                // Handle number keys
                if (Character.isDigit(keyChar)) {
                    int number = Character.getNumericValue(keyChar);

                    Offsets targetOffsets = numberToOffsets.get(number);
                    if (targetOffsets != null) {
                        // Show new overlays for the selected position
                        showOverlaysForPosition(targetOffsets);

                        // Also notify the callback that we jumped to this position
                        if (jumpCallback != null) {
                            jumpCallback.accept(targetOffsets);
                        }
                    } else {
                        // Invalid number - just hide overlays
                        hideOverlays();
                    }
                    e.consume();
                } else {
                    // Any other key - hide overlays
                    hideOverlays();
                    e.consume();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (!isActive) return;
                // Also handle escape in keyTyped to ensure it's caught
                if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
                    hideOverlays();
                    e.consume();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (!isActive) return;

                // Handle escape in keyReleased as well for maximum compatibility
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hideOverlays();
                    e.consume();
                }
            }
        };
    }

    /**
     * Clears only the visual overlays without fully deactivating
     */
    private void clearOverlaysOnly() {
        // Remove overlay labels
        JComponent editorComponent = editor.getContentComponent();
        for (JLabel label : overlayLabels) {
            editorComponent.remove(label);
        }
        overlayLabels.clear();

        // Remove highlighters
        for (RangeHighlighter highlighter : highlighters) {
            editor.getMarkupModel().removeHighlighter(highlighter);
        }
        highlighters.clear();

        // Clear mappings
        numberToOffsets.clear();

        // Repaint editor
        editorComponent.repaint();
    }

    /**
     * Hides all overlays and cleans up completely
     */
    public void hideOverlays() {
        if (!isActive) return;

        System.out.println("hideOverlays() called"); // Debug output

        isActive = false;

        // Clear visual overlays
        clearOverlaysOnly();

        // Remove key listener
        if (keyListener != null) {
            JComponent editorComponent = editor.getContentComponent();
            editorComponent.removeKeyListener(keyListener);
            keyListener = null;
        }

        // Clear callback
        jumpCallback = null;

        System.out.println("Overlays hidden and cleaned up"); // Debug output
    }

    /**
     * Checks if overlays are currently active
     */
    public boolean isActive() {
        return isActive;
    }
}