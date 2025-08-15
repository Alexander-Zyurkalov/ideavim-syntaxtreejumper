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
import com.zyurkalov.ideavim.syntaxtreejumper.config.MotionHandlerConfig;
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

        registerStructuredMotionHandlers();

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
     * Registers motion handlers using the structured configuration approach.
     */
    private void registerStructuredMotionHandlers() {
        MotionHandlerConfig[] motionConfigs = {
                new MotionHandlerConfig("ArgumentList", "a", ArgumentParameterListMotionHandler::new),
                new MotionHandlerConfig("Statement", "s", StatementMotionHandler::new),
                new MotionHandlerConfig("LoopConditional", "l", LoopConditionalMotionHandler::new),
                new MotionHandlerConfig("MethodFunction", "f", MethodFunctionMotionHandler::new)
        };

        for (MotionHandlerConfig config : motionConfigs) {
            registerMotionHandler(config);
        }
    }

    /**
     * Registers a single motion handler configuration.
     */
    private void registerMotionHandler(MotionHandlerConfig config) {
        // Register backward handler
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(config.getBackwardCommand()),
                getOwner(),
                new FunctionHandler(Direction.BACKWARD, config.handlerFactory()),
                false);

        // Register forward handler
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(config.getForwardCommand()),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, config.handlerFactory()),
                false);

        // Map backward shortcut
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(config.getBackwardShortcut()),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(config.getBackwardCommand()),
                true);

        // Map forward shortcut
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(config.getForwardShortcut()),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(config.getForwardCommand()),
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