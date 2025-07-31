package com.zyurkalov.ideavim.syntaxtreejumper.handlers;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.event.SelectionListener;
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
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapterFactory;
import com.zyurkalov.ideavim.syntaxtreejumper.highlighting.HighlightingConfig;
import com.zyurkalov.ideavim.syntaxtreejumper.highlighting.PsiElementHighlighter;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.MotionHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FunctionHandler implements ExtensionHandler {

    private final Direction direction;
    private final BiFunction<SyntaxTreeAdapter, Direction, MotionHandler> navigatorFactory;

    // Static map to track highlighters per editor to avoid conflicts
    private static final ConcurrentHashMap<Editor, PsiElementHighlighter> editorHighlighters =
            new ConcurrentHashMap<>();

    // Static map to track selection listeners per editor
    private static final ConcurrentHashMap<Editor, SelectionListener> editorSelectionListeners =
            new ConcurrentHashMap<>();

    // Static map to track caret listeners per editor
    private static final ConcurrentHashMap<Editor, CaretListener> editorCaretListeners =
            new ConcurrentHashMap<>();


    public FunctionHandler(Direction direction, BiFunction<SyntaxTreeAdapter, Direction, MotionHandler> navigatorFactory) {
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

        // Get or create the syntax tree adapter for this editor
        SyntaxTreeAdapter syntaxTree = SyntaxTreeAdapterFactory.createAdapter(psiFile);

        MotionHandler navigator = navigatorFactory.apply(syntaxTree, direction);
        List<LogicalPosition> caretPositions = new ArrayList<>();
        List<Caret> carets = editor.getCaretModel().getAllCarets();

        // Ensure highlighter and listeners are set up for this editor
        setupEditorHighlighting(editor, vimEditor );

        boolean anyMotionExecuted = false;

        for (Caret caret : carets) {
            int startSelectionOffset = caret.getOffset();
            int endSelectionOffset = caret.getOffset();
            if (caret.hasSelection()) {
                startSelectionOffset = caret.getSelectionStart();
                endSelectionOffset = caret.getSelectionEnd();
            }

            var initialOffsets = new Offsets(startSelectionOffset, endSelectionOffset);
            var optionalOffsets = navigator.findNext(initialOffsets);

            if (optionalOffsets.isPresent()) {
                Offsets offsets = optionalOffsets.get();
                caret.setSelection(offsets.leftOffset(), offsets.rightOffset());
                caret.moveToOffset(offsets.leftOffset());
                anyMotionExecuted = true;
            }

            caretPositions.add(caret.getLogicalPosition());
        }

        // Update highlighting based on new positions
        if (anyMotionExecuted) {
            updateHighlightingForEditor(editor);
        }

        scrollToFirstOrLast(caretPositions, editor);
        vimEditor.setMode(new Mode.VISUAL(SelectionType.CHARACTER_WISE, new Mode.NORMAL()));
    }


    /**
     * Sets up highlighting, caret, and selection listeners for the given editor if not already present.
     */
    public static void setupEditorHighlighting(@NotNull Editor editor, VimEditor vimEditor) {
        // Set up highlighter
        editorHighlighters.computeIfAbsent(editor, PsiElementHighlighter::new);


        // Set up a caret listener
        editorCaretListeners.computeIfAbsent(editor, e -> {
            CaretListener caretListener = new CaretListener() {
                @Override
                public void caretPositionChanged(@NotNull CaretEvent event) {
                    var mode = vimEditor.getMode();
                    if (mode instanceof Mode.INSERT)
                        clearHighlightsForEditor(editor);
                    else
                        updateHighlightingForEditor(editor);
                }
            };
            editor.getCaretModel().addCaretListener(caretListener);
            return caretListener;
        });


        // Set up a selection listener
        editorSelectionListeners.computeIfAbsent(editor, e -> {
            SelectionListener selectionListener = new SelectionListener() {
                @Override
                public void selectionChanged(@NotNull SelectionEvent event) {
                    var mode = vimEditor.getMode();
                    if (mode instanceof Mode.INSERT)
                        clearHighlightsForEditor(editor);
                    else
                        updateHighlightingForEditor(editor);
                }
            };
            editor.getSelectionModel().addSelectionListener(selectionListener);
            return selectionListener;
        });


        // Initial highlighting update
        updateHighlightingForEditor(editor);
    }

    /**
     * Updates highlighting for all carets in the given editor.
     */
    public static void updateHighlightingForEditor(@NotNull Editor editor) {
        HighlightingConfig config = HighlightingConfig.getInstance();

        if (!config.isHighlightingEnabled()) {
            clearHighlightsForEditor(editor);
            return;
        }

        if (editor.getProject() == null) return;
        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (file == null) return;
        PsiFile psiFile = PsiManager.getInstance(editor.getProject()).findFile(file);
        if (psiFile == null) return;

        // Get the syntax tree adapter for this editor - this will now use language detection
        SyntaxTreeAdapter syntaxTree = SyntaxTreeAdapterFactory.createAdapter(psiFile);

        PsiElementHighlighter highlighter = editorHighlighters.get(editor);
        if (highlighter == null) return;

        // Clear existing highlights
        highlighter.clearHighlights();

        // Check if there's any selection in any caret
        boolean hasAnySelection = editor.getCaretModel().getAllCarets().stream()
                .anyMatch(Caret::hasSelection);

        Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        if (hasAnySelection) {
            // If there's selection, highlight the primary caret's selection
            if (primaryCaret.hasSelection()) {
                highlighter.highlightElementAndSiblings(
                        syntaxTree,
                        primaryCaret.getSelectionStart(),
                        primaryCaret.getSelectionEnd()
                );
            } else {
                // Find the first caret with selection
                for (Caret caret : editor.getCaretModel().getAllCarets()) {
                    if (caret.hasSelection()) {
                        highlighter.highlightElementAndSiblings(
                                syntaxTree,
                                caret.getSelectionStart(),
                                caret.getSelectionEnd()
                        );
                        break;
                    }
                }
            }
        } else {
            // If no selection, highlight based on the primary caret position
            int offset = primaryCaret.getOffset();
            highlighter.highlightElementAndSiblings(syntaxTree, offset, offset);
        }
    }

    private void scrollToFirstOrLast(List<LogicalPosition> caretPositions, Editor editor) {
        Function<List<LogicalPosition>, LogicalPosition> getFirstOrLast = switch (direction) {
            case FORWARD -> List::getLast;
            case BACKWARD -> List::getFirst;
        };
        caretPositions.sort(Comparator.comparingInt(LogicalPosition::getLine));
        editor.getScrollingModel().scrollTo(getFirstOrLast.apply(caretPositions), ScrollType.MAKE_VISIBLE);
    }

    /**
     * Manually clear highlights for a specific editor.
     * This can be called from external code if needed.
     */
    public static void clearHighlightsForEditor(@NotNull Editor editor) {
        PsiElementHighlighter highlighter = editorHighlighters.get(editor);
        if (highlighter != null) {
            highlighter.clearHighlights();
        }
    }

    /**
     * Clean up highlighter, caret listener, selection listener, and syntax tree adapter when editor is disposed.
     * Should be called from editor disposal listeners.
     */
    public static void cleanupEditor(@NotNull Editor editor) {
        // Remove and clean up highlighter
        PsiElementHighlighter highlighter = editorHighlighters.remove(editor);
        if (highlighter != null) {
            highlighter.clearHighlights();
        }

        // Remove selection listener
        SelectionListener selectionListener = editorSelectionListeners.remove(editor);
        if (selectionListener != null) {
            editor.getSelectionModel().removeSelectionListener(selectionListener);
        }

        // Remove caret listener
        CaretListener caretListener = editorCaretListeners.remove(editor);
        if (caretListener != null) {
            editor.getCaretModel().removeCaretListener(caretListener);
        }


    }
}