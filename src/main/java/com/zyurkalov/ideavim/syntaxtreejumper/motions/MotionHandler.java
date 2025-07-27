package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;

import java.util.Optional;

/**
 * Interface for all motion handlers in the syntax tree jumper plugin.
 * Motion handlers are responsible for finding the next position to navigate to
 * based on the current cursor/selection position.
 */
public interface MotionHandler {

    /**
     * Finds the next position to navigate to from the given initial offsets.
     *
     * @param initialOffsets The current cursor position or selection range
     * @return Optional containing the new offsets or empty if no valid next position found
     */
    Optional<Offsets> findNext(Offsets initialOffsets);
}
