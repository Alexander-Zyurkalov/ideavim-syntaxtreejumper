package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.psi.PsiElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.zyurkalov.ideavim.syntaxtreejumper.adapters.FakePsiElementTreeBuilder.leaf;
import static com.zyurkalov.ideavim.syntaxtreejumper.adapters.FakePsiElementTreeBuilder.list;

class FakePsiElementTreeBuilderTest {

    @Test
    void listFromString() {
        PsiElement[] result = FakePsiElementTreeBuilder.listFromString("i= 1 + 2;").getChildren();

        Assertions.assertArrayEquals(list("i", "=", " ", "1", " ", "+", " ", "2", ";").getChildren(), result);
    }


    @Test
    void flattenList() {
        PsiElement[] result = list(leaf("for"), leaf(" "), leaf("("), list("int", " ", "i", "=", "0")).getChildren();
        PsiElement[] expected = FakePsiElementTreeBuilder.listFromString("for (int i=0").getChildren();
    }

    @Test
    void testParentAndSiblingRelationships() {
        // Create a parent element with multiple children
        FakePsiElementTreeBuilder.MyFakePsiElement parent = list("first", "second", "third");

        PsiElement[] children = parent.getChildren();

        // Verify we have the expected number of children
        Assertions.assertEquals(3, children.length);

        FakePsiElementTreeBuilder.MyFakePsiElement first =
                (FakePsiElementTreeBuilder.MyFakePsiElement) children[0];
        FakePsiElementTreeBuilder.MyFakePsiElement second =
                (FakePsiElementTreeBuilder.MyFakePsiElement) children[1];
        FakePsiElementTreeBuilder.MyFakePsiElement third =
                (FakePsiElementTreeBuilder.MyFakePsiElement) children[2];

        // Test parent relationships
        Assertions.assertEquals(parent, first.getParent());
        Assertions.assertEquals(parent, second.getParent());
        Assertions.assertEquals(parent, third.getParent());

        // Test sibling relationships for first child
        Assertions.assertNull(first.getPrevSibling()); // first has no previous sibling
        Assertions.assertEquals(second, first.getNextSibling());

        // Test sibling relationships for middle child
        Assertions.assertEquals(first, second.getPrevSibling());
        Assertions.assertEquals(third, second.getNextSibling());

        // Test sibling relationships for last child
        Assertions.assertEquals(second, third.getPrevSibling());
        Assertions.assertNull(third.getNextSibling()); // last has no next sibling
    }

    @Test
    void testParentAndSiblingRelationshipsWithNonFlattenedSublists() {
        // Create sublists that won't be flattened (they don't have "list" as text)
        FakePsiElementTreeBuilder.MyFakePsiElement condition =
                FakePsiElementTreeBuilder.branch("i", "<", "10");
        FakePsiElementTreeBuilder.MyFakePsiElement body =
                FakePsiElementTreeBuilder.branch("print", "(", "i", ")");

        // Create parent with sublists that won't be flattened
        FakePsiElementTreeBuilder.MyFakePsiElement parent =
                FakePsiElementTreeBuilder.list(
                        leaf("while"),
                        leaf("("),
                        condition,  // This sublist won't be flattened (text is "condition")
                        leaf(")"),
                        leaf("{"),
                        body,       // This sublist won't be flattened (text is "body")
                        leaf("}")
                );

        PsiElement[] children = parent.getChildren();

        // Should have 7 children (no flattening occurred)
        Assertions.assertEquals(7, children.length);

        // Cast children for easier testing
        FakePsiElementTreeBuilder.MyFakePsiElement whileKeyword =
                (FakePsiElementTreeBuilder.MyFakePsiElement) children[0];
        FakePsiElementTreeBuilder.MyFakePsiElement openParen =
                (FakePsiElementTreeBuilder.MyFakePsiElement) children[1];
        FakePsiElementTreeBuilder.MyFakePsiElement conditionChild =
                (FakePsiElementTreeBuilder.MyFakePsiElement) children[2];
        FakePsiElementTreeBuilder.MyFakePsiElement closeParen =
                (FakePsiElementTreeBuilder.MyFakePsiElement) children[3];
        FakePsiElementTreeBuilder.MyFakePsiElement openBrace =
                (FakePsiElementTreeBuilder.MyFakePsiElement) children[4];
        FakePsiElementTreeBuilder.MyFakePsiElement bodyChild =
                (FakePsiElementTreeBuilder.MyFakePsiElement) children[5];
        FakePsiElementTreeBuilder.MyFakePsiElement closeBrace =
                (FakePsiElementTreeBuilder.MyFakePsiElement) children[6];


        // Test parent relationships - all direct children should have a parent as their parent
        Assertions.assertEquals(parent, whileKeyword.getParent());
        Assertions.assertEquals(parent, openParen.getParent());
        Assertions.assertEquals(parent, conditionChild.getParent());
        Assertions.assertEquals(parent, closeParen.getParent());
        Assertions.assertEquals(parent, openBrace.getParent());
        Assertions.assertEquals(parent, bodyChild.getParent());
        Assertions.assertEquals(parent, closeBrace.getParent());

        // Test sibling relationships for first child
        Assertions.assertNull(whileKeyword.getPrevSibling());
        Assertions.assertEquals(openParen, whileKeyword.getNextSibling());

        // Test sibling relationships for middle children
        Assertions.assertEquals(openParen, conditionChild.getPrevSibling());
        Assertions.assertEquals(closeParen, conditionChild.getNextSibling());

        Assertions.assertEquals(conditionChild, closeParen.getPrevSibling());
        Assertions.assertEquals(openBrace, closeParen.getNextSibling());

        Assertions.assertEquals(openBrace, bodyChild.getPrevSibling());
        Assertions.assertEquals(closeBrace, bodyChild.getNextSibling());

        // Test sibling relationships for last child
        Assertions.assertEquals(bodyChild, closeBrace.getPrevSibling());
        Assertions.assertNull(closeBrace.getNextSibling());

        // Test that sublists have their own internal structure
        PsiElement[] conditionChildren = conditionChild.getChildren();
        Assertions.assertEquals(3, conditionChildren.length);

        // Test parent relationships within the sublist
        for (PsiElement child : conditionChildren) {
            Assertions.assertEquals(conditionChild, child.getParent());
        }

        // Test sibling relationships within the condition sublist
        FakePsiElementTreeBuilder.MyFakePsiElement i =
                (FakePsiElementTreeBuilder.MyFakePsiElement) conditionChildren[0];
        FakePsiElementTreeBuilder.MyFakePsiElement lessThan =
                (FakePsiElementTreeBuilder.MyFakePsiElement) conditionChildren[1];
        FakePsiElementTreeBuilder.MyFakePsiElement ten =
                (FakePsiElementTreeBuilder.MyFakePsiElement) conditionChildren[2];

        Assertions.assertNull(i.getPrevSibling());
        Assertions.assertEquals(lessThan, i.getNextSibling());
        Assertions.assertEquals(i, lessThan.getPrevSibling());
        Assertions.assertEquals(ten, lessThan.getNextSibling());
        Assertions.assertEquals(lessThan, ten.getPrevSibling());
        Assertions.assertNull(ten.getNextSibling());
    }

    public static void main(String[] args) {
        System.out.println(FakePsiElementTreeBuilder.makeForLoop1To10());
    }
}