package com.zyurkalov.ideavim.syntaxtreejumper.config;

import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.MotionHandler;

import java.util.function.BiFunction;

/**
 * Configuration record for registering motion handlers with their commands and shortcuts.
 */
public record MotionHandlerConfig(
        String name,                    // Used for generating command names like "BackwardArgument", "ForwardArgument"
        String shortcutLetter,         // The letter used in shortcuts like "a" for "[a" and "]a"
        BiFunction<SyntaxTreeAdapter, Direction, MotionHandler> handlerFactory
) {
    
    public String getBackwardCommand() {
        return "<Plug>Backward" + name;
    }
    
    public String getForwardCommand() {
        return "<Plug>Forward" + name;
    }
    
    public String getBackwardShortcut() {
        return "[" + shortcutLetter;
    }
    
    public String getForwardShortcut() {
        return "]" + shortcutLetter;
    }
}