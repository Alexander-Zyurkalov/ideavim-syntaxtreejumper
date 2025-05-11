package com.zyurkalov.ideavim.syntaxtreejumper;

import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.extension.VimExtension;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.SameLevelElementsNavigator;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.SubWordNavigator;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

import static com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping;
import static com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing;

public class SyntaxTreeJumper implements VimExtension {

    @Override
    public @NotNull String getName() {
        return "syntaxtreejumper";
    }

    @Override
    public void init() {
        // Register the extension handlers with <Plug> mappings
        String commandJumpToPrevElement = "<Plug>JumpToPrevElement";
        String commandJumpToNextElement = "<Plug>JumpToNextElement";
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToNextElement),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, SameLevelElementsNavigator::new),
                false);
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToPrevElement),
                getOwner(),
                new FunctionHandler(Direction.BACKWARD, SameLevelElementsNavigator::new),
                false);
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-w>"),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, SubWordNavigator::new),
                false);
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-S-w>"),
                getOwner(),
                new FunctionHandler(Direction.BACKWARD, SubWordNavigator::new),
                false);

        // Map the default key bindings to the <Plug> mappings

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-n>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToNextElement),
                true);
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-S-n>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToPrevElement),
                true);
    }

}
