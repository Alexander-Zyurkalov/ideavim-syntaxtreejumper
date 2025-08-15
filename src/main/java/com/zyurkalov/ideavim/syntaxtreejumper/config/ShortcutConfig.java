package com.zyurkalov.ideavim.syntaxtreejumper.config;

import com.zyurkalov.ideavim.syntaxtreejumper.Direction;

/**
 * Configuration record for individual shortcuts.
 */
public record ShortcutConfig(
        String keySequence,
        Direction direction,
        boolean addNewCaret
) {}