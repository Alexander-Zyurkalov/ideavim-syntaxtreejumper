package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.PsiSyntaxTreeAdapter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartSelectionExtendHandlerTest {

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

    record SmartSelectionTestData(
            Offsets initialOffsets,
            String initialText,
            Offsets expectedOffsets,
            String expectedText,
            String explanation
    ) {
        @Override
        public @NotNull String toString() {
            return explanation;
        }
    }

    static Stream<SmartSelectionTestData> smartExtensionTestCases() {
        return Stream.of(
                new SmartSelectionTestData(
                        new Offsets(62, 63), // Initial: cursor placed at the number '1'
                        "1",
                        new Offsets(62, 66), // Expected: selects "1 +" part of the expression
                        "1 + ",
                        "Select first operand with operator from cursor on '1'"
                ),
                new SmartSelectionTestData(
                        new Offsets(62, 64), // Initial: cursor placed at "1 "
                        "1 ",
                        new Offsets(62, 66), // Expected: selects "1 +" part of the expression
                        "1 + ",
                        "Select first operand with leading space in single addition expression"
                ),
                new SmartSelectionTestData(
                        new Offsets(66, 67), // Initial: selection only contains "2"
                        "2",
                        new Offsets(63, 67), // Expected: expands to "+ 2" including operator
                        " + 2",
                        "Expand '2' selection to include operator"
                ),
                new SmartSelectionTestData( // TODO: actually we should do something different in this case
                        new Offsets(64, 65), // Initial: selection only contains "+"
                        "+",
                        new Offsets(64, 67), // Expected: expands to "+ 2" including operator
                        "+ 2",
                        "Expand '+' selection to include 2"
                ),
                new SmartSelectionTestData(
                        new Offsets(85, 86), // Initial: cursor on '1' in "1 + 2 + 3" expression
                        "",
                        new Offsets(85, 89), // Expected: selects "1 +" part
                        "1 + ",
                        "Select first operand with operator in triple addition"
                ),
                new SmartSelectionTestData(
                        new Offsets(89, 90), // Initial: cursor on '2' in "1 + 2 + 3" expression
                        "2",
                        new Offsets(89, 93), // Expected: selects "2 +"
                        "2 + ",
                        "Select first and last operands in triple addition"
                ),
                new SmartSelectionTestData(
                        new Offsets(93, 94), // Initial: cursor on '2' in "1 + 2 + 3" expression
                        "3",
                        new Offsets(90, 94), // Expected: selects "+ 3"
                        " + 3",
                        "Select the last operand and the operator before it"
                ),
                new SmartSelectionTestData(
                        new Offsets(143, 144), // Initial: cursor on '1' in "1+2 +3"
                        "1",
                        new Offsets(143, 145), // Expected: selects "1+"
                        "1+",
                        "Select first operand with operator in mixed spacing expression"
                ),
                new SmartSelectionTestData(
                        new Offsets(145, 146), // Initial: cursor on '2' in "1+2 +3"
                        "2",
                        new Offsets(145, 148), // Expected: selects "+2"
                        "2 +",
                        "Select second operand with left operator in mixed spacing"
                ),
                new SmartSelectionTestData(
                        new Offsets(167, 168), // Initial: cursor on '1' in "1+2+3"
                        "1",
                        new Offsets(167, 169), // Expected: selects "1+"
                        "1+",
                        "Select first operand with operator in no-space expression"
                ),
                new SmartSelectionTestData(
                        new Offsets(190, 191), // Initial: cursor on '1' in "1 +2+3"
                        "1",
                        new Offsets(190, 193), // Expected: selects "1 +"
                        "1 +",
                        "Select first operand with operator in leading space expression"
                ),
                new SmartSelectionTestData(
                        new Offsets(195, 195), // Initial: cursor on '1' in "1 +2+3"
                        "3",
                        new Offsets(194, 196), // Expected: selects "1 +"
                        "+3",
                        "Select last operand with operator in leading space expression"
                ),
                new SmartSelectionTestData(
                        new Offsets(192, 194), // Initial: When "+2" is selected initially
                        "+2",
                        new Offsets(193, 195), // Expected: Selection will shift to "2+"
                        "2+",
                        "Permutate selections if already preselected: forward"
                ),
                new SmartSelectionTestData(
                        new Offsets(193, 195), // Initial: When "2+" is selected initially
                        "2+",
                        new Offsets(191, 194), // Expected: Selection will shift to "+2"
                        " +2",
                        "Permutate selections if already preselected: backward"
                ),
                // Tests for function parameters - starting with the whole argument selected
                new SmartSelectionTestData(
                        new Offsets(230, 235), // Initial: selection covers the whole first parameter "int a"
                        "int a",
                        new Offsets(230, 237), // Expected: extends to include comma "int a, "
                        "int a, ",
                        "Extend first parameter 'int a' to include comma"
                ),
                new SmartSelectionTestData(
                        new Offsets(237, 242), // Initial: selection covers the whole second parameter "int b"
                        "int b",
                        new Offsets(235, 242), // Expected: extends backward to include comma ", int b"
                        ", int b",
                        "Extend second parameter 'int b' to include preceding comma"
                ),
                new SmartSelectionTestData(
                        new Offsets(318, 323), // Initial: selection covers parameter "int b" in three argument functions
                        "int b",
                        new Offsets(318, 325), // Expected: extends forward to include comma "int b, "
                        "int b, ",
                        "Extend middle parameter 'int b' to include following comma in three argument function"
                ),
                new SmartSelectionTestData(
                        new Offsets(318, 325), // Initial: "int b, " selected 
                        "int b, ",
                        new Offsets(316, 323), // Expected: extends to ", int b" 
                        ", int b",
                        "Permutate middle parameter selection in three argument function from forward to backward"
                )
        );
    }

    static Stream<SmartSelectionTestData> edgeCaseTestCases() {
        return Stream.empty();
    }

    @ParameterizedTest
    @MethodSource("smartExtensionTestCases")
    void testSmartSelectionExtension(SmartSelectionTestData testData) {
        runSmartSelectionTest(testData);
    }

    //    @ParameterizedTest
//    @MethodSource("edgeCaseTestCases")
    void testEdgeCases(SmartSelectionTestData testData) {
        runSmartSelectionTest(testData);
    }

    private void runSmartSelectionTest(SmartSelectionTestData testData) {
        String javaCode = """
                public class TestClass {
                    void execute() {
                        int a = 1 + 2;
                        int b = 1 + 2 + 3;
                        int c = 1 + 2 + 3 + 4;
                        int d = 1+2 +3;
                        int e = 1+2+3;
                        int f = 1 +2+3;
                    }
                    int funcWithArguments(int a, int b) {
                        return a + b;
                    }
                    int anotherFunctionWithArguments(int a, int b, int c) {
                        return a + b + c;
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
        SmartSelectionExtendHandler handler = new SmartSelectionExtendHandler(new PsiSyntaxTreeAdapter(javaFile));

        ApplicationManager.getApplication().runReadAction(() -> {
            Optional<Offsets> result = handler.findNext(testData.initialOffsets);
            assertTrue(result.isPresent(), "Handler should return a result");

            Offsets actualOffsets = result.get();
            assertEquals(testData.expectedOffsets, actualOffsets, testData.explanation);

            // Verify the selected text matches expectations if provided
            if (!testData.expectedText.isEmpty()) {
                String actualSelectedText = javaCode.substring(
                        actualOffsets.leftOffset(),
                        actualOffsets.rightOffset()
                );

                assertEquals(testData.expectedText, actualSelectedText,
                        "Selected text should match expected: " + testData.explanation);
            }
        });
    }
}

