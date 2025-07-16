package com.zyurkalov.ideavim.syntaxtreejumper.motions.argument_motion;

import com.intellij.psi.PsiElement;

import java.util.List;

public interface ArgumentContext {

    List<PsiElement> getArguments();
}
