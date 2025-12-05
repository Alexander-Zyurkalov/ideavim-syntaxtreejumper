package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * TidalMidi PSI-based implementation of SyntaxNode.
 */
public class TidalMidiSyntaxNode extends SyntaxNode {

    public TidalMidiSyntaxNode(PsiElement psiElement) {
        super(psiElement);
    }

    @Override
    @Nullable
    public SyntaxNode getParent() {
        PsiElement parent = psiElement.getParent();
        return parent != null ? new TidalMidiSyntaxNode(parent) : null;
    }

    @Override
    @NotNull
    public List<SyntaxNode> getChildren() {
        PsiElement[] children = psiElement.getChildren();
        if (children.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(children).map(TidalMidiSyntaxNode::new).map(node -> (SyntaxNode) node).toList();
    }

    @Override
    @Nullable
    public SyntaxNode getPreviousSibling() {
        PsiElement sibling = psiElement.getPrevSibling();
        return sibling != null ? new TidalMidiSyntaxNode(sibling) : null;
    }

    @Override
    @Nullable
    public SyntaxNode getNextSibling() {
        PsiElement sibling = psiElement.getNextSibling();
        return sibling != null ? new TidalMidiSyntaxNode(sibling) : null;
    }

    @Override
    public boolean isEquivalentTo(@Nullable SyntaxNode other) {
        if (!(other instanceof TidalMidiSyntaxNode tidalNode)) {
            return false;
        }
        return psiElement.isEquivalentTo(tidalNode.psiElement);
    }

    @Override
    public SyntaxNode getFirstChild() {
        PsiElement firstChild = psiElement.getFirstChild();
        if (firstChild == null) {
            return null;
        }
        return new TidalMidiSyntaxNode(firstChild);
    }

    @Override
    public SyntaxNode getLastChild() {
        PsiElement lastChild = psiElement.getLastChild();
        if (lastChild == null) {
            return null;
        }
        return new TidalMidiSyntaxNode(lastChild);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TidalMidiSyntaxNode that)) return false;
        return Objects.equals(psiElement, that.psiElement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(psiElement);
    }

    @Override
    public @NotNull String toString() {
        return "TidalMidiSyntaxNode{" + psiElement.getClass().getSimpleName() + ", text='" + getText() + "'}";
    }

    // TidalMidi-specific overrides

    @Override
    public boolean isMethodOrFunctionCallExpression() {
        // TidalMidi doesn't have traditional function calls, but operators might be considered similar
        return false;
    }



    @Override
    public boolean isFunctionArgument() {
        String typeName = getTypeName();
        SyntaxNode parent = getParent();

        if (parent == null) {
            return false;
        }

        String parentTypeName = parent.getTypeName();

        // Parameters in operators or bjorklund params
        if (parentTypeName.equals("BJORKLUND_PARAMS")) {
            return typeName.equals("PARAMETER") || typeName.equals("NUMBER");
        }

        return false;
    }

    @Override
    public boolean isExpressionStatement() {
        return getTypeName().equals("EXPRESSION");
    }

    @Override
    public boolean isDeclarationStatement() {
        return getTypeName().equals("TARGET_ASSIGN");
    }

    @Override
    public boolean isAStatement() {
        String typeName = getTypeName();
        return typeName.equals("SECTION") || typeName.equals("SECTION_ELEMENT");
    }

    @Override
    public boolean isReturnStatement() {
        // TidalMidi doesn't have return statements
        return false;
    }

    @Override
    public boolean isLoopStatement() {
        String typeName = getTypeName();
        // Repeats and certain operators could be considered loop-like
        return typeName.equals("REPEAT") || typeName.equals("OP_REPLICATE");
    }

    @Override
    public boolean isFunctionDefinition() {
        // TidalMidi doesn't have function definitions
        return false;
    }

    @Override
    public boolean isMethodDefinition() {
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

        // TXT tokens in target context can be considered variables
        if (typeName.equals("TXT") || typeName.equals("TARGET_TXT")) {
            return parentTypeName.equals("TARGET") || parentTypeName.equals("TARGET_ASSIGN");
        }

        return false;
    }

    @Override
    public boolean isBracket() {
        return super.isBracket();
    }

    @Override
    public boolean isExpressionList() {
        return false;
    }

    @Override
    public boolean isTypeParameter() {
        return false;
    }

    @Override
    public boolean isLoopOrConditionalStatement() {
        return false;
    }

    @Override
    public boolean isConditionalStatement() {
        return false;
    }

    @Override
    public boolean isFunctionParameter() {
        return false;
    }

    @Override
    public boolean isBlock() {
        String typeName = getTypeName();
        // Groups act as blocks in TidalMidi
        return typeName.equals("SUBDIVISION") || typeName.equals("ALTERNATING") || typeName.equals("POLYMETER") || typeName.equals("GROUP") || typeName.equals("SECTIONS");
    }

    @Override
    public boolean isEqualSymbol() {
        return getTypeName().equals("EQUALS") || getText().equals("=");
    }

    @Override
    public boolean isExpression() {
        String typeName = getTypeName();
        return typeName.equals("EXPRESSION") || typeName.equals("SECTION_ELEMENT") || typeName.endsWith("OPERATOR");
    }

    @Override
    public boolean isClassDefinition() {
        // TidalMidi doesn't have class definitions
        return false;
    }

    @Override
    public boolean isTemplate() {
        // Could consider polymeter or groups with parameters as templates
        return getTypeName().equals("POLYMETER");
    }

    @Override
    public boolean isComment() {
        // TidalMidi grammar doesn't define comments, but check for standard comment types
        String typeName = getTypeName();
        return typeName.contains("COMMENT");
    }

    @Override
    public boolean isMacro() {
        // TidalMidi doesn't have macros
        return false;
    }

    @Override
    public boolean isImport() {
        // TidalMidi doesn't have import statements
        return false;
    }

    @Override
    public boolean isTypeUsage() {
        // Mode could be considered a type usage in musical context
        return getTypeName().equals("MODE");
    }

    @Override
    public boolean isCompoundExpression() {
        String typeName = getTypeName();
        return typeName.equals("EXPRESSION") || typeName.equals("SECTIONS") || typeName.equals("CHORD") || typeName.equals("SECTION") || typeName.equals("BJORKLUND_PARAMS") || super.isCompoundExpression();
    }

    @Override
    public boolean isOperator() {
        String text = getText();

        // TidalMidi operator symbols
        return switch (text) {
            case "!", "@", "?", "*", "/", ":", "%", ",", "|", ".", "'" -> true;
            default -> super.isOperator();
        };

    }
}