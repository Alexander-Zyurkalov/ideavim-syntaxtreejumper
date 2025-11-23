package com.zyurkalov.ideavim.syntaxtreejumper.handlers;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiManager;
import com.maddyhome.idea.vim.api.ExecutionContext;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.command.OperatorArguments;
import com.maddyhome.idea.vim.extension.ExtensionHandler;
import com.maddyhome.idea.vim.newapi.IjVimEditorKt;
import com.maddyhome.idea.vim.state.mode.Mode;
import com.maddyhome.idea.vim.state.mode.SelectionType;
import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.ElementWithSiblings;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxNode;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapterFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Handler that swaps the current element with its sibling in the specified direction.
 * Uses SameLevelElementsMotionHandler to find the current element and its siblings.
 */
public class MoveSiblingHandler implements ExtensionHandler {

    private final MotionDirection direction;

    public MoveSiblingHandler(MotionDirection direction) {
        this.direction = direction;
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
        PsiFile editorPsiFile = PsiManager.getInstance(editor.getProject()).findFile(file);
        if (editorPsiFile == null) return;

        List<LogicalPosition> caretPositions = new ArrayList<>();
        List<Caret> carets = editor.getCaretModel().getAllCarets();

        for (Caret caret : carets) {

            // Check for injected language at the caret position
            int offset = caret.getOffset();
            InjectedLanguageManager injectedManager = InjectedLanguageManager.getInstance(editorPsiFile.getProject());
            var psiFile = editorPsiFile;
            PsiElement injectedElement = injectedManager.findInjectedElementAt(psiFile, offset);

            int injectionOffset = 0;
            if (injectedElement != null) {
                PsiLanguageInjectionHost injectionHost = injectedManager.getInjectionHost(injectedElement);
                if (injectionHost != null) {
                    injectionOffset = injectionHost.getTextOffset() + 1;
                    psiFile = injectedElement.getContainingFile();
                }
            }
            var syntaxTree = SyntaxTreeAdapterFactory.createAdapter(psiFile);

            int startSelectionOffset = caret.getOffset();
            int endSelectionOffset = caret.getOffset();
            if (caret.hasSelection()) {
                startSelectionOffset = caret.getSelectionStart();
                endSelectionOffset = caret.getSelectionEnd();
            }

            var currentOffsets = new Offsets(
                    startSelectionOffset - injectionOffset,
                    endSelectionOffset - injectionOffset);

            // Use SameLevelElementsMotionHandler to find the current element and siblings
            ElementWithSiblings elementWithSiblings =
                    syntaxTree.findElementWithSiblings(currentOffsets, direction);

            if (elementWithSiblings.currentElement() == null) {
                caretPositions.add(caret.getLogicalPosition());
                continue;
            }

            // Get the sibling to swap with based on a direction
            SyntaxNode targetSibling = switch (direction) {
                case BACKWARD -> elementWithSiblings.previousSibling();
                case FORWARD -> elementWithSiblings.nextSibling();
                case EXPAND, SHRINK -> null; // TODO: what shall I do here?
            };

            if (targetSibling == null) {
                caretPositions.add(caret.getLogicalPosition());
                continue; // No sibling to swap with
            }

            // Perform the swap and update the cursor position
            TextRange originalElementTextRange = elementWithSiblings.currentElement().getTextRange();
            originalElementTextRange = new TextRange(originalElementTextRange.getStartOffset() + injectionOffset,
                    originalElementTextRange.getEndOffset() + injectionOffset);
            TextRange targetElementTextRange = targetSibling.getTextRange();
            targetElementTextRange = new TextRange(targetElementTextRange.getStartOffset() + injectionOffset,
                    targetElementTextRange.getEndOffset() + injectionOffset);
            Offsets newOffsets = swapElements(editor, originalElementTextRange, targetElementTextRange);

            startSelectionOffset = newOffsets.leftOffset();
            endSelectionOffset = newOffsets.rightOffset();

            caret.setSelection(startSelectionOffset, endSelectionOffset);
            caret.moveToOffset(startSelectionOffset);

            caretPositions.add(caret.getLogicalPosition());
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
                                 @NotNull TextRange originalElementTextRange,
                                 @NotNull TextRange targetElementTextRange) {

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
            //TODO: come up with better options for shirking and expanding
            case FORWARD, EXPAND -> caretPositions.getLast(); // Last position
            case BACKWARD, SHRINK -> caretPositions.getFirst(); // First position
        };

        editor.getScrollingModel().scrollTo(targetPosition, ScrollType.MAKE_VISIBLE);
    }
}