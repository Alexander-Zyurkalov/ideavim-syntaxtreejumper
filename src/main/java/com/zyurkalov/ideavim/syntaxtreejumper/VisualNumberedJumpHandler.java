// FILE: VisualNumberedJumpHandler.java
package com.zyurkalov.ideavim.syntaxtreejumper;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.maddyhome.idea.vim.api.ExecutionContext;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.command.OperatorArguments;
import com.maddyhome.idea.vim.extension.ExtensionHandler;
import com.maddyhome.idea.vim.newapi.IjVimEditorKt;
import com.maddyhome.idea.vim.state.mode.Mode;
import com.maddyhome.idea.vim.state.mode.SelectionType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler that shows visual overlays for numbered jumps instead of immediately jumping.
 * This replaces the direct numbered jump handlers and provides visual feedback.
 */
public class VisualNumberedJumpHandler implements ExtensionHandler {

    // Static map to keep track of overlay managers per editor
    private static final Map<Editor, NumberedJumpOverlayManager> overlayManagers = new HashMap<>();

    @Override
    public void execute(
            @NotNull VimEditor vimEditor,
            @NotNull ExecutionContext context,
            @NotNull OperatorArguments operatorArguments) {

        Editor editor = IjVimEditorKt.getIj(vimEditor);
        if (editor.getProject() == null) return;

        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (file == null) return;

        PsiFile psiFile = PsiManager.getInstance(editor.getProject()).findFile(file);
        if (psiFile == null) return;

        // Get or create an overlay manager for this editor
        NumberedJumpOverlayManager overlayManager = overlayManagers.computeIfAbsent(
                editor,
                e -> new NumberedJumpOverlayManager(e, psiFile)
        );

        // Get the current caret position
        Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        int startOffset = primaryCaret.getOffset();
        int endOffset = primaryCaret.getOffset();

        if (primaryCaret.hasSelection()) {
            startOffset = primaryCaret.getSelectionStart();
            endOffset = primaryCaret.getSelectionEnd();
        }

        Offsets currentOffsets = new Offsets(startOffset, endOffset);

        // Show overlays and wait for user input
        overlayManager.showOverlaysAndWaitForInput(currentOffsets, (targetOffsets) -> {
            // This callback is called when the user selects a number
            performJump(vimEditor, editor, targetOffsets);
        });
    }

    /**
     * Performs the actual jump to the selected target
     */
    private void performJump(VimEditor vimEditor, Editor editor, Offsets targetOffsets) {
        // Handle multiple carets if needed
        Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();

        // Set selection and move caret
        primaryCaret.setSelection(targetOffsets.leftOffset(), targetOffsets.rightOffset());
        primaryCaret.moveToOffset(targetOffsets.leftOffset());

        // Scroll to make the target visible
        LogicalPosition targetPosition = editor.offsetToLogicalPosition(targetOffsets.leftOffset());
        editor.getScrollingModel().scrollTo(targetPosition, ScrollType.MAKE_VISIBLE);

        // Set visual mode to show the selection
        vimEditor.setMode(new Mode.VISUAL(SelectionType.CHARACTER_WISE, new Mode.NORMAL()));
    }

    /**
     * Cleanup method to remove overlay managers for closed editors
     */
    public static void cleanupForEditor(Editor editor) {
        NumberedJumpOverlayManager manager = overlayManagers.remove(editor);
        if (manager != null && manager.isActive()) {
            manager.hideOverlays();
        }
    }

    /**
     * Hide overlays for all editors (useful for plugin cleanup)
     */
    public static void hideAllOverlays() {
        for (NumberedJumpOverlayManager manager : overlayManagers.values()) {
            if (manager.isActive()) {
                manager.hideOverlays();
            }
        }
        overlayManagers.clear();
    }
}