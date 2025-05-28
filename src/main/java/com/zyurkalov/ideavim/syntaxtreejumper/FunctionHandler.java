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
import com.zyurkalov.ideavim.syntaxtreejumper.motions.MotionHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FunctionHandler implements ExtensionHandler {

    private final Direction direction;
    private final BiFunction<PsiFile, Direction, MotionHandler> navigatorFactory;

    public FunctionHandler(Direction direction,  BiFunction<PsiFile, Direction, MotionHandler> navigatorFactory) {
        this.direction = direction;
        this.navigatorFactory = navigatorFactory;
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

        var navigator = navigatorFactory.apply(psiFile, direction);
        List<LogicalPosition> caretPositions = new ArrayList<>(); // to scroll to the first or the last position
        List<Caret> carets = editor.getCaretModel().getAllCarets();
        for (Caret caret : carets) {

            int startSelectionOffset = caret.getOffset();
            int endSelectionOffset = caret.getOffset();
            if (caret.hasSelection()) {
                startSelectionOffset = caret.getSelectionStart();
                endSelectionOffset = caret.getSelectionEnd();
            }

            var initialOffsets = new Offsets(startSelectionOffset, endSelectionOffset);
            navigator.findNext(initialOffsets).ifPresent((offsets) -> {
                caret.setSelection(offsets.leftOffset(), offsets.rightOffset());
                caret.moveToOffset(offsets.leftOffset());
            });

            caretPositions.add(caret.getLogicalPosition());
        }

        scrollToFirstOrLast(caretPositions, editor);

        vimEditor.setMode(new Mode.VISUAL(SelectionType.CHARACTER_WISE, new Mode.NORMAL()));
    }

    private void scrollToFirstOrLast(List<LogicalPosition> caretPositions, Editor editor) {
        Function<List<LogicalPosition>, LogicalPosition> getFirstOrLast = switch (direction) {
            case FORWARD -> List::getLast;
            case BACKWARD -> List::getFirst;
        };
        caretPositions.sort(Comparator.comparingInt(LogicalPosition::getLine));
        editor.getScrollingModel().scrollTo(getFirstOrLast.apply(caretPositions), ScrollType.MAKE_VISIBLE);
    }
}
