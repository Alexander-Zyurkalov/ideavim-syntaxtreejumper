package com.zyurkalov.ideavim.syntaxtreejumper;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Global keyboard interceptor similar to AceJump's approach.
 * Replaces the global TypedActionHandler to intercept all typed characters.
 */
public class GlobalKeyInterceptor implements TypedActionHandler {
    
    private static final GlobalKeyInterceptor INSTANCE = new GlobalKeyInterceptor();
    private final Map<Editor, TypedActionHandler> attachedHandlers = new HashMap<>();
    private TypedActionHandler originalHandler;
    private boolean isActive = false;
    
    private GlobalKeyInterceptor() {}
    
    public static GlobalKeyInterceptor getInstance() {
        return INSTANCE;
    }
    
    @Override
    public void execute(@NotNull Editor editor, char charTyped, @NotNull DataContext dataContext) {
        // If we have a custom handler for this editor, use it
        TypedActionHandler customHandler = attachedHandlers.get(editor);
        if (customHandler != null) {
            customHandler.execute(editor, charTyped, dataContext);
            return;
        }
        
        // Otherwise, use the original handler
        if (originalHandler != null) {
            originalHandler.execute(editor, charTyped, dataContext);
        }
    }
    
    /**
     * Attaches a custom typed action handler for the specified editor.
     * If this is the first attachment, replaces the global handler.
     */
    public void attach(Editor editor, TypedActionHandler handler) {
        if (!isActive) {
            // Save the original handler and replace it with ours
            TypedAction typedAction = TypedAction.getInstance();
            originalHandler = typedAction.getRawHandler();
            typedAction.setupRawHandler(this);
            isActive = true;
        }
        
        attachedHandlers.put(editor, handler);
    }
    
    /**
     * Detaches the custom handler for the specified editor.
     * If this was the last attachment, restores the original global handler.
     */
    public void detach(Editor editor) {
        attachedHandlers.remove(editor);
        
        if (attachedHandlers.isEmpty() && isActive) {
            // Restore the original handler
            if (originalHandler != null) {
                TypedAction.getInstance().setupRawHandler(originalHandler);
                originalHandler = null;
            }
            isActive = false;
        }
    }
    
    /**
     * Checks if an editor has a custom handler attached
     */
    public boolean isAttached(Editor editor) {
        return attachedHandlers.containsKey(editor);
    }
    
    /**
     * Emergency cleanup - detaches all handlers and restores original
     */
    public void detachAll() {
        attachedHandlers.clear();
        if (isActive && originalHandler != null) {
            TypedAction.getInstance().setupRawHandler(originalHandler);
            originalHandler = null;
            isActive = false;
        }
    }
}