package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;

import java.util.Optional;

public interface MotionHandler {
    Optional<Offsets> findNext(Offsets initialOffsets);
}
