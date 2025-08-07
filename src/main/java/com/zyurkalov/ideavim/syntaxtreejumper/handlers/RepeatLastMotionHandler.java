package com.zyurkalov.ideavim.syntaxtreejumper.handlers;

import com.maddyhome.idea.vim.api.ExecutionContext;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.command.OperatorArguments;
import com.maddyhome.idea.vim.extension.ExtensionHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static com.zyurkalov.ideavim.syntaxtreejumper.handlers.FunctionHandler.lastExecutedHandler;
import static com.zyurkalov.ideavim.syntaxtreejumper.handlers.FunctionHandler.lastExecutedHandlerArguments;

/**
 * Handler that repeats the last executed FunctionHandler motion.
 * This handler does not store itself as the last executed motion to prevent infinite recursion.
 */
public class RepeatLastMotionHandler implements ExtensionHandler {
    
    @Override
    public void execute(
            @NotNull VimEditor vimEditor,
            @NotNull ExecutionContext context,
            @NotNull OperatorArguments operatorArguments) {
        
        // Check if there's a last executed handler to repeat
        if (lastExecutedHandler.isEmpty() || lastExecutedHandlerArguments.isEmpty()) {
            // No motion to repeat - silently return
            return;
        }
        
        FunctionHandler handlerToRepeat = lastExecutedHandler.get();
        OperatorArguments lastArguments = lastExecutedHandlerArguments.get();
        
        lastExecutedHandler = Optional.empty();
        lastExecutedHandlerArguments = Optional.empty();
        
        try {
            operatorArguments = new OperatorArguments(
            operatorArguments.getCount1() * lastArguments.getCount1(), operatorArguments.component3());
            handlerToRepeat.execute(vimEditor, context, operatorArguments);
        } finally {
            // Restore the original handler as the last executed motion
            // This ensures that later repeats will still work
            lastExecutedHandler = Optional.of(handlerToRepeat);
            lastExecutedHandlerArguments = Optional.of(lastArguments);
        }
    }
}

