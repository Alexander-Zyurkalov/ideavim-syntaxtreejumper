package com.zyurkalov.ideavim.syntaxtreejumper.config;

import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.MotionHandler;

import java.util.function.BiFunction;

/**
 * Configuration record for registering motion handlers with their commands and shortcuts.
 */
public record MotionHandlerConfig(
        String name,                    // Used for generating command names like "BackwardArgument", "ForwardArgument"
        ShortcutConfig[] shortcuts,     // Array of shortcut configurations
        BiFunction<SyntaxTreeAdapter, MotionDirection, MotionHandler> handlerFactory
) {

    public String getBackwardCommand() {
        return "<Plug>Backward" + name;
    }

    public String getForwardCommand() {
        return "<Plug>Forward" + name;
    }

    public String getExtendBackwardCommand() {
        return "<Plug>ExtendBackward" + name;
    }

    public String getExtendForwardCommand() {
        return "<Plug>ExtendForward" + name;
    }
}