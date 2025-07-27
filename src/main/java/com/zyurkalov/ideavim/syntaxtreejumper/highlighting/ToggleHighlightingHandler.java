package com.zyurkalov.ideavim.syntaxtreejumper.highlighting;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.maddyhome.idea.vim.api.ExecutionContext;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.api.VimEditorGroup;
import com.maddyhome.idea.vim.command.OperatorArguments;
import com.maddyhome.idea.vim.extension.ExtensionHandler;
import com.maddyhome.idea.vim.newapi.IjVimEditorKt;
import com.zyurkalov.ideavim.syntaxtreejumper.handlers.FunctionHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;

/**
 * Handler for toggling PSI element highlighting on/off.
 * Now integrates with the dynamic highlighting system.
 */
public class ToggleHighlightingHandler implements ExtensionHandler {

    @Override
    public void execute(
            @NotNull VimEditor vimEditor,
            @NotNull ExecutionContext context,
            @NotNull OperatorArguments operatorArguments) {

        HighlightingConfig config = HighlightingConfig.getInstance();
        config.toggleHighlighting();

        if (config.isHighlightingEnabled()) {
            // When enabling highlighting, set up highlighting for all editors and update them
            setupAndUpdateAllEditors();
        } else {
            // When disabling highlighting, clear all existing highlights
            clearAllHighlights();
        }

        // Print status to console
        System.out.println("PSI Element Highlighting: " +
                (config.isHighlightingEnabled() ? "ENABLED" : "DISABLED"));
    }

    /**
     * Sets up highlighting listeners and updates highlighting for all open editors.
     */
    private void setupAndUpdateAllEditors() {
        VimEditorGroup editorGroup = injector.getEditorGroup();
        Collection<VimEditor> allEditors = editorGroup.getEditors();

        for (VimEditor vimEditor : allEditors) {
            Editor editor = IjVimEditorKt.getIj(vimEditor);
            FunctionHandler.setupEditorHighlighting(editor, vimEditor);
        }
    }

    /**
     * Clears highlights from all open editors.
     */
    private void clearAllHighlights() {
        Editor[] allEditors = EditorFactory.getInstance().getAllEditors();
        for (Editor editor : allEditors) {
            FunctionHandler.clearHighlightsForEditor(editor);
        }
    }
}