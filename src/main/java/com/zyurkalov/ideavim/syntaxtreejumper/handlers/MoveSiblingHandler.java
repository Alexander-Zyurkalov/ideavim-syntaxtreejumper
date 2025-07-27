package com.zyurkalov.ideavim.syntaxtreejumper.handlers;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.util.TextRange;
import com.maddyhome.idea.vim.api.ExecutionContext;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.command.OperatorArguments;
import com.maddyhome.idea.vim.extension.ExtensionHandler;
import com.maddyhome.idea.vim.newapi.IjVimEditorKt;
import com.maddyhome.idea.vim.state.mode.Mode;
import com.maddyhome.idea.vim.state.mode.SelectionType;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapterFactory;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.SameLevelElementsMotionHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Handler that swaps the current element with its sibling in the specified direction.
 * Uses SameLevelElementsMotionHandler to find the current element and its siblings.
 */
public class MoveSiblingHandler implements ExtensionHandler {

    private final Direction direction;

    public MoveSiblingHandler(Direction direction) {
        this.direction = direction;
    }

    @Override
    public void execute(
            @NotNull VimEditor vimEditor,
            @NotNull ExecutionContext context,
            @NotNull OperatorArguments operatorArguments) {
        Editor editor = IjVimEditorKt.getIj(vimEditor);

        // Get syntax tree adapter
        SyntaxTreeAdapter syntaxTree = SyntaxTreeAdapterFactory.createAdapter(editor);
        if (syntaxTree == null) {
            return;
        }

        List<LogicalPosition> caretPositions = new ArrayList<>();
        List<Caret> carets = editor.getCaretModel().getAllCarets();
        boolean anyMotionExecuted = false;

        for (Caret caret : carets) {
            int startSelectionOffset = caret.getOffset();
            int endSelectionOffset = caret.getOffset();
            if (caret.hasSelection()) {
                startSelectionOffset = caret.getSelectionStart();
                endSelectionOffset = caret.getSelectionEnd();
            }

            Offsets currentOffsets = new Offsets(startSelectionOffset, endSelectionOffset);

            // Use SameLevelElementsMotionHandler to find the current element and siblings
            SameLevelElementsMotionHandler motionHandler = new SameLevelElementsMotionHandler(syntaxTree, direction);
            SameLevelElementsMotionHandler.ElementWithSiblings elementWithSiblings =
                    motionHandler.findElementWithSiblings(currentOffsets);

            if (elementWithSiblings.currentElement() == null) {
                caretPositions.add(caret.getLogicalPosition());
                continue;
            }

            // Get the sibling to swap with based on direction
            SyntaxTreeAdapter.SyntaxNode targetSibling = switch (direction) {
                case BACKWARD -> elementWithSiblings.previousSibling();
                case FORWARD -> elementWithSiblings.nextSibling();
            };

            if (targetSibling == null) {
                caretPositions.add(caret.getLogicalPosition());
                continue; // No sibling to swap with
            }

            // Perform the swap and update the cursor position
            Offsets newOffsets = swapElements(editor, elementWithSiblings.currentElement(), targetSibling);
            caret.setSelection(newOffsets.leftOffset(), newOffsets.rightOffset());
            caret.moveToOffset(newOffsets.leftOffset());
            anyMotionExecuted = true;

            caretPositions.add(caret.getLogicalPosition());
        }

        // Update highlighting if any motion was executed
        if (anyMotionExecuted) {
            FunctionHandler.updateHighlightingForEditor(editor);
        }

        // Scroll to the appropriate caret position
        scrollToFirstOrLast(caretPositions, editor);

        // Set visual mode
        vimEditor.setMode(new Mode.VISUAL(SelectionType.CHARACTER_WISE, new Mode.NORMAL()));
    }

    /**
     * Swaps two syntax elements in the editor by replacing their text content.
     * Returns the new offsets of the moved element, or null if swap failed.
     */
    private Offsets swapElements(@NotNull Editor editor,
                                 @NotNull SyntaxTreeAdapter.SyntaxNode originalElement,
                                 @NotNull SyntaxTreeAdapter.SyntaxNode targetElement) {
        TextRange originalElementTextRange = originalElement.getTextRange();
        TextRange targetElementTextRange = targetElement.getTextRange();

        // Get the text content of both elements
        String originalElementText = editor.getDocument().getText(originalElementTextRange);
        String targetElementText = editor.getDocument().getText(targetElementTextRange);

        // Determine which element comes first in the document
        boolean isOriginalElementFirst = originalElementTextRange.getStartOffset() < targetElementTextRange.getStartOffset();

        // Calculate the new position of the originalElement after the swap
        Offsets newOffsets;
        if (isOriginalElementFirst) {
            //OriginalElement will move to where targetElement was, accounting for the size difference
            int sizeDifference = targetElementText.length() - originalElementText.length();
            newOffsets = new Offsets(
                    targetElementTextRange.getStartOffset() + sizeDifference,
                    targetElementTextRange.getEndOffset()
            );
        } else {
            // OriginalElement will move to where the targetElement was
            newOffsets = new Offsets(targetElementTextRange.getStartOffset(), targetElementTextRange.getStartOffset() + originalElementText.length());

        }

        // Use WriteCommandAction to ensure proper undo/redo support
        WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
            if (isOriginalElementFirst) {
                // Replace the second element first (later in a document) to maintain correct offsets
                editor.getDocument().replaceString(targetElementTextRange.getStartOffset(), targetElementTextRange.getEndOffset(), originalElementText);
                // Then replace the first element
                editor.getDocument().replaceString(originalElementTextRange.getStartOffset(), originalElementTextRange.getEndOffset(), targetElementText);
            } else {
                // Replace the first element first (later in a document) to maintain correct offsets
                editor.getDocument().replaceString(originalElementTextRange.getStartOffset(), originalElementTextRange.getEndOffset(), targetElementText);
                // Then replace the second element
                editor.getDocument().replaceString(targetElementTextRange.getStartOffset(), targetElementTextRange.getEndOffset(), originalElementText);
            }
        });


        return newOffsets;
    }

    /**
     * Scrolls to the first or last caret position based on direction.
     */
    private void scrollToFirstOrLast(List<LogicalPosition> caretPositions, Editor editor) {
        if (caretPositions.isEmpty()) {
            return;
        }

        caretPositions.sort(
                Comparator.comparingInt((LogicalPosition pos) -> pos.line).
                        thenComparingInt(pos -> pos.column)
        );

        LogicalPosition targetPosition = switch (direction) {
            case FORWARD -> caretPositions.get(caretPositions.size() - 1); // Last position
            case BACKWARD -> caretPositions.get(0); // First position
        };

        editor.getScrollingModel().scrollTo(targetPosition, ScrollType.MAKE_VISIBLE);
    }
}