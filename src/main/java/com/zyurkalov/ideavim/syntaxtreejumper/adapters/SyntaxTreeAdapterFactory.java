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
        } else if (isLuaFile(language, fileType)) {
            return createLuaAdapter(psiFile);
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

        // Check for Rust language identifiers
        return languageId.contains("rust") ||
                fileTypeName.contains("rust");
    }

    /**
     * Determines if the file is a Lua file based on language and file type.
     */
    private static boolean isLuaFile(@NotNull Language language, @NotNull FileType fileType) {
        String languageId = language.getID().toLowerCase();
        String fileTypeName = fileType.getName().toLowerCase();

        // Check for Lua language identifiers
        return languageId.contains("lua") ||
                fileTypeName.contains("lua");
    }


    private static @NotNull SyntaxTreeAdapter createCppAdapter(@NotNull PsiFile psiFile) {
        try {
            return new CppSyntaxTreeAdapter(psiFile);
        } catch (Exception e) {
            return new PsiSyntaxTreeAdapter(psiFile);
        }
    }

    private static @NotNull SyntaxTreeAdapter createRustAdapter(@NotNull PsiFile psiFile) {
        try {
            return new RustSyntaxTreeAdapter(psiFile);
        } catch (Exception e) {
            return new PsiSyntaxTreeAdapter(psiFile);
        }
    }

    /**
     * Creates a Lua-specific adapter.
     * Falls back to the PSI adapter if a Lua adapter cannot be created.
     */
    private static @NotNull SyntaxTreeAdapter createLuaAdapter(@NotNull PsiFile psiFile) {
        try {
            return new LuaSyntaxTreeAdapter(psiFile);
        } catch (Exception e) {
            return new PsiSyntaxTreeAdapter(psiFile);
        }
    }


    // Utility methods for debugging. We only use them when adding new languages

    /**
     * Prints the structure of a file in a formatted table.
     * Shows TypeName, ParentTypeName, and Text for each PSI element.
     */
    private static void printFileStructure(@NotNull PsiFile psiFile) {
        System.out.println("\n" + "=".repeat(120));
        System.out.println("FILE STRUCTURE: " + psiFile.getName());
        System.out.println("=".repeat(120));
        System.out.printf("%-35s | %-35s | %s%n", "TypeName", "ParentTypeName", "Text");
        System.out.println("-".repeat(120));

        printElementStructure(psiFile, 0);

        System.out.println("=".repeat(120));
        System.out.println();
    }

    /**
     * Recursively prints the structure of a PSI element and its children.
     */
    private static void printElementStructure(@NotNull com.intellij.psi.PsiElement element, int depth) {
        String typeName = getTypeName(element);
        if (typeName.equals("WHITE_SPACE")) {
            return;
        }
        String parentTypeName = element.getParent() != null ? getTypeName(element.getParent()) : "ROOT";
        String text = truncateText(element.getText(), 40);

        // Add indentation to show the tree structure
        String indent = "  ".repeat(depth);
        String displayTypeName = indent + typeName;

        System.out.println(String.format("%-35s | %-35s | %s",
                truncateText(displayTypeName, 35),
                truncateText(parentTypeName, 35),
                text));

        // Process children
        for (com.intellij.psi.PsiElement child : element.getChildren()) {
            printElementStructure(child, depth + 1);
        }
    }

    /**
     * Gets the type name of a PSI element.
     */
    private static String getTypeName(@NotNull com.intellij.psi.PsiElement element) {
        if (element.getNode() != null) {
            return element.getNode().getElementType().toString();
        }
        return element.getClass().getSimpleName();
    }

    /**
     * Truncates text to the specified maximum length, adding "..." if truncated.
     */
    private static String truncateText(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        // Replace newlines and multiple spaces with single space
        text = text.replaceAll("\\s+", " ").trim();

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength - 3) + "...";
    }

}