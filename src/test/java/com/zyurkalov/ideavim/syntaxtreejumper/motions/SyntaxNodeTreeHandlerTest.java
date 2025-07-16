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

class SyntaxNodeTreeHandlerTest {

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

    record HelixSelectionTestData(
            Offsets initialOffsets,
            String initialText,
            Offsets expectedOffsets,
            String expectedText,
            SyntaxNodeTreeHandler.SyntaxNoteMotionType motionType,
            String explanation
    ) {
        @Override
        public @NotNull String toString() {
            return explanation;
        }
    }

    static Stream<HelixSelectionTestData> expandSelectionTestCases() {
        return Stream.of(
                // Basic expansion from the cursor position
                new HelixSelectionTestData(
                        new Offsets(94, 94), // cursor at 'i' in "int i = 0;"
                        "",
                        new Offsets(94, 95), // select 'i'
                        "i",
                        SyntaxNodeTreeHandler.SyntaxNoteMotionType.EXPAND,
                        "Expand: cursor position to identifier"
                ),
                new HelixSelectionTestData(
                        new Offsets(13, 13), // cursor at 'i' in "int i = 0;"
                        "",
                        new Offsets(13, 17), // select 'i'
                        "Test",
                        SyntaxNodeTreeHandler.SyntaxNoteMotionType.EXPAND,
                        "Expand: cursor position to SubWord"
                ),

                // Expand from a single character to identifier
                new HelixSelectionTestData(
                        new Offsets(94, 95), // 'i' selected
                        "i",
                        new Offsets(90, 100), // "int i = 0;"
                        "int i = 0;",
                        SyntaxNodeTreeHandler.SyntaxNoteMotionType.EXPAND,
                        "Expand: identifier to assignment expression"
                ),

                // Expand from statement to block
                new HelixSelectionTestData(
                        new Offsets(90, 100), // "int i = 0;"
                        "int i = 0;",
                        new Offsets(85, 151), // full for loop
                        "for (int i = 0; i < 10; i++) { a[i] = 2 * i; }",
                        SyntaxNodeTreeHandler.SyntaxNoteMotionType.EXPAND,
                        "Expand: for loop initialization to entire for loop"
                )
        );
    }

    static Stream<HelixSelectionTestData> shrinkSelectionTestCases() {
        return Stream.of(

                new HelixSelectionTestData(
                        new Offsets(13, 22), // cursor at 'i' in "int i = 0;"
                        "TestClass",
                        new Offsets(17, 22), // select 'i'
                        "Class",
                        SyntaxNodeTreeHandler.SyntaxNoteMotionType.SHRINK,
                        "Shrink: an element to SubWord"
                ),                // Shrink from method call to method name
                new HelixSelectionTestData(
                        new Offsets(160, 185), // full method call
                        "System.out.println(\"Test\")",
                        new Offsets(160, 178), // method name
                        "System.out.println",
                        SyntaxNodeTreeHandler.SyntaxNoteMotionType.SHRINK,
                        "Shrink: method call to method name"
                ),
                // Shrink from System.out.println to System.out
                new HelixSelectionTestData(
                        new Offsets(160, 178), // fully qualified method name
                        "System.out.println",
                        new Offsets(160, 170), // qualifier part
                        "System.out",
                        SyntaxNodeTreeHandler.SyntaxNoteMotionType.SHRINK,
                        "Shrink: qualified method name to qualifier"
                ),

                // Shrink from a full statement to the main expression
                new HelixSelectionTestData(
                        new Offsets(128, 141), // "a[i] = 2 * i;"
                        "a[i] = 2 * i;",
                        new Offsets(128, 140), // "a[i] = 2 * i"
                        "a[i] = 2 * i",
                        SyntaxNodeTreeHandler.SyntaxNoteMotionType.SHRINK,
                        "Shrink: statement to assignment expression"
                )
        );
    }

    static Stream<HelixSelectionTestData> edgeCaseTestCases() {
        return Stream.of(
                // No selection for shrink should return the same offsets
                new HelixSelectionTestData(
                        new Offsets(124, 124), // cursor position
                        "",
                        new Offsets(124, 124), // same position
                        "",
                        SyntaxNodeTreeHandler.SyntaxNoteMotionType.SHRINK,
                        "Edge case: shrink with no selection should return same position"
                ),

                // Expand at the end of a file
                new HelixSelectionTestData(
                        new Offsets(194, 195), // "}"
                        "}",
                        new Offsets(0, 195), // whole class body
                        "public class TestClass { void execute() { int[] a = new int[10]; for (int i = 0; i < 10; i++) { a[i] = 2 * i; } System.out.println(\"Test\"); } }",
                        SyntaxNodeTreeHandler.SyntaxNoteMotionType.EXPAND,
                        "Edge case: expand from closing brace"
                ),
                // Expand at the end of a file
                new HelixSelectionTestData(
                        new Offsets(0, 196), // "}"
                        "public class TestClass { void execute() { int[] a = new int[10]; for (int i = 0; i < 10; i++) { a[i] = 2 * i; } System.out.println(\"Test\"); } }",
                        new Offsets(0, 196), // whole class body
                        "public class TestClass { void execute() { int[] a = new int[10]; for (int i = 0; i < 10; i++) { a[i] = 2 * i; } System.out.println(\"Test\"); } }",
                        SyntaxNodeTreeHandler.SyntaxNoteMotionType.EXPAND,
                        "Edge case: whole body to whole body"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("expandSelectionTestCases")
    void testExpandSelection(HelixSelectionTestData testData) {
        runHelixSelectionTest(testData);
    }

    @ParameterizedTest
    @MethodSource("shrinkSelectionTestCases")
    void testShrinkSelection(HelixSelectionTestData testData) {
        runHelixSelectionTest(testData);
    }

    @ParameterizedTest
    @MethodSource("edgeCaseTestCases")
    void testEdgeCases(HelixSelectionTestData testData) {
        runHelixSelectionTest(testData);
    }

    private void runHelixSelectionTest(HelixSelectionTestData testData) {
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

        // Verify test prerequisite - initial selection matches expected text
        if (!testData.initialText.isEmpty()) {
            String actualInitialText = javaCode.substring(
                    testData.initialOffsets.leftOffset(),
                    testData.initialOffsets.rightOffset()
            ).trim().replaceAll("\\s+", " ");
            assertTrue(
                    testData.initialText.equals(actualInitialText) ||
                            testData.initialText.contains(actualInitialText) ||
                            actualInitialText.contains(testData.initialText),
                    "Test prerequisite failed: expected initial text '" + testData.initialText +
                            "' but got '" + actualInitialText + "'"
            );
        }

        PsiFile javaFile = myFixture.configureByText("TestClass.java", javaCode);
        SyntaxNodeTreeHandler handler = new SyntaxNodeTreeHandler(javaFile, testData.motionType);

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

                String normalisedActual = actualSelectedText.trim().replaceAll("\\s+", " ");

                assertEquals(testData.expectedText, normalisedActual);
            }
        });
    }
}