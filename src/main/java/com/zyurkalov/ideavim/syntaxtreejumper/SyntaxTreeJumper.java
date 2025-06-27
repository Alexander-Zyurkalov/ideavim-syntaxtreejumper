package com.zyurkalov.ideavim.syntaxtreejumper;

import com.intellij.psi.PsiFile;
import com.maddyhome.idea.vim.api.VimInjectorKt;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.extension.VimExtension;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.ArgumentMotionHandler;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.SameLevelElementsMotionHandler;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.SubWordMotionHandler;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.SyntaxNodeTreeHandler;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.argument_motion.JavaContext;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.argument_motion.LanguageContext;
import com.zyurkalov.ideavim.syntaxtreejumper.motions.argument_motion.RustContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        // Argument navigation handlers
        String commandJumpToNextArgument = "<Plug>JumpToNextArgument";
        String commandJumpToPrevArgument = "<Plug>JumpToPrevArgument";

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToNextArgument),
                getOwner(),
                new FunctionHandler(Direction.FORWARD,
                        (psiFile, direction) -> new ArgumentMotionHandler(psiFile, direction,  getContext(psiFile, direction))),
                false);

        putExtensionHandlerMapping(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToPrevArgument),
                getOwner(),
                new FunctionHandler(Direction.BACKWARD,
                        (psiFile, direction) -> new ArgumentMotionHandler(psiFile, direction, getContext(psiFile, direction))),
                false);

        // Map the default key bindings for argument navigation (]a and [a)
        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("]a"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToNextArgument),
                true);

        putKeyMappingIfMissing(
                EnumSet.of(MappingMode.NORMAL, MappingMode.VISUAL),
                VimInjectorKt.getInjector().getParser().parseKeys("[a"),
                getOwner(),
                VimInjectorKt.getInjector().getParser().parseKeys(commandJumpToPrevArgument),
                true);
    }

    /**
     * Returns the appropriate LanguageContext based on the file type and available language support.
     * Currently, supports Java and Rust files when their respective plugins are available.
     *
     * @param psiFile The PSI file to determine the language context for
     * @param direction The direction of navigation
     * @return LanguageContext for the appropriate language, or null if not supported
     */
    private static @Nullable LanguageContext getContext(PsiFile psiFile, Direction direction) {
        if (psiFile == null) {
            return null;
        }

        // Check if this is a Java file
        if (isJavaFile(psiFile)) {
            // Check if Java plugin/support is available
            if (isJavaPluginAvailable()) {
                return new JavaContext(direction);
            }
        }

        // Check if this is a Rust file
        if (isRustFile(psiFile)) {
            // Check if Rust plugin/support is available
            if (isRustPluginAvailable()) {
                return new RustContext(direction);
            }
        }

        // TODO: Add support for other languages here
        // if (isJavaScriptFile(psiFile) && isJavaScriptPluginAvailable()) {
        //     return new JavaScriptContext(direction);
        // }
        // if (isPythonFile(psiFile) && isPythonPluginAvailable()) {
        //     return new PythonContext(direction);
        // }

        return null;
    }

    /**
     * Checks if the given PSI file is a Java file.
     */
    private static boolean isJavaFile(PsiFile psiFile) {
        String fileName = psiFile.getName();
        return fileName.endsWith(".java") ||
               "JAVA".equals(psiFile.getFileType().getName()) ||
               "Java".equals(psiFile.getLanguage().getID());
    }

    /**
     * Checks if the given PSI file is a Rust file.
     */
    private static boolean isRustFile(PsiFile psiFile) {
        String fileName = psiFile.getName();
        return fileName.endsWith(".rs") ||
               "Rust".equals(psiFile.getFileType().getName()) ||
               "Rust".equals(psiFile.getLanguage().getID()) ||
               "RUST".equals(psiFile.getFileType().getName());
    }

    /**
     * Checks if Java plugin/support is available in the current IDE.
     */
    private static boolean isJavaPluginAvailable() {
        try {
            // Try to load a Java-specific class to verify Java support is available
            Class.forName("com.intellij.psi.PsiJavaFile");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if Rust plugin/support is available in the current IDE.
     */
    private static boolean isRustPluginAvailable() {
        try {
            // Try to load Rust-specific classes to verify Rust support is available
            // The org.rust.lang plugin provides these classes
            Class.forName("org.rust.lang.core.psi.RsFile");
            return true;
        } catch (ClassNotFoundException e) {
            // Fallback: try alternative Rust plugin class names
            try {
                Class.forName("org.rust.lang.RsFileType");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }

}