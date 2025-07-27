
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

import java.util.Objects;

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
    @Nullable
    public static SyntaxTreeAdapter createAdapter(@NotNull PsiFile psiFile) {
        Language language = psiFile.getLanguage();
        FileType fileType = psiFile.getFileType();

        // Check for C++ files first (as they might need special handling)
        if (isCppFile(language, fileType)) {
            return createCppAdapter(psiFile);
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
     * Creates a C++ specific adapter.
     * Falls back to PSI adapter if C++ adapter cannot be created.
     */
    @Nullable
    private static SyntaxTreeAdapter createCppAdapter(@NotNull PsiFile psiFile) {
        try {
            return new CppSyntaxTreeAdapter(psiFile);
        } catch (Exception e) {
            // If C++ adapter fails to initialize, fall back to PSI adapter
            return new PsiSyntaxTreeAdapter(psiFile);
        }
    }

    /**
     * Creates an adapter for a specific language by language ID.
     * This method can be extended to support additional language-specific adapters.
     * 
     * @param psiFile The PSI file
     * @param languageId The language identifier
     * @return The appropriate adapter, or null if not supported
     */
    @Nullable
    public static SyntaxTreeAdapter createAdapterForLanguage(@NotNull PsiFile psiFile, @NotNull String languageId) {
        return switch (languageId.toLowerCase()) {
            case "cpp", "c++", "objective-c", "objectivec" -> createCppAdapter(psiFile);
            default -> new PsiSyntaxTreeAdapter(psiFile);
        };
    }

    /**
     * Checks if an adapter can be created for the given file.
     * 
     * @param psiFile The PSI file to check
     * @return true if an adapter can be created, false otherwise
     */
    public static boolean canCreateAdapter(@NotNull PsiFile psiFile) {
        try {
            return createAdapter(psiFile) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the preferred adapter class for a given language.
     * Useful for testing or when you need to know which adapter would be used.
     * 
     * @param language The language to check
     * @return The adapter class that would be used
     */
    @NotNull
    public static Class<? extends SyntaxTreeAdapter> getAdapterClass(@NotNull Language language) {
        if (isCppFile(language, Objects.requireNonNull(language.getAssociatedFileType()))) {
            return CppSyntaxTreeAdapter.class;
        }
        return PsiSyntaxTreeAdapter.class;
    }
}
