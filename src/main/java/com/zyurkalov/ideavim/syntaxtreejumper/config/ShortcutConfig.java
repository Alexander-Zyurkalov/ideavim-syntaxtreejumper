package com.zyurkalov.ideavim.syntaxtreejumper.config;

import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;

/**
 * Configuration record for individual shortcuts.
 */
public record ShortcutConfig(
        String keySequence,
        MotionDirection direction,
        boolean addNewCaret
) {}