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
import com.zyurkalov.ideavim.syntaxtreejumper.highlighting.ToggleHighlightingHandler;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.SameLevelElementsMotionHandler;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.SubWordMotionHandler;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.SyntaxNodeTreeHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumSet;

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;
import static com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping;
import static com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing;

public class SyntaxTreeJumper implements VimExtension, Disposable {

    @Override
    public @NotNull String getName() {
        return "syntaxtreejumper";
    }

    @Override
    public void init() {
        // Register the extension handlers with <Plug> mappings
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

        // Note: SubWordMotionHandler still uses PsiFile directly, so we need a wrapper
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-w>"),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, (syntaxTree, direction) -> {
                    // SubWordMotionHandler still needs the PsiFile, so we extract it if using PSI adapter
                    if (syntaxTree instanceof com.zyurkalov.ideavim.syntaxtreejumper.adapters.PsiSyntaxTreeAdapter(
                            com.intellij.psi.PsiFile psiFile
                    )) {
                        // We'll need to add a getter method to PsiSyntaxTreeAdapter
                        return new SubWordMotionHandler(psiFile, direction);
                    }
                    throw new UnsupportedOperationException("SubWordMotionHandler currently only supports PSI-based syntax trees");
                }),
                false);
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-S-w>"),
                getOwner(),
                new FunctionHandler(Direction.BACKWARD, (syntaxTree, direction) -> {
                    if (syntaxTree instanceof com.zyurkalov.ideavim.syntaxtreejumper.adapters.PsiSyntaxTreeAdapter(
                            com.intellij.psi.PsiFile psiFile
                    )) {
                        return new SubWordMotionHandler(psiFile, direction);
                    }
                    throw new UnsupportedOperationException("SubWordMotionHandler currently only supports PSI-based syntax trees");
                }),
                false);

        // Map the default key bindings to the <Plug> mappings
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

        // Map the default key bindings (Alt-o and Alt-i like in Helix)
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

        // Highlighting toggle functionality
        String commandToggleHighlighting = "<Plug>ToggleHighlighting";
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandToggleHighlighting),
                getOwner(),
                new ToggleHighlightingHandler(),
                false);

        // Map Alt-h to toggle highlighting
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-h>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandToggleHighlighting),
                true);


        // Register sibling motion handlers
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

    // Map Alt-[ and Alt-] to sibling motion handlers
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

        // Set up automatic highlighting for all editors
        setupAutomaticHighlighting();
    }

    /**
     * Sets up automatic highlighting for existing and new editors.
     */
    private void setupAutomaticHighlighting() {
        // Set up highlighting for all currently open editors
        VimEditorGroup editorGroup = injector.getEditorGroup();
        Collection<VimEditor> allEditors = editorGroup.getEditors();

        for (VimEditor vimEditor : allEditors) {
            Editor editor = IjVimEditorKt.getIj(vimEditor);
            FunctionHandler.setupEditorHighlighting(editor, vimEditor);
        }


        // Set up listener for new editors using the non-deprecated method
        EditorFactoryListener editorFactoryListener = new EditorFactoryListener() {
            @Override
            public void editorCreated(@NotNull EditorFactoryEvent event) {
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

        // Use the non-deprecated method with Disposable
        EditorFactory.getInstance().addEditorFactoryListener(editorFactoryListener, this);
    }

    /**
     * No need for explicit cleanup - the listener will be automatically removed
     * when this Disposable is disposed of.
     */
    public void dispose() {

        // Clean up all remaining editors
        Editor[] allEditors = EditorFactory.getInstance().getAllEditors();
        for (Editor editor : allEditors) {
            FunctionHandler.cleanupEditor(editor);
        }
    }
}