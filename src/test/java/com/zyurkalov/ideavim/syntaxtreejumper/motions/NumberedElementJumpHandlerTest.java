package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumberedElementJumpHandlerTest {
    
    private CodeInsightTestFixture myFixture;
    
    @BeforeEach
    public void setUp() throws Exception {
        IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
        TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = 
                factory.createLightFixtureBuilder(getClass().getName());
        IdeaProjectTestFixture fixture = fixtureBuilder.getFixture();
        myFixture = factory.createCodeInsightFixture(fixture);
        myFixture.setUp();
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        myFixture.tearDown();
    }
    
    record NumberedJumpTestData(
            Offsets initialOffsets,
            String initialText,
            int targetNumber,
            Offsets expectedOffsets,
            String expectedText,
            String explanation
    ) {
        @Override
        public @NotNull String toString() {
            return explanation;
        }
    }
    
    static Stream<NumberedJumpTestData> parentJumpTestCases() {
        return Stream.of(
                // Jump to parent (number 0) from cursor position
                new NumberedJumpTestData(
                        new Offsets(94, 94), // cursor at 'i' in "int i = 0;"
                        "",
                        0,
                        new Offsets(90, 100), // "int i = 0;"
                        "int i = 0;",
                        "Parent jump: cursor to declaration statement"
                ),
                
                // Jump to parent from identifier selection
                new NumberedJumpTestData(
                        new Offsets(94, 95), // 'i' selected
                        "i",
                        0,
                        new Offsets(90, 100), // "int i = 0;"
                        "int i = 0;",
                        "Parent jump: identifier to declaration statement"
                ),
                
                // Jump to parent from statement to for loop
                new NumberedJumpTestData(
                        new Offsets(90, 100), // "int i = 0;"
                        "int i = 0;",
                        0,
                        new Offsets(85, 151), // full for loop
                        "for (int i = 0; i < 10; i++) { a[i] = 2 * i; }",
                        "Parent jump: statement to for loop"
                ),
                
                // Jump to parent from assignment within for loop
                new NumberedJumpTestData(
                        new Offsets(128, 141), // "a[i] = 2 * i;"
                        "a[i] = 2 * i;",
                        0,
                        new Offsets(120, 151), // for loop body
                        "{ a[i] = 2 * i; }",
                        "Parent jump: assignment to block statement"
                ),
                
                // Jump to parent from method call
                new NumberedJumpTestData(
                        new Offsets(160, 185), // System.out.println("Test");
                        "System.out.println(\"Test\");",
                        0,
                        new Offsets(42, 195), // method body
                        "{ int[] a = new int[10]; for (int i = 0; i < 10; i++) { a[i] = 2 * i; } System.out.println(\"Test\"); }",
                        "Parent jump: method call to method body"
                )
        );
    }
    
    static Stream<NumberedJumpTestData> siblingJumpTestCases() {
        return Stream.of(

                new NumberedJumpTestData(
                        new Offsets(90, 93), //`int` in `int i = 0;`"
                        "int",
                        2,
                        new Offsets(94, 95),
                        "i", // ``i` from ``"int i = 0;"
                        "Sibling jump: next simplest element"
                ),
                 new NumberedJumpTestData(
                        new Offsets(90, 90), //`int` in `int i = 0;`"
                        "",
                        2,
                        new Offsets(94, 95),
                        "i", // ``i` from ``"int i = 0;"
                        "Sibling jump: next simplest element with no initial selection"
                ),
                new NumberedJumpTestData(
                        new Offsets(94, 95), //`i` in `int i = 0;`"
                        "i",
                        1,
                        new Offsets(90, 93),
                        "int", // `int` from ``"int i = 0;"
                        "Sibling jump: prev simplest element"
                ),
                new NumberedJumpTestData(
                        new Offsets(94, 95), //`i` in `int i = 0;`"
                        "i",
                        0,
                        new Offsets(90, 100),
                        "int i = 0;", // `int` from ``"int i = 0;"
                        "Sibling jump: jump to the parent"
                )

//                // Jump to first sibling in for loop initialization
//                new NumberedJumpTestData(
//                        new Offsets(90, 100), // "int i = 0;" (currently at initialization)
//                        "int i = 0;",
//                        1,
//                        new Offsets(90, 100), // same element (first sibling)
//                        "int i = 0;",
//                        "Sibling jump: already at first sibling in for loop"
//                ),
//
//                // Jump to second sibling in for loop (condition)
//                new NumberedJumpTestData(
//                        new Offsets(90, 100), // "int i = 0;"
//                        "int i = 0;",
//                        2,
//                        new Offsets(102, 108), // "i < 10"
//                        "i < 10",
//                        "Sibling jump: initialization to condition"
//                ),
//
//                // Jump to third sibling in for loop (increment)
//                new NumberedJumpTestData(
//                        new Offsets(90, 100), // "int i = 0;"
//                        "int i = 0;",
//                        3,
//                        new Offsets(110, 113), // "i++"
//                        "i++",
//                        "Sibling jump: initialization to increment"
//                ),
//
//                // Jump to fourth sibling in for loop (body)
//                new NumberedJumpTestData(
//                        new Offsets(90, 100), // "int i = 0;"
//                        "int i = 0;",
//                        4,
//                        new Offsets(120, 151), // "{ a[i] = 2 * i; }"
//                        "{ a[i] = 2 * i; }",
//                        "Sibling jump: initialization to body"
//                ),
//
//                // Jump from method declaration elements
//                new NumberedJumpTestData(
//                        new Offsets(18, 25), // "execute" method name
//                        "execute",
//                        1,
//                        new Offsets(13, 17), // "void" return type
//                        "void",
//                        "Sibling jump: method name to return type"
//                ),
//
//                // Jump to method body from method name
//                new NumberedJumpTestData(
//                        new Offsets(18, 25), // "execute" method name
//                        "execute",
//                        3,
//                        new Offsets(42, 195), // method body
//                        "{ int[] a = new int[10]; for (int i = 0; i < 10; i++) { a[i] = 2 * i; } System.out.println(\"Test\"); }",
//                        "Sibling jump: method name to body"
//                ),
//
//                // Jump within assignment expression
//                new NumberedJumpTestData(
//                        new Offsets(128, 131), // "a[i]" (left side of assignment)
//                        "a[i]",
//                        1,
//                        new Offsets(128, 131), // same element (first sibling)
//                        "a[i]",
//                        "Sibling jump: already at first sibling in assignment"
//                ),
//
//                // Jump to right side of assignment
//                new NumberedJumpTestData(
//                        new Offsets(128, 131), // "a[i]"
//                        "a[i]",
//                        2,
//                        new Offsets(134, 139), // "2 * i"
//                        "2 * i",
//                        "Sibling jump: left side to right side of assignment"
//                )
        );
    }
    
    static Stream<NumberedJumpTestData> edgeCaseTestCases() {
        return Stream.of(
                // Invalid number (out of range)
                new NumberedJumpTestData(
                        new Offsets(94, 95), // 'i'
                        "i",
                        10, // invalid number
                        new Offsets(94, 95), // should stay at same position
                        "i",
                        "Edge case: invalid number should return original position"
                ),
                
                // Negative number
                new NumberedJumpTestData(
                        new Offsets(94, 95), // 'i'
                        "i",
                        -1, // invalid number
                        new Offsets(94, 95), // should stay at same position
                        "i",
                        "Edge case: negative number should return original position"
                ),
                
                // Jump to sibling that doesn't exist (number too high)
                new NumberedJumpTestData(
                        new Offsets(128, 131), // "a[i]" in assignment (only has 2 siblings)
                        "a[i]",
                        5, // number higher than available siblings
                        new Offsets(128, 131), // should stay at same position
                        "a[i]",
                        "Edge case: sibling number too high should return original position"
                ),
                
                // Jump at file level (should find class as first sibling)
                new NumberedJumpTestData(
                        new Offsets(0, 0), // beginning of file
                        "",
                        1,
                        new Offsets(0, 195), // entire class
                        "public class TestClass { void execute() { int[] a = new int[10]; for (int i = 0; i < 10; i++) { a[i] = 2 * i; } System.out.println(\"Test\"); } }",
                        "Edge case: jump to first sibling at file level"
                ),
                
                // Jump to parent from top-level element (should stay at same position)
                new NumberedJumpTestData(
                        new Offsets(0, 195), // entire class
                        "public class TestClass { void execute() { int[] a = new int[10]; for (int i = 0; i < 10; i++) { a[i] = 2 * i; } System.out.println(\"Test\"); } }",
                        0, // parent jump
                        new Offsets(0, 195), // should stay at same position (no parent available)
                        "public class TestClass { void execute() { int[] a = new int[10]; for (int i = 0; i < 10; i++) { a[i] = 2 * i; } System.out.println(\"Test\"); } }",
                        "Edge case: parent jump from top-level should return file level"
                )
        );
    }
    
    static Stream<NumberedJumpTestData> complexStructureTestCases() {
        return Stream.of(
                // Jump within nested structures
                new NumberedJumpTestData(
                        new Offsets(160, 166), // "System" in System.out.println
                        "System",
                        1,
                        new Offsets(160, 166), // first part of qualified expression
                        "System",
                        "Complex: first part of qualified expression"
                ),
                
                new NumberedJumpTestData(
                        new Offsets(160, 166), // "System"
                        "System",
                        2,
                        new Offsets(167, 170), // "out"
                        "out",
                        "Complex: second part of qualified expression"
                ),
                
                new NumberedJumpTestData(
                        new Offsets(160, 166), // "System"
                        "System",
                        3,
                        new Offsets(171, 178), // "println"
                        "println",
                        "Complex: method name in qualified expression"
                ),
                
                // Jump within array access
                new NumberedJumpTestData(
                        new Offsets(128, 129), // "a" in a[i]
                        "a",
                        1,
                        new Offsets(128, 129), // array reference
                        "a",
                        "Complex: array reference in array access"
                ),
                
                new NumberedJumpTestData(
                        new Offsets(128, 129), // "a"
                        "a",
                        2,
                        new Offsets(130, 131), // "i" (array index)
                        "i",
                        "Complex: array index in array access"
                )
        );
    }
    
    @ParameterizedTest
    @MethodSource("parentJumpTestCases")
    void testParentJumps(NumberedJumpTestData testData) {
        runNumberedJumpTest(testData);
    }
    
    @ParameterizedTest
    @MethodSource("siblingJumpTestCases")
    void testSiblingJumps(NumberedJumpTestData testData) {
        runNumberedJumpTest(testData);
    }
    
    @ParameterizedTest
    @MethodSource("edgeCaseTestCases")
    void testEdgeCases(NumberedJumpTestData testData) {
        runNumberedJumpTest(testData);
    }
    
    @ParameterizedTest
    @MethodSource("complexStructureTestCases")
    void testComplexStructures(NumberedJumpTestData testData) {
        runNumberedJumpTest(testData);
    }
    
    private void runNumberedJumpTest(NumberedJumpTestData testData) {
        String javaCode = """
                public class TestClass {
                    void execute() {
                        int[] a = new int[10];
                        for (int i = 0; i < 10; i++) {
                            a[i] = 2 * i;
                        }
                        System.out.println("Test");
                    }
                }
                """;
        
        // Verify test prerequisite - initial selection matches expected text if provided
        if (!testData.initialText.isEmpty()) {
            String actualInitialText = javaCode.substring(
                    testData.initialOffsets.leftOffset(),
                    testData.initialOffsets.rightOffset()
            );
            assertTrue(
                    testData.initialText.equals(actualInitialText) ||
                            actualInitialText.contains(testData.initialText) ||
                            testData.initialText.contains(actualInitialText),
                    "Test prerequisite failed: expected initial text '" + testData.initialText +
                            "' but got '" + actualInitialText + "'"
            );
        }
        
        PsiFile javaFile = myFixture.configureByText("TestClass.java", javaCode);
        NumberedElementJumpHandler handler = new NumberedElementJumpHandler(javaFile, testData.targetNumber);
        
        ApplicationManager.getApplication().runReadAction(() -> {
            Optional<Offsets> result = handler.findNext(testData.initialOffsets);
            assertTrue(result.isPresent(), "Handler should return a result");
            
            Offsets actualOffsets = result.get();
            assertEquals(testData.expectedOffsets, actualOffsets, testData.explanation);
            
            // Verify the selected text matches expectations
            if (!testData.expectedText.isEmpty()) {
                String actualSelectedText = javaCode.substring(
                        actualOffsets.leftOffset(),
                        actualOffsets.rightOffset()
                );
                
                String normalizedActual = actualSelectedText.trim().replaceAll("\\s+", " ");
                String normalizedExpected = testData.expectedText.trim().replaceAll("\\s+", " ");
                
                assertEquals(normalizedExpected, normalizedActual, 
                        "Selected text mismatch for: " + testData.explanation);
            }
        });
    }
}