package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;

import java.util.Optional;

public interface Navigator {
    Optional<Offsets> findNextObjectsOffsets(Offsets initialOffsets);
}
