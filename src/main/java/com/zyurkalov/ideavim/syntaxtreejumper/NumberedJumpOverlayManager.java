package com.zyurkalov.ideavim.syntaxtreejumper;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Manages visual overlays showing numbered labels for jump targets.
 * Uses AceJump-style global key interception for reliable input handling.
 * Extended to support continuous jumping until explicitly dismissed.
 */
public class NumberedJumpOverlayManager implements TypedActionHandler {

    private final Editor editor;
    private final PsiFile psiFile;
    private final List<JLabel> overlayLabels = new ArrayList<>();
    private final List<RangeHighlighter> highlighters = new ArrayList<>();
    private final Map<Character, Offsets> keyToOffsets = new HashMap<>();
    private boolean isActive = false;
    private Consumer<Offsets> jumpCallback;

    // QWERTY keyboard order for keys (digits first, then letters in QWERTY order)
    private static final char[] QWERTY_KEYS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p',
            'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l',
            'z', 'x', 'c', 'v', 'b', 'n', 'm'
    };

    public NumberedJumpOverlayManager(Editor editor, PsiFile psiFile) {
        this.editor = editor;
        this.psiFile = psiFile;
    }

    /**
     * TypedActionHandler implementation - handles all typed characters when active
     */
    @Override
    public void execute(@NotNull Editor editor, char charTyped, @NotNull DataContext dataContext) {
        if (!isActive || !this.editor.equals(editor)) {
            return; // Should not happen, but safety check
        }

        char keyChar = Character.toLowerCase(charTyped);

        // Handle escape character - hide overlays completely
        if (charTyped == '\u001B') { // ESC character
            hideOverlays();
            return;
        }

        // Handle valid jump keys
        if (isValidJumpKey(keyChar)) {
            Offsets targetOffsets = keyToOffsets.get(keyChar);
            if (targetOffsets != null) {
                // Valid selection - perform jump
                if (jumpCallback != null) {
                    jumpCallback.accept(targetOffsets);
                }
                // Instead of hiding overlays, refresh them from the new position
                refreshOverlaysFromCurrentPosition();
            } else {
                // Invalid key for current targets - hide overlays
                hideOverlays();
            }
        } else {
            // Any other key - hide overlays
            hideOverlays();
        }

        // Don't call any other handlers - we consume all input when active
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
     * Refreshes overlays from the current editor position
     */
    private void refreshOverlaysFromCurrentPosition() {
        if (!isActive) {
            return;
        }

        // Get current caret position
        int startOffset = editor.getCaretModel().getOffset();
        int endOffset = startOffset;

        if (editor.getCaretModel().getPrimaryCaret().hasSelection()) {
            startOffset = editor.getCaretModel().getPrimaryCaret().getSelectionStart();
            endOffset = editor.getCaretModel().getPrimaryCaret().getSelectionEnd();
        }

        Offsets currentOffsets = new Offsets(startOffset, endOffset);

        // Calculate new jump targets
        Map<Character, Offsets> jumpTargets = calculateJumpTargets(currentOffsets);

        // If no targets available, hide overlays
        if (jumpTargets.isEmpty()) {
            hideOverlays();
            return;
        }

        // Clear current overlays and show new ones
        clearOverlaysOnly();
        keyToOffsets.clear();
        keyToOffsets.putAll(jumpTargets);
        createOverlays(jumpTargets);

        // Request focus to ensure we receive key events
        editor.getContentComponent().requestFocus();
    }

    /**
     * Shows overlays for a specific position
     */
    private void showOverlaysForPosition(Offsets currentOffsets) {
        // Clear any existing overlays first
        clearOverlaysOnly();

        // Calculate all possible jump targets
        Map<Character, Offsets> jumpTargets = calculateJumpTargets(currentOffsets);
        if (jumpTargets.isEmpty()) {
            return;
        }

        // Store the mapping for later use
        keyToOffsets.clear();
        keyToOffsets.putAll(jumpTargets);

        // Create visual overlays
        createOverlays(jumpTargets);

        // Attach global key interceptor
        GlobalKeyInterceptor.getInstance().attach(editor, this);
        isActive = true;

        // Request focus to ensure we receive key events
        editor.getContentComponent().requestFocus();
    }

    /**
     * Calculates all possible jump targets (siblings + parent)
     */
    private Map<Character, Offsets> calculateJumpTargets(Offsets currentOffsets) {
        NumberedElementJumpHandler handler = new NumberedElementJumpHandler(psiFile, 0);
        List<Offsets> targets = handler.findAllTargets(currentOffsets);
        Map<Character, Offsets> result = new HashMap<>();

        for (int i = 0; i < targets.size() && i < QWERTY_KEYS.length; i++) {
            result.put(QWERTY_KEYS[i], targets.get(i));
        }

        return result;
    }

    /**
     * Creates visual overlay labels for each jump target
     */
    private void createOverlays(Map<Character, Offsets> jumpTargets) {
        for (Map.Entry<Character, Offsets> entry : jumpTargets.entrySet()) {
            char key = entry.getKey();
            Offsets offsets = entry.getValue();

            // Skip very small targets (single character selections)
            if (key == '0' || offsets.rightOffset() - offsets.leftOffset() < 1) {
                continue;
            }

            // Create highlighting for the target range
            createHighlighter(offsets);

            // Create an overlay label
            createOverlayLabel(key, offsets);
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
     * Creates an overlay label showing the key
     */
    private void createOverlayLabel(char key, Offsets offsets) {
        // Convert text offset to screen coordinates
        LogicalPosition logicalPos = editor.offsetToLogicalPosition(offsets.leftOffset());
        Point screenPos = editor.logicalPositionToXY(logicalPos);

        // Create label
        JLabel label = createLabel(key, screenPos);

        // Add to an editor component
        JComponent editorComponent = editor.getContentComponent();
        editorComponent.add(label);
        editorComponent.setComponentZOrder(label, 0); // Bring to the front

        overlayLabels.add(label);
    }

    private JLabel createLabel(char key, Point screenPos) {
        JLabel label = new JLabel(String.valueOf(key));
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

        // Repaint editor
        editorComponent.repaint();
    }

    /**
     * Hides all overlays and cleans up completely
     */
    public void hideOverlays() {
        if (!isActive) return;

        // Detach from the global key interceptor first
        GlobalKeyInterceptor.getInstance().detach(editor);
        isActive = false;

        // Clear visual overlays
        clearOverlaysOnly();

        // Clear mappings
        keyToOffsets.clear();

        // Clear callback
        jumpCallback = null;
    }

    /**
     * Checks if the given character is a valid jump key
     */
    private boolean isValidJumpKey(char c) {
        for (char validKey : QWERTY_KEYS) {
            if (validKey == c) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if overlays are currently active
     */
    public boolean isActive() {
        return isActive;
    }
}