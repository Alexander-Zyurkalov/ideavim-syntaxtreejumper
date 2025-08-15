package com.zyurkalov.ideavim.syntaxtreejumper;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.api.VimEditorGroup;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.extension.VimExtension;
import com.maddyhome.idea.vim.newapi.IjVimEditorKt;
import com.zyurkalov.ideavim.syntaxtreejumper.handlers.FunctionHandler;
import com.zyurkalov.ideavim.syntaxtreejumper.handlers.MoveSiblingHandler;
import com.zyurkalov.ideavim.syntaxtreejumper.handlers.RepeatLastMotionHandler;
import com.zyurkalov.ideavim.syntaxtreejumper.highlighting.ToggleHighlightingHandler;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumSet;

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;
import static com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping;
import static com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing;

public class SyntaxTreeJumper implements VimExtension, Disposable {

    private EditorFactoryListener editorFactoryListener;
    private boolean isDisposed = false;

    @Override
    public @NotNull String getName() {
        return "syntaxtreejumper";
    }

    @Override
    public void init() {

        registerBasicElementNavigation();

        registerSelectionHandlers();

        registerSpecialHandlers();

        // Argument/Parameter List navigation functionality
        String commandBackwardArgumentList = "<Plug>BackwardArgumentList";
        String commandForwardArgumentList = "<Plug>ForwardArgumentList";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandBackwardArgumentList),
                getOwner(),
                new FunctionHandler(Direction.BACKWARD, ArgumentParameterListMotionHandler::new),
                false);

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandForwardArgumentList),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, ArgumentParameterListMotionHandler::new),
                false);

        // Map [-a to backward argument/parameter list navigation
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("[a"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandBackwardArgumentList),
                true);

        // Map ]-a to forward argument/parameter list navigation
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("]a"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandForwardArgumentList),
                true);

        // Map ]-a to forward argument/parameter list navigation
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("]a"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandForwardArgumentList),
                true);

        // Statement navigation functionality
        String commandBackwardStatement = "<Plug>BackwardStatement";
        String commandForwardStatement = "<Plug>ForwardStatement";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandBackwardStatement),
                getOwner(),
                new FunctionHandler(Direction.BACKWARD, StatementMotionHandler::new),
                false);

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandForwardStatement),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, StatementMotionHandler::new),
                false);

        // Map [-s to backward statement navigation
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("[s"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandBackwardStatement),
                true);

        // Map ]-s to forward statement navigation
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("]s"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandForwardStatement),
                true);

        // Loop and conditional statement navigation functionality
        String commandBackwardLoopConditional = "<Plug>BackwardLoopConditional";
        String commandForwardLoopConditional = "<Plug>ForwardLoopConditional";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandBackwardLoopConditional),
                getOwner(),
                new FunctionHandler(Direction.BACKWARD, LoopConditionalMotionHandler::new),
                false);

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandForwardLoopConditional),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, LoopConditionalMotionHandler::new),
                false);

        // Map [-l to backward loop/conditional statement navigation
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("[l"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandBackwardLoopConditional),
                true);

        // Map ]-l to forward loop/conditional statement navigation
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("]l"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandForwardLoopConditional),
                true);

        // Method/Function navigation functionality
        String commandBackwardMethodFunction = "<Plug>BackwardMethodFunction";
        String commandForwardMethodFunction = "<Plug>ForwardMethodFunction";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandBackwardMethodFunction),
                getOwner(),
                new FunctionHandler(Direction.BACKWARD, MethodFunctionMotionHandler::new),
                false);

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandForwardMethodFunction),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, MethodFunctionMotionHandler::new),
                false);

// Map [-f to backward method/function navigation
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("[f"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandBackwardMethodFunction),
                true);

// Map ]-f to forward method/function navigation
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("]f"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandForwardMethodFunction),
                true);

        // Set up automatic highlighting for all editors
        setupAutomaticHighlighting();
    }

    private void registerBasicElementNavigation() {
        String commandJumpToPrevElement = "<Plug>JumpToPrevElement";
        String commandJumpToNextElement = "<Plug>JumpToNextElement";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToNextElement),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, SameLevelElementsMotionHandler::new),
                false);

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToPrevElement),
                getOwner(),
                new FunctionHandler(Direction.BACKWARD, SameLevelElementsMotionHandler::new),
                false);

        // Map the default key bindings
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-n>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToNextElement),
                true);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-S-n>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToPrevElement),
                true);

        // Selection-extending versions
        String commandJumpToPrevElementExtend = "<Plug>ExtendJumpToPrevElement";
        String commandJumpToNextElementExtend = "<Plug>ExtendJumpToNextElement";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToNextElementExtend),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, SameLevelElementsMotionHandler::new, true),
                false);

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToPrevElementExtend),
                getOwner(),
                new FunctionHandler(Direction.BACKWARD, SameLevelElementsMotionHandler::new, true),
                false);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<C-A-n>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToNextElementExtend),
                true);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<C-A-S-n>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToPrevElementExtend),
                true);
    }

    private void registerSelectionHandlers() {
        // Selection expansion/shrinking handlers
        String commandExpandSelection = "<Plug>ExpandSelection";
        String commandShrinkSelection = "<Plug>ShrinkSelection";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandExpandSelection),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, (syntaxTree, direction) ->
                        SyntaxNodeTreeHandler.createExpandHandler(syntaxTree)),
                false);

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandShrinkSelection),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, (syntaxTree, direction) ->
                        SyntaxNodeTreeHandler.createShrinkHandler(syntaxTree)),
                false);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-o>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandExpandSelection),
                true);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-i>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandShrinkSelection),
                true);

        // Smart Selection Extend Handler
        String commandSmartSelectionExtend = "<Plug>SmartSelectionExtend";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandSmartSelectionExtend),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, (syntaxTree, direction) ->
                        new SmartSelectionExtendHandler(syntaxTree)),
                false);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-e>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandSmartSelectionExtend),
                true);
    }

    /**
     * Registers special handlers that don't follow the standard pattern.
     */
    private void registerSpecialHandlers() {
        // SubWord motion handlers (special case - uses PsiFile directly)
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-w>"),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, (syntaxTree, direction) -> {
                    return new SubWordMotionHandler(syntaxTree.getPsiFile(), direction);
                }),
                false);

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-S-w>"),
                getOwner(),
                new FunctionHandler(Direction.BACKWARD, (syntaxTree, direction) -> {
                    return new SubWordMotionHandler(syntaxTree.getPsiFile(), direction);
                }),
                false);

        // Selection-extending versions of subword motions
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<C-A-w>"),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, (syntaxTree, direction) -> {
                    return new SubWordMotionHandler(syntaxTree.getPsiFile(), direction);
                }, true),
                false);

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<C-A-S-w>"),
                getOwner(),
                new FunctionHandler(Direction.BACKWARD, (syntaxTree, direction) -> {
                    return new SubWordMotionHandler(syntaxTree.getPsiFile(), direction);
                }, true),
                false);

        // Highlighting toggle
        String commandToggleHighlighting = "<Plug>ToggleHighlighting";
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandToggleHighlighting),
                getOwner(),
                new ToggleHighlightingHandler(),
                false);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-h>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandToggleHighlighting),
                true);

        // Sibling motion handlers (special case - doesn't use BiFunction pattern)
        String commandJumpToPrevSibling = "<Plug>JumpToPrevSibling";
        String commandJumpToNextSibling = "<Plug>JumpToNextSibling";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToPrevSibling),
                getOwner(),
                new MoveSiblingHandler(Direction.BACKWARD),
                false);

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToNextSibling),
                getOwner(),
                new MoveSiblingHandler(Direction.FORWARD),
                false);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-[>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToPrevSibling),
                true);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-]>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToNextSibling),
                true);

        // Repeat last motion
        String commandRepeatLastMotion = "<Plug>RepeatLastMotion";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandRepeatLastMotion),
                getOwner(),
                new RepeatLastMotionHandler(),
                false);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-r>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandRepeatLastMotion),
                true);
    }

    /**
     * Sets up automatic highlighting for existing and new editors.
     */
    private void setupAutomaticHighlighting() {
        if (isDisposed) {
            return;
        }

        // Set up highlighting for all currently open editors
        VimEditorGroup editorGroup = injector.getEditorGroup();
        Collection<VimEditor> allEditors = editorGroup.getEditors();

        for (VimEditor vimEditor : allEditors) {
            Editor editor = IjVimEditorKt.getIj(vimEditor);
            FunctionHandler.setupEditorHighlighting(editor, vimEditor);
        }

        // Create and store the listener reference
        editorFactoryListener = new EditorFactoryListener() {
            @Override
            public void editorCreated(@NotNull EditorFactoryEvent event) {
                if (isDisposed) {
                    return;
                }
                Editor editor = event.getEditor();
                var vimEditor = IjVimEditorKt.getVim(editor);
                FunctionHandler.setupEditorHighlighting(editor, vimEditor);
            }

            @Override
            public void editorReleased(@NotNull EditorFactoryEvent event) {
                Editor editor = event.getEditor();
                FunctionHandler.cleanupEditor(editor);
            }
        };

        // Register the listener manually so we can control its lifecycle
        EditorFactory.getInstance().addEditorFactoryListener(editorFactoryListener);
    }

    @Override
    public void dispose() {
        if (isDisposed) {
            return;
        }

        isDisposed = true;

        // Remove the editor factory listener
        if (editorFactoryListener != null) {
            EditorFactory.getInstance().removeEditorFactoryListener(editorFactoryListener);
            editorFactoryListener = null;
        }

        // Clean up all remaining editors
        Editor[] allEditors = EditorFactory.getInstance().getAllEditors();
        for (Editor editor : allEditors) {
            FunctionHandler.cleanupEditor(editor);
        }
    }
}