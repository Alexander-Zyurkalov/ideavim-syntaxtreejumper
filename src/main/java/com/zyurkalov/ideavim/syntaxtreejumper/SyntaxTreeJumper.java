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
import com.zyurkalov.ideavim.syntaxtreejumper.config.ShortcutConfig;
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
        registerStructuredMotionHandlers();
        registerSpecialHandlers();
        setupAutomaticHighlighting();
    }

    /**
     * Registers motion handlers using the structured configuration approach.
     */
    private void registerStructuredMotionHandlers() {
        MotionHandlerConfig[] motionConfigs = {
                // Basic element navigation
                new MotionHandlerConfig(
                        "Element",
                        new ShortcutConfig[]{
                                new ShortcutConfig("<A-n>", MotionDirection.FORWARD, false),
                                new ShortcutConfig("<A-S-n>", MotionDirection.BACKWARD, false),
                                new ShortcutConfig("<A-o>", MotionDirection.EXPAND, false),
                                new ShortcutConfig("<A-i>", MotionDirection.SHRINK, false),
                                new ShortcutConfig("<C-A-n>", MotionDirection.FORWARD, true),
                                new ShortcutConfig("<C-A-S-n>", MotionDirection.BACKWARD, true)
                        },
                        SyntaxTreeNodesMotionHandler::new
                ),

                // SubWord motion (special wrapper needed)
                new MotionHandlerConfig(
                        "SubWord",
                        new ShortcutConfig[]{
                                new ShortcutConfig("<A-w>", MotionDirection.FORWARD, false),
                                new ShortcutConfig("<A-S-w>", MotionDirection.BACKWARD, false),
                                new ShortcutConfig("<C-A-w>", MotionDirection.FORWARD, true),
                                new ShortcutConfig("<C-A-S-w>", MotionDirection.BACKWARD, true)
                        },
                        (syntaxTree, direction) -> new SubWordMotionHandler(syntaxTree.getPsiFile(), direction)
                ),

                // Argument/Parameter List navigation
                new MotionHandlerConfig(
                        "ArgumentList",
                        new ShortcutConfig[]{
                                new ShortcutConfig("[a", MotionDirection.BACKWARD, false),
                                new ShortcutConfig("]a", MotionDirection.FORWARD, false),
                                new ShortcutConfig("[A", MotionDirection.EXPAND, false),
                                new ShortcutConfig("]A", MotionDirection.SHRINK, false),
                                new ShortcutConfig("<C-[>a", MotionDirection.BACKWARD, true),
                                new ShortcutConfig("<C-]>a", MotionDirection.FORWARD, true)
                        },
                        ArgumentParameterListMotionHandler::new
                ),

                // Statement navigation
                new MotionHandlerConfig(
                        "Statement",
                        new ShortcutConfig[]{
                                new ShortcutConfig("[s", MotionDirection.BACKWARD, false),
                                new ShortcutConfig("[S", MotionDirection.EXPAND, false),
                                new ShortcutConfig("]s", MotionDirection.FORWARD, false),
                                new ShortcutConfig("]S", MotionDirection.SHRINK, false),
                                new ShortcutConfig("<C-[>s", MotionDirection.BACKWARD, true),
                                new ShortcutConfig("<C-]>s", MotionDirection.FORWARD, true)
                        },
                        StatementMotionHandler::new
                ),

                // Loop/Conditional navigation
                new MotionHandlerConfig(
                        "LoopConditional",
                        new ShortcutConfig[]{
                                new ShortcutConfig("[l", MotionDirection.BACKWARD, false),
                                new ShortcutConfig("]l", MotionDirection.FORWARD, false),
                                new ShortcutConfig("[L", MotionDirection.EXPAND, false),
                                new ShortcutConfig("]L", MotionDirection.SHRINK, false),
                                new ShortcutConfig("<C-[>l", MotionDirection.BACKWARD, true),
                                new ShortcutConfig("<C-]>l", MotionDirection.FORWARD, true)
                        },
                        LoopConditionalMotionHandler::new
                ),

                // Method/Function navigation
                new MotionHandlerConfig(
                        "MethodFunction",
                        new ShortcutConfig[]{
                                new ShortcutConfig("[f", MotionDirection.BACKWARD, false),
                                new ShortcutConfig("[F", MotionDirection.EXPAND, false),
                                new ShortcutConfig("]f", MotionDirection.FORWARD, false),
                                new ShortcutConfig("]F", MotionDirection.SHRINK, false),
                                new ShortcutConfig("<C-[>f", MotionDirection.BACKWARD, true),
                                new ShortcutConfig("<C-]>f", MotionDirection.FORWARD, true)
                        },
                        MethodFunctionMotionHandler::new
                ),

                // Operator navigation
                new MotionHandlerConfig(
                        "Operator",
                        new ShortcutConfig[]{
                                new ShortcutConfig("[o", MotionDirection.BACKWARD, false),
                                new ShortcutConfig("]o", MotionDirection.FORWARD, false),
                                new ShortcutConfig("[O", MotionDirection.EXPAND, false),
                                new ShortcutConfig("]O", MotionDirection.SHRINK, false),
                                new ShortcutConfig("<C-[>o", MotionDirection.BACKWARD, true),
                                new ShortcutConfig("<C-]>o", MotionDirection.FORWARD, true)
                        },
                        OperatorMotionHandler::new
                ),

                // Variable navigation (only forward/backward)
                new MotionHandlerConfig(
                        "Variable",
                        new ShortcutConfig[]{
                                new ShortcutConfig("[v", MotionDirection.BACKWARD, false),
                                new ShortcutConfig("]v", MotionDirection.FORWARD, false),
                                new ShortcutConfig("[V", MotionDirection.EXPAND, false),
                                new ShortcutConfig("]V", MotionDirection.SHRINK, false),
                                new ShortcutConfig("<C-[>v", MotionDirection.BACKWARD, true),
                                new ShortcutConfig("<C-]>v", MotionDirection.FORWARD, true)
                        },
                        VariableMotionHandler::new
                ),

                new MotionHandlerConfig(
                        "CodeBlock",
                        new ShortcutConfig[]{
                                new ShortcutConfig("[b", MotionDirection.BACKWARD, false),
                                new ShortcutConfig("]b", MotionDirection.FORWARD, false),
                                new ShortcutConfig("[B", MotionDirection.EXPAND, false),
                                new ShortcutConfig("]B", MotionDirection.SHRINK, false),
                                new ShortcutConfig("<C-[>B", MotionDirection.BACKWARD, true),
                                new ShortcutConfig("<C-]>B", MotionDirection.FORWARD, true)
                        },
                        CodeBlockMotionHandler::new
                ),

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
                new FunctionHandler(MotionDirection.BACKWARD, config.handlerFactory()),
                false);

        // Register forward handler
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(config.getForwardCommand()),
                getOwner(),
                new FunctionHandler(MotionDirection.FORWARD, config.handlerFactory()),
                false);

        // Register expand handler
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(config.getExpandCommand()),
                getOwner(),
                new FunctionHandler(MotionDirection.EXPAND, config.handlerFactory()),
                false);

        // Register shrink handler
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(config.getShrinkCommand()),
                getOwner(),
                new FunctionHandler(MotionDirection.SHRINK, config.handlerFactory()),
                false);

        // Register extend backward handler (for new caret)
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(config.getExtendBackwardCommand()),
                getOwner(),
                new FunctionHandler(MotionDirection.BACKWARD, config.handlerFactory(), true),
                false);

        // Register extend forward handler (for new caret)
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(config.getExtendForwardCommand()),
                getOwner(),
                new FunctionHandler(MotionDirection.FORWARD, config.handlerFactory(), true),
                false);

        // Register extend expand handler (for new caret)
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(config.getExtendExpandCommand()),
                getOwner(),
                new FunctionHandler(MotionDirection.EXPAND, config.handlerFactory(), true),
                false);

        // Register extend shrink handler (for new caret)
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(config.getExtendShrinkCommand()),
                getOwner(),
                new FunctionHandler(MotionDirection.SHRINK, config.handlerFactory(), true),
                false);

        // Map all shortcuts to their respective commands
        for (ShortcutConfig shortcut : config.shortcuts()) {
            String targetCommand = switch (shortcut.direction()) {
                case FORWARD -> shortcut.addNewCaret() ? config.getExtendForwardCommand() : config.getForwardCommand();
                case BACKWARD ->
                        shortcut.addNewCaret() ? config.getExtendBackwardCommand() : config.getBackwardCommand();
                case EXPAND -> shortcut.addNewCaret() ? config.getExtendExpandCommand() : config.getExpandCommand();
                case SHRINK -> shortcut.addNewCaret() ? config.getExtendShrinkCommand() : config.getShrinkCommand();
            };

            putKeyMappingIfMissing(
                    EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                    VimInjectorKt.getInjector().getParser().parseKeys(shortcut.keySequence()),
                    getOwner(),
                    VimInjectorKt.getInjector().getParser().parseKeys(targetCommand),
                    true);
        }
    }

    /**
     * Registers special handlers that don't follow the standard pattern.
     */
    private void registerSpecialHandlers() {
        // Smart Selection Extend Handler
        String commandSmartSelectionExtend = "<Plug>SmartSelectionExtend";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandSmartSelectionExtend),
                getOwner(),
                new FunctionHandler(MotionDirection.FORWARD, (syntaxTree, direction) ->
                        new SmartSelectionExtendHandler(syntaxTree)),
                false);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-e>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandSmartSelectionExtend),
                true);

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
        String commandMoveToPrevSibling = "<Plug>MoveToPrevSibling";
        String commandMoveToNextSibling = "<Plug>MoveToNextSibling";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandMoveToPrevSibling),
                getOwner(),
                new MoveSiblingHandler(MotionDirection.BACKWARD),
                false);

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandMoveToNextSibling),
                getOwner(),
                new MoveSiblingHandler(MotionDirection.FORWARD),
                false);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-[>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandMoveToPrevSibling),
                true);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-]>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandMoveToNextSibling),
                true);

        // Repeat the last motion
        String commandRepeatLastMotion = "<Plug>RepeatLastMotion";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandRepeatLastMotion),
                getOwner(),
                new RepeatLastMotionHandler(false),
                false);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-r>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandRepeatLastMotion),
                true);

        // Repeat the last motion
        String commandRepeatLastOppositeMotion = "<Plug>RepeatLastOppositeMotion";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandRepeatLastOppositeMotion),
                getOwner(),
                new RepeatLastMotionHandler(true),
                false);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-S-r>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandRepeatLastOppositeMotion),
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