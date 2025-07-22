package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Handles navigation between list elements (function arguments, array elements, etc.)
 * while skipping separators like commas, semicolons, and other punctuation.
 */
public class ListElementsMotionHandler implements MotionHandler {

    private final PsiFile psiFile;
    private final Direction direction;
    
    // Common separator tokens that should be skipped
    private static final Set<String> SEPARATORS = new HashSet<>(Arrays.asList(
        ",", ";", ":", "|", "&", "||", "&&", "->", "=>"
    ));
    
    // PSI element types that typically represent list-like structures
    private static final Set<String> LIST_CONTAINER_TYPES = new HashSet<>(Arrays.asList(
        "PsiParameterList", "PsiExpressionList", "PsiArrayInitializerExpression",
        "PsiArgumentList", "PsiTypeParameterList", "PsiReferenceParameterList",
        "JSArrayLiteralExpression", "JSObjectLiteralExpression", "JSParameterList",
        "PYArgumentList", "PYParameterList", "PYListLiteralExpression", "PYDictLiteralExpression"
    ));

    public ListElementsMotionHandler(PsiFile psiFile, Direction direction) {
        this.psiFile = psiFile;
        this.direction = direction;
    }

    @Override
    public Optional<Offsets> findNext(Offsets initialOffsets) {
        PsiElement initialElement = findElementAtPosition(initialOffsets);
        if (initialElement == null) {
            return Optional.of(initialOffsets);
        }

        // Find the containing list structure
        PsiElement listContainer = findContainingList(initialElement);
        if (listContainer == null) {
            return Optional.of(initialOffsets);
        }

        // Get all meaningful items in the list
        List<PsiElement> listItems = getListItems(listContainer);
        if (listItems.isEmpty()) {
            return Optional.of(initialOffsets);
        }

        // Find current item and navigate to next/previous
        PsiElement currentItem = findCurrentListItem(initialElement, listItems, initialOffsets);
        if (currentItem == null) {
            return Optional.of(initialOffsets);
        }

        PsiElement nextItem = findNextListItem(currentItem, listItems);
        if (nextItem == null) {
            return Optional.of(initialOffsets);
        }

        TextRange nextRange = nextItem.getTextRange();
        return Optional.of(new Offsets(nextRange.getStartOffset(), nextRange.getEndOffset()));
    }

    /**
     * Finds the PSI element at the current position, handling selections appropriately
     */
    private @Nullable PsiElement findElementAtPosition(Offsets offsets) {
        if (offsets.leftOffset() == offsets.rightOffset()) {
            // No selection, just cursor position
            return psiFile.findElementAt(offsets.leftOffset());
        } else {
            // We have a selection, find the element that best represents it
            PsiElement leftElement = psiFile.findElementAt(offsets.leftOffset());
            PsiElement rightElement = psiFile.findElementAt(offsets.rightOffset() - 1);
            
            if (leftElement == null) return rightElement;
            if (rightElement == null) return leftElement;
            
            // If selection spans multiple elements, find their common parent
            if (!leftElement.equals(rightElement)) {
                return PsiTreeUtil.findCommonParent(leftElement, rightElement);
            }
            
            return leftElement;
        }
    }

    /**
     * Finds the containing list structure (function parameters, array elements, etc.)
     */
    private @Nullable PsiElement findContainingList(@NotNull PsiElement element) {
        PsiElement current = element;
        
        while (current != null) {
            String elementType = current.getClass().getSimpleName();
            
            // Check if this element represents a list container
            if (LIST_CONTAINER_TYPES.contains(elementType)) {
                return current;
            }
            
            // Also check for generic parenthesized expressions or bracket expressions
            String text = current.getText();
            if (text != null && text.length() > 2) {
                char first = text.charAt(0);
                char last = text.charAt(text.length() - 1);
                if ((first == '(' && last == ')') || 
                    (first == '[' && last == ']') || 
                    (first == '{' && last == '}')) {
                    // This might be a list-like structure
                    List<PsiElement> children = getListItems(current);
                    if (children.size() > 1) {
                        return current;
                    }
                }
            }
            
            current = current.getParent();
        }
        
        return null;
    }

    /**
     * Extracts meaningful list items from a list container, filtering out separators and whitespace
     */
    private @NotNull List<PsiElement> getListItems(@NotNull PsiElement listContainer) {
        List<PsiElement> items = new ArrayList<>();
        
        for (PsiElement child : listContainer.getChildren()) {
            if (isMeaningfulListItem(child)) {
                items.add(child);
            }
        }
        
        return items;
    }

    /**
     * Determines if an element is a meaningful list item (not a separator or whitespace)
     */
    private boolean isMeaningfulListItem(@NotNull PsiElement element) {
        // Skip whitespace
        if (element instanceof PsiWhiteSpace) {
            return false;
        }
        
        // Skip empty elements
        String text = element.getText();
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // Skip separators
        if (SEPARATORS.contains(text.trim())) {
            return false;
        }
        
        // Skip single character punctuation that's likely a separator
        if (text.trim().length() == 1) {
            char c = text.trim().charAt(0);
            if (c == ',' || c == ';' || c == '|' || c == '&') {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Finds the current list item that contains or is closest to the initial position
     */
    private @Nullable PsiElement findCurrentListItem(@NotNull PsiElement initialElement, 
                                                     @NotNull List<PsiElement> listItems, 
                                                     @NotNull Offsets initialOffsets) {
        // First, try to find an item that contains the initial element
        for (PsiElement item : listItems) {
            if (PsiTreeUtil.isAncestor(item, initialElement, false) || item.equals(initialElement)) {
                return item;
            }
        }
        
        // If no item contains the initial element, find the closest one by position
        PsiElement closestItem = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (PsiElement item : listItems) {
            TextRange itemRange = item.getTextRange();
            int distance = Math.min(
                Math.abs(itemRange.getStartOffset() - initialOffsets.leftOffset()),
                Math.abs(itemRange.getEndOffset() - initialOffsets.rightOffset())
            );
            
            if (distance < minDistance) {
                minDistance = distance;
                closestItem = item;
            }
        }
        
        return closestItem;
    }

    /**
     * Finds the next list item in the specified direction
     */
    private @Nullable PsiElement findNextListItem(@NotNull PsiElement currentItem, 
                                                  @NotNull List<PsiElement> listItems) {
        int currentIndex = listItems.indexOf(currentItem);
        if (currentIndex == -1) {
            return null;
        }
        
        int nextIndex = switch (direction) {
            case FORWARD -> currentIndex + 1;
            case BACKWARD -> currentIndex - 1;
        };
        
        if (nextIndex >= 0 && nextIndex < listItems.size()) {
            return listItems.get(nextIndex);
        }
        
        return null;
    }
}