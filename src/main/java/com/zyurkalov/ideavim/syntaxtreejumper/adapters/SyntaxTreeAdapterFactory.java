
package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Factory class for creating appropriate SyntaxTreeAdapter instances based on file type and language.
 */
public class SyntaxTreeAdapterFactory {

    /**
     * Creates a SyntaxTreeAdapter for the given editor.
     *
     * @param editor The editor to create an adapter for
     * @return The appropriate adapter, or null if no suitable adapter can be created
     */
    @Nullable
    public static SyntaxTreeAdapter createAdapter(@NotNull Editor editor) {
        Project project = editor.getProject();
        if (project == null) {
            return null;
        }

        VirtualFile virtualFile = editor.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }

        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile == null) {
            return null;
        }

        return createAdapter(psiFile);
    }

    /**
     * Creates a SyntaxTreeAdapter for the given PSI file.
     *
     * @param psiFile The PSI file to create an adapter for
     * @return The appropriate adapter, or null if no suitable adapter can be created
     */
    public static @NotNull SyntaxTreeAdapter createAdapter(@NotNull PsiFile psiFile) {
        Language language = psiFile.getLanguage();
        FileType fileType = psiFile.getFileType();

        // Check for C++ files first (as they might need special handling)
        if (isCppFile(language, fileType)) {
            return createCppAdapter(psiFile);
        } else if (isRustFile(language, fileType)) {
            return createRustAdapter(psiFile);
        }

        // For all other languages, use the PSI adapter
        return new PsiSyntaxTreeAdapter(psiFile);
    }

    /**
     * Determines if the file is a C++ file based on language and file type.
     */
    private static boolean isCppFile(@NotNull Language language, @NotNull FileType fileType) {
        String languageId = language.getID().toLowerCase();
        String fileTypeName = fileType.getName().toLowerCase();

        // Check for C++ language identifiers
        return languageId.contains("cpp") ||
                languageId.contains("c++") ||
                languageId.contains("objective-c") ||
                fileTypeName.contains("cpp") ||
                fileTypeName.contains("c++") ||
                fileTypeName.contains("objectivec");
    }

    /**
     * Determines if the file is a Rust file based on language and file type.
     */
    private static boolean isRustFile(@NotNull Language language, @NotNull FileType fileType) {
        String languageId = language.getID().toLowerCase();
        String fileTypeName = fileType.getName().toLowerCase();

        // Check for C++ language identifiers
        return languageId.contains("rust") ||
                fileTypeName.contains("rust");
    }


    /**
     * Creates a C++-specific adapter.
     * Falls back to PSI adapter if a C++ adapter cannot be created.
     */
    private static @NotNull SyntaxTreeAdapter createCppAdapter(@NotNull PsiFile psiFile) {
        try {
            return new CppSyntaxTreeAdapter(psiFile);
        } catch (Exception e) {
            // If the C++ adapter fails to initialise, fall back to PSI adapter
            return new PsiSyntaxTreeAdapter(psiFile);
        }
    }

    /**
     * Creates a Rust-specific adapter.
     * Falls back to the PSI adapter if a Rust adapter cannot be created.
     */
    private static @NotNull SyntaxTreeAdapter createRustAdapter(@NotNull PsiFile psiFile) {
        try {
            return new RustSyntaxTreeAdapter(psiFile);
        } catch (Exception e) {
            // If the C++ adapter fails to initialise, fall back to PSI adapter
            return new PsiSyntaxTreeAdapter(psiFile);
        }
    }



}
