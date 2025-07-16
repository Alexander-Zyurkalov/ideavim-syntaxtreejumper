package com.zyurkalov.ideavim.syntaxtreejumper;

import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.extension.VimExtension;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.SameLevelElementsMotionHandler;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.SubWordMotionHandler;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.SyntaxNodeTreeHandler;
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
                new FunctionHandler(Direction.FORWARD, SameLevelElementsMotionHandler::new),
                false);
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToPrevElement),
                getOwner(),
                new FunctionHandler(Direction.BACKWARD, SameLevelElementsMotionHandler::new),
                false);
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-w>"),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, SubWordMotionHandler::new),
                false);
        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-S-w>"),
                getOwner(),
                new FunctionHandler(Direction.BACKWARD, SubWordMotionHandler::new),
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


        String commandExpandSelection = "<Plug>ExpandSelection";
        String commandShrinkSelection = "<Plug>ShrinkSelection";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandExpandSelection),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, (psiFile, direction) ->
                        SyntaxNodeTreeHandler.createExpandHandler(psiFile)),
                false);

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandShrinkSelection),
                getOwner(),
                new FunctionHandler(Direction.FORWARD, (psiFile, direction) ->
                        SyntaxNodeTreeHandler.createShrinkHandler(psiFile)),
                false);

// Map the default key bindings (Alt-o and Alt-i like in Helix)
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-o>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandExpandSelection),
                true);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("<A-i>"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandShrinkSelection),
                true);

        // Register numbered jump handlers (0-9)
        for (int i = 0; i <= 9; i++) {
            String commandNumberedJump = "<Plug>NumberedJump" + i;
            final int number = i;
            putExtensionHandlerMapping(
                    EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                    VimInjectorKt.getInjector().getParser().parseKeys(commandNumberedJump),
                    getOwner(),
                    NumberedJumpFunctionHandler.createForNumber(number),
                    false);

            // Map Alt-j followed by the number
            String keySequence = "<A-j>" + i;
            putKeyMappingIfMissing(
                    EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                    VimInjectorKt.getInjector().getParser().parseKeys(keySequence),
                    getOwner(),
                    VimInjectorKt.getInjector().getParser().parseKeys(commandNumberedJump),
                    true);
        }
    }

}