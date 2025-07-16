package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles numbered jumps to PSI elements at the same level.
 * Similar to Vim's f/t motions but for syntax tree elements.
 * 
 * Usage:
 * - Alt-j followed by 1-9: Jump to the nth sibling element
 * - Alt-j followed by 0: Jump to parent element (like Alt-o)
 */
public class NumberedElementJumpHandler implements MotionHandler {
    
    private final PsiFile psiFile;
    private final int targetNumber; // 0-9, where 0 means parent
    
    public NumberedElementJumpHandler(PsiFile psiFile, int targetNumber) {
        this.psiFile = psiFile;
        this.targetNumber = targetNumber;
    }
    
    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {
        if (targetNumber < 0 || targetNumber > 9) {
            return Optional.of(initialOffsets);
        }
        
        // Handle parent jump (0)
        if (targetNumber == 0) {
            return jumpToParent(initialOffsets);
        }
        
        // Handle sibling jumps (1-9)
        return jumpToNthSibling(initialOffsets, targetNumber);
    }
    
    /**
     * Jumps to the parent element (similar to Alt-o expand selection)
     */
    private Optional<Offsets> jumpToParent(Offsets initialOffsets) {
        PsiElement leftElement = psiFile.findElementAt(initialOffsets.leftOffset());
        PsiElement rightElement = psiFile.findElementAt(Math.max(0, initialOffsets.rightOffset() - 1));
        
        if (leftElement == null) {
            return Optional.of(initialOffsets);
        }
        
        PsiElement targetElement;
        if (initialOffsets.leftOffset() == initialOffsets.rightOffset()) {
            // No selection - find parent of current element
            targetElement = findMeaningfulParent(leftElement);
        } else {
            // Has selection - find parent that encompasses current selection
            targetElement = findSmallestCommonParent(leftElement, rightElement, initialOffsets);
        }
        
        if (targetElement == null) {
            return Optional.of(initialOffsets);
        }
        
        TextRange parentRange = targetElement.getTextRange();
        return Optional.of(new Offsets(parentRange.getStartOffset(), parentRange.getEndOffset()));
    }
    
    /**
     * Jumps to the nth sibling element at the same level
     */
    private Optional<Offsets> jumpToNthSibling(Offsets initialOffsets, int n) {
        PsiElement currentElement = findCurrentElement(initialOffsets);
        if (currentElement == null) {
            return Optional.of(initialOffsets);
        }
        
        // Find the parent to get siblings from
        PsiElement parent = currentElement.getParent();
        if (parent == null) {
            return Optional.of(initialOffsets);
        }
        
        // Get all meaningful siblings
        List<PsiElement> siblings = getMeaningfulSiblings(parent);
        if (siblings.isEmpty() || n > siblings.size()) {
            return Optional.of(initialOffsets);
        }
        
        // Jump to the nth sibling (1-indexed)
        PsiElement targetSibling = siblings.get(n - 1);
        TextRange targetRange = targetSibling.getTextRange();
        
        return Optional.of(new Offsets(targetRange.getStartOffset(), targetRange.getEndOffset()));
    }
    
    /**
     * Finds the current element based on the selection/cursor position
     */
    private PsiElement findCurrentElement(Offsets offsets) {
        PsiElement leftElement = psiFile.findElementAt(offsets.leftOffset());
        
        if (leftElement == null) {
            return null;
        }
        
        // If we have a selection, try to find the element that encompasses it
        if (offsets.leftOffset() != offsets.rightOffset()) {
            PsiElement rightElement = psiFile.findElementAt(offsets.rightOffset() - 1);
            if (rightElement != null && !leftElement.equals(rightElement)) {
                PsiElement commonParent = PsiTreeUtil.findCommonParent(leftElement, rightElement);
                if (commonParent != null) {
                    // Find the smallest parent that fully encompasses the selection
                    while (commonParent != null) {
                        TextRange range = commonParent.getTextRange();
                        if (range.getStartOffset() <= offsets.leftOffset() && 
                            range.getEndOffset() >= offsets.rightOffset()) {
                            return commonParent;
                        }
                        commonParent = commonParent.getParent();
                    }
                }
            }
        }
        
        // Find a meaningful element (not whitespace, not too small)
        return findMeaningfulElement(leftElement);
    }
    
    /**
     * Finds a meaningful element by walking up the tree if needed
     */
    private PsiElement findMeaningfulElement(PsiElement element) {
        while (element != null) {
            if (!(element instanceof PsiWhiteSpace) && 
                !element.getText().trim().isEmpty() &&
                element.getChildren().length > 0) {
                return element;
            }
            element = element.getParent();
        }
        return element;
    }
    
    /**
     * Finds a meaningful parent element
     */
    private PsiElement findMeaningfulParent(PsiElement element) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (!(parent instanceof PsiWhiteSpace) && 
                !parent.getText().trim().isEmpty()) {
                // Check if parent is actually larger than current element
                TextRange parentRange = parent.getTextRange();
                TextRange elementRange = element.getTextRange();
                if (parentRange.getStartOffset() < elementRange.getStartOffset() ||
                    parentRange.getEndOffset() > elementRange.getEndOffset()) {
                    return parent;
                }
            }
            parent = parent.getParent();
        }
        return parent;
    }
    
    /**
     * Gets all meaningful sibling elements (excluding whitespace and empty elements)
     */
    private List<PsiElement> getMeaningfulSiblings(PsiElement parent) {
        List<PsiElement> siblings = new ArrayList<>();
        
        for (PsiElement child : parent.getChildren()) {
            if (!(child instanceof PsiWhiteSpace) && 
                !child.getText().trim().isEmpty()) {
                siblings.add(child);
            }
        }
        
        return siblings;
    }
    
    /**
     * Finds the smallest common parent that encompasses the given selection
     */
    private PsiElement findSmallestCommonParent(PsiElement leftElement, PsiElement rightElement, Offsets selection) {
        if (rightElement == null) {
            rightElement = leftElement;
        }
        
        PsiElement commonParent = PsiTreeUtil.findCommonParent(leftElement, rightElement);
        
        // Walk up the tree until we find an element that fully encompasses our selection
        while (commonParent != null) {
            TextRange range = commonParent.getTextRange();
            if (range.getStartOffset() <= selection.leftOffset() &&
                range.getEndOffset() >= selection.rightOffset()) {
                
                // Check if this parent is actually larger than our current selection
                if (range.getStartOffset() < selection.leftOffset() ||
                    range.getEndOffset() > selection.rightOffset()) {
                    return commonParent;
                }
            }
            commonParent = commonParent.getParent();
        }
        
        return commonParent;
    }
}