package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Lua PSI-based implementation of SyntaxNode.
 */
public class LuaSyntaxNode extends SyntaxNode {

    public LuaSyntaxNode(PsiElement psiElement) {
        super(psiElement);
    }

    @Override
    @Nullable
    public SyntaxNode getParent() {
        PsiElement parent = psiElement.getParent();
        return parent != null ? new LuaSyntaxNode(parent) : null;
    }

    @Override
    @NotNull
    public List<SyntaxNode> getChildren() {
        PsiElement[] children = psiElement.getChildren();
        if (children.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(children)
                .map(LuaSyntaxNode::new)
                .map(node -> (SyntaxNode) node)
                .toList();
    }

    @Override
    @Nullable
    public SyntaxNode getPreviousSibling() {
        PsiElement sibling = psiElement.getPrevSibling();
        return sibling != null ? new LuaSyntaxNode(sibling) : null;
    }

    @Override
    @Nullable
    public SyntaxNode getNextSibling() {
        PsiElement sibling = psiElement.getNextSibling();
        return sibling != null ? new LuaSyntaxNode(sibling) : null;
    }

    @Override
    public boolean isEquivalentTo(@Nullable SyntaxNode other) {
        if (!(other instanceof LuaSyntaxNode luaNode)) {
            return false;
        }
        return psiElement.isEquivalentTo(luaNode.psiElement);
    }

    @Override
    public SyntaxNode getFirstChild() {
        PsiElement firstChild = psiElement.getFirstChild();
        if (firstChild == null) {
            return null;
        }
        return new LuaSyntaxNode(firstChild);
    }

    @Override
    public SyntaxNode getLastChild() {
        PsiElement lastChild = psiElement.getLastChild();
        if (lastChild == null) {
            return null;
        }
        return new LuaSyntaxNode(lastChild);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LuaSyntaxNode that)) return false;
        return Objects.equals(psiElement, that.psiElement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(psiElement);
    }

    @Override
    public @NotNull String toString() {
        return "LuaSyntaxNode{" + psiElement.getClass().getSimpleName() +
                ", text='" + getText() + "'}";
    }

    @Override
    public boolean isMethodOrFunctionCallExpression() {
        String typeName = getTypeName();
        return typeName.equals("CALL_EXPR");
    }

    @Override
    public boolean isFunctionArgument() {
        SyntaxNode parent = getParent();
        if (parent == null) {
            return false;
        }
        String parentTypeName = parent.getTypeName();
        String typeName = getTypeName();
        
        // Arguments are inside LIST_ARGS
        return parentTypeName.equals("LIST_ARGS") && 
               (typeName.endsWith("_EXPR") || typeName.equals("NAME_EXPR"));
    }

    @Override
    public boolean isExpressionStatement() {
        // In Lua, CALL_STAT is a statement that's just an expression
        return getTypeName().equals("CALL_STAT");
    }

    @Override
    public boolean isDeclarationStatement() {
        String typeName = getTypeName();
        return typeName.equals("LOCAL_DEF") || typeName.equals("ASSIGN_STAT");
    }

    @Override
    public boolean isAStatement() {
        String typeName = getTypeName();
        return typeName.endsWith("_STAT");
    }

    @Override
    public boolean isReturnStatement() {
        return getTypeName().equals("RETURN_STAT");
    }

    @Override
    public boolean isLoopStatement() {
        String typeName = getTypeName();
        return typeName.equals("WHILE_STAT") ||
               typeName.equals("FOR_A_STAT") ||   // Numeric for loop
               typeName.equals("FOR_B_STAT") ||   // Generic for loop
               typeName.equals("REPEAT_STAT") ||
               typeName.equals("IF_STAT");        // Including if as a control structure
    }

    @Override
    public boolean isFunctionDefinition() {
        String typeName = getTypeName();
        // "Global Function", "Class Method", and LOCAL_FUNC_DEF
        return typeName.equals("Global Function") ||
               typeName.equals("Class Method") ||
               typeName.equals("LOCAL_FUNC_DEF");
    }

    @Override
    public boolean isMethodDefinition() {
        // In Lua, methods are Class Methods with : syntax
        String typeName = getTypeName();
        if (!typeName.equals("Class Method")) {
            return false;
        }
        
        // Check if it has : syntax by looking for CLASS_METHOD_NAME child
        for (SyntaxNode child : getChildren()) {
            if (child.getTypeName().equals("CLASS_METHOD_NAME")) {
                // Check if the text contains ':'
                return child.getText().contains(":");
            }
        }
        return false;
    }

    @Override
    public boolean isVariable() {
        String typeName = getTypeName();
        SyntaxNode parent = getParent();
        
        if (parent == null) {
            return false;
        }
        
        String parentTypeName = parent.getTypeName();
        
        // NAME_EXPR can be a variable reference
        // NAME_DEF is a variable definition
        boolean isNameType = typeName.equals("NAME_EXPR") || typeName.equals("NAME_DEF");
        
        // Check if parent context makes this a variable
        boolean isInVariableContext = 
            parentTypeName.equals("NAME_LIST") ||      // In local/assignment declarations
            parentTypeName.equals("VAR_LIST") ||       // In assignment left side
            parentTypeName.equals("INDEX_EXPR") ||     // Base of field access
            parentTypeName.equals("BINARY_EXPR") ||    // In expressions
            parentTypeName.equals("UNARY_EXPR") ||     // In unary expressions
            parentTypeName.equals("LIST_ARGS") ||      // As function argument
            parentTypeName.equals("EXPR_LIST");        // In expression lists
            
        return isNameType && isInVariableContext;
    }

    @Override
    public boolean isBlock() {
        String typeName = getTypeName();
        return typeName.equals("LuaBlock");
    }

    @Override
    public boolean isEqualSymbol() {
        return getText().equals("=");
    }

    @Override
    public boolean isExpression() {
        String typeName = getTypeName();
        return typeName.endsWith("_EXPR");
    }

    @Override
    public boolean isClassDefinition() {
        // In Lua, "classes" are typically tables with metatables
        // This is tricky to detect, but we can look for:
        // 1. Table assignment to a local/global
        // 2. Followed by __index assignment
        // 3. Or metatable operations
        
        String typeName = getTypeName();
        if (!typeName.equals("LOCAL_DEF") && !typeName.equals("ASSIGN_STAT")) {
            return false;
        }
        
        // Check if next sibling has __index or setmetatable
        SyntaxNode nextSibling = getNextSibling();
        if (nextSibling != null) {
            String nextText = nextSibling.getText();
            if (nextText.contains(".__index") || nextText.contains("setmetatable")) {
                return true;
            }
        }
        
        // Check if the value is an empty table (common pattern)
        String text = getText();
        return text.matches(".*=\\s*\\{\\s*\\}.*");
    }

    @Override
    public boolean isTemplate() {
        // Lua doesn't have templates, but we could consider generic functions
        // For now, return false
        return false;
    }

    @Override
    public boolean isComment() {
        String typeName = getTypeName();
        return typeName.equals("BLOCK_COMMENT") ||
               typeName.equals("SHORT_COMMENT") ||
               typeName.equals("DOC_COMMENT");
    }

    @Override
    public boolean isMacro() {
        // Lua doesn't have macros in the C/Rust sense
        // But we could consider require() calls as imports
        return false;
    }

    @Override
    public boolean isImport() {
        // In Lua, imports are require() calls
        if (!isMethodOrFunctionCallExpression()) {
            return false;
        }
        
        // Check if the call is to 'require'
        for (SyntaxNode child : getChildren()) {
            if (child.getTypeName().equals("NAME_EXPR") && 
                child.getText().equals("require")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isTypeUsage() {
        // In Lua, type usage appears in doc comments
        String typeName = getTypeName();
        SyntaxNode parent = getParent();
        
        if (parent == null) {
            return false;
        }
        
        String parentTypeName = parent.getTypeName();
        
        // Check if this is a type reference in doc comments
        return (typeName.equals("GENERAL_TY") || typeName.equals("CLASS_NAME_REF")) &&
               (parentTypeName.equals("TAG_PARAM") || 
                parentTypeName.equals("TAG_RETURN") ||
                parentTypeName.equals("TYPE_LIST"));
    }
}