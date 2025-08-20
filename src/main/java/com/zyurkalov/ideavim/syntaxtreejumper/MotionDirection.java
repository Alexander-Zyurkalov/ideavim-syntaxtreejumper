package com.zyurkalov.ideavim.syntaxtreejumper;

public enum MotionDirection {
    BACKWARD,
    FORWARD,
    EXPAND,  // Alt-o: expand selection to parent
    SHRINK   // Alt-i: shrink selection to children
}
