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
import com.zyurkalov.ideavim.syntaxtreejumper.motions.NumberedElementJumpHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handler for numbered jumps to PSI elements.
 * 
 * This is a simplified version that requires separate handlers for each number.
 * In practice, you would register separate handlers for 0-9, like:
 * Alt-j-0, Alt-j-1, Alt-j-2, etc.
 * 
 * For this example, we'll assume a number is passed in during construction.
 */
public class NumberedJumpFunctionHandler implements ExtensionHandler {
    
    private final int targetNumber;
    
    public NumberedJumpFunctionHandler(int targetNumber) {
        this.targetNumber = targetNumber;
    }
    
    public NumberedJumpFunctionHandler() {
        this(1); // Default to first element
    }
    
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
        
        executeNumberedJump(editor, vimEditor, psiFile, targetNumber);
    }
    /**
     * Executes the numbered jump with the specified target number
     */
    private void executeNumberedJump(Editor editor, VimEditor vimEditor, PsiFile psiFile, int targetNumber) {
        NumberedElementJumpHandler handler = new NumberedElementJumpHandler(psiFile, targetNumber);
        
        List<LogicalPosition> caretPositions = new ArrayList<>();
        List<Caret> carets = editor.getCaretModel().getAllCarets();
        
        for (Caret caret : carets) {
            int startSelectionOffset = caret.getOffset();
            int endSelectionOffset = caret.getOffset();
            
            if (caret.hasSelection()) {
                startSelectionOffset = caret.getSelectionStart();
                endSelectionOffset = caret.getSelectionEnd();
            }
            
            Offsets initialOffsets = new Offsets(startSelectionOffset, endSelectionOffset);
            Optional<Offsets> result = handler.findNext(initialOffsets);
            
            result.ifPresent(offsets -> {
                caret.setSelection(offsets.leftOffset(), offsets.rightOffset());
                caret.moveToOffset(offsets.leftOffset());
            });
            
            caretPositions.add(caret.getLogicalPosition());
        }
        
        // Scroll to make the target visible
        if (!caretPositions.isEmpty()) {
            // For numbered jumps, always scroll to the first caret position
            LogicalPosition targetPosition = caretPositions.get(0);
            editor.getScrollingModel().scrollTo(targetPosition, ScrollType.MAKE_VISIBLE);
        }
        
        // Set visual mode to show the selection
        vimEditor.setMode(new Mode.VISUAL(SelectionType.CHARACTER_WISE, new Mode.NORMAL()));
    }
    
    // Factory methods for different numbers
    public static NumberedJumpFunctionHandler createForNumber(int number) {
        return new NumberedJumpFunctionHandler(number);
    }
}