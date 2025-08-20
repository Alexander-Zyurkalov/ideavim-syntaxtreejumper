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
import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection.*;

public class FunctionHandler implements ExtensionHandler {

    private final MotionDirection direction;
    private final BiFunction<SyntaxTreeAdapter, MotionDirection, MotionHandler> navigatorFactory;
    private final boolean addNewCaret;

    // Static variable to track the last executed FunctionHandler
    public static Optional<FunctionHandler> lastExecutedHandler = Optional.empty();
    public static Optional<OperatorArguments> lastExecutedHandlerArguments = Optional.empty();

    // Static map to track highlighters per editor to avoid conflicts
    private static final ConcurrentHashMap<Editor, PsiElementHighlighter> editorHighlighters =
            new ConcurrentHashMap<>();

    // Static map to track selection listeners per editor
    private static final ConcurrentHashMap<Editor, SelectionListener> editorSelectionListeners =
            new ConcurrentHashMap<>();

    // Static map to track caret listeners per editor
    private static final ConcurrentHashMap<Editor, CaretListener> editorCaretListeners =
            new ConcurrentHashMap<>();

    /**
     * Constructor for motion without adding a new caret (backward compatibility).
     */
    public FunctionHandler(MotionDirection direction, BiFunction<SyntaxTreeAdapter, MotionDirection, MotionHandler> navigatorFactory) {
        this(direction, navigatorFactory, false);
    }

    /**
     * Constructor with a new caret addition parameter.
     *
     * @param direction        The direction of the motion
     * @param navigatorFactory Factory to create the motion handler
     * @param addNewCaret      Whether to add a new caret with selection (true) or move existing carets (false)
     */
    public FunctionHandler(MotionDirection direction, BiFunction<SyntaxTreeAdapter, MotionDirection,
            MotionHandler> navigatorFactory, boolean addNewCaret
    ) {
        this.direction = direction;
        this.navigatorFactory = navigatorFactory;
        this.addNewCaret = addNewCaret;
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

        // Get the count from operatorArguments (defaults to 1 if no count provided)
        int count = operatorArguments.getCount1(); // This gets the count, defaulting to 1

        // Get or create the syntax tree adapter for this editor
        SyntaxTreeAdapter syntaxTree = SyntaxTreeAdapterFactory.createAdapter(psiFile);

        MotionHandler navigator = navigatorFactory.apply(syntaxTree, direction);
        List<LogicalPosition> caretPositionsToScrollTo = new ArrayList<>();
        List<Caret> carets = editor.getCaretModel().getAllCarets();

        // Ensure highlighter and listeners are set up for this editor
        setupEditorHighlighting(editor, vimEditor);

        boolean anyMotionExecuted = false;
        List<Offsets> newCaretOffsets = new ArrayList<>();

        // When creating new carets, we should only do that for frontier carets
        int start_caret = 0;
        int end_caret = carets.size() - 1;
        if (addNewCaret && (direction == EXPAND ||  direction == BACKWARD )) {
            end_caret = 0;
        } else if (addNewCaret && (direction == SHRINK || direction == FORWARD)) {
            start_caret = carets.size() - 1;
        }

        // Execute the motion 'count' times for each caret
        for (int caret_i = start_caret; caret_i <= end_caret; caret_i++) {
            Caret caret = carets.get(caret_i);
            int startSelectionOffset = caret.getOffset();
            int endSelectionOffset = caret.getOffset();
            if (caret.hasSelection()) {
                startSelectionOffset = caret.getSelectionStart();
                endSelectionOffset = caret.getSelectionEnd();
            }

            var currentOffsets = new Offsets(startSelectionOffset, endSelectionOffset);

            // Apply the motion 'count' times
            for (int i = 0; i < count; i++) {
                var optionalOffsets = navigator.findNext(currentOffsets);
                if (optionalOffsets.isPresent()) {
                    currentOffsets = optionalOffsets.get();
                    anyMotionExecuted = true;
                } else {
                    // If we can't find the next position, stop trying
                    break;
                }
            }

            // Only update position if we moved at least once
            if (anyMotionExecuted) {
                if (addNewCaret) {
                    newCaretOffsets.add(currentOffsets);
                } else {
                    caret.setSelection(currentOffsets.leftOffset(), currentOffsets.rightOffset());
                    caret.moveToOffset(currentOffsets.leftOffset());
                }
            }
        }

        // Create new carets for all found targets
        for (Offsets offsets : newCaretOffsets) {
            Caret newCaret = editor.getCaretModel().addCaret(
                    editor.offsetToLogicalPosition(offsets.leftOffset()),
                    true // make visible
            );
            if (newCaret != null) {
                newCaret.setSelection(offsets.leftOffset(), offsets.rightOffset());
                caretPositionsToScrollTo.add(newCaret.getLogicalPosition());
            }
        }

        // Also add positions of existing carets to decide where to scroll to.
        for (Caret caret : carets) {
            caretPositionsToScrollTo.add(caret.getLogicalPosition());
        }

        // Update highlighting based on new positions
        if (anyMotionExecuted) {
            updateHighlightingForEditor(editor);
        }

        scrollToFirstOrLast(caretPositionsToScrollTo, editor);

        // Set mode based on whether any motion was executed
        if (anyMotionExecuted) {
            lastExecutedHandler = Optional.of(this);
            lastExecutedHandlerArguments = Optional.of(operatorArguments);
            vimEditor.setMode(new Mode.VISUAL(SelectionType.CHARACTER_WISE, new Mode.NORMAL()));
        }
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
            case FORWARD, EXPAND -> List::getLast;
            case BACKWARD, SHRINK -> List::getFirst;
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