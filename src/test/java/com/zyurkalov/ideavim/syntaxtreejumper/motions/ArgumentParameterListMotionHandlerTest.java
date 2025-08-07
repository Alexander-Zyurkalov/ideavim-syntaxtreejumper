package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
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

class ArgumentParameterListMotionHandlerTest {

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

    record ArgumentParameterListTestData(
            Offsets initialOffsets,
            String initialText,
            Offsets expectedOffsets,
            String expectedText,
            Direction direction,
            String explanation
    ) {
        @Override
        public @NotNull String toString() {
            return explanation;
        }
    }

    static Stream<ArgumentParameterListTestData> forwardNavigationTestCases() {
        return Stream.of(
                new ArgumentParameterListTestData(
                        new Offsets(57, 58),
                        "(",
                        new Offsets(58, 68),
                        "int param1",
                        Direction.FORWARD,
                        "Forward: search within neighbours"
                ),
                new ArgumentParameterListTestData(
                        new Offsets(57, 57),
                        "(",
                        new Offsets(58, 68),
                        "int param1",
                        Direction.FORWARD,
                        "Forward: search within neighbours with no selection"
                ),
                new ArgumentParameterListTestData(
                        new Offsets(58, 68), // at int param1
                        "int param1",
                        new Offsets(70, 83),
                        "String param2",
                        Direction.FORWARD,
                        "Forward: jump to next parameter"
                ),
                new ArgumentParameterListTestData(
                        new Offsets(41, 57),
                        "methodWithParams",
                        new Offsets(58, 68),
                        "int param1",
                        Direction.FORWARD,
                        "Forward: searching in the next element recursively"
                ),
                new ArgumentParameterListTestData(
                        new Offsets(13, 22),
                        "TestClass",
                        new Offsets(58, 68),
                        "int param1",
                        Direction.FORWARD,
                        "Forward: searching in the next element with deeper recursion"
                ),
                new ArgumentParameterListTestData(
                        new Offsets(85, 99),
                        "boolean param3",
                        new Offsets(209, 217),
                        "double x",
                        Direction.FORWARD,
                        "Forward: by going to the parent first"
                ),
                new ArgumentParameterListTestData(
                        new Offsets(219, 227),
                        "double y",
                        new Offsets(261, 262),
                        "x",
                        Direction.FORWARD,
                        "Forward: finding a function call "
                ),
                new ArgumentParameterListTestData(
                        new Offsets(261, 262),
                        "x",
                        new Offsets(264, 265),
                        "y",
                        Direction.FORWARD,
                        "Forward: finding the next function call argument"
                )
        );
    }

    static Stream<ArgumentParameterListTestData> backwardNavigationTestCases() {
        return Stream.of(
                new ArgumentParameterListTestData(
                        new Offsets(99, 100),
                        ")",
                        new Offsets(85, 99),
                        "boolean param3",
                        Direction.BACKWARD,
                        "Backward: search within neighbours"
                ),
                new ArgumentParameterListTestData(
                        new Offsets(99, 99),
                        ")",
                        new Offsets(85, 99),
                        "boolean param3",
                        Direction.BACKWARD,
                        "Backward: search within neighbours with no selection"
                ),
                new ArgumentParameterListTestData(
                        new Offsets(70, 83), // at String param2
                        "String param2",
                        new Offsets(58, 68),
                        "int param1",
                        Direction.BACKWARD,
                        "Backward: jump to previous parameter"
                ),

                new ArgumentParameterListTestData(
                        new Offsets(101, 178),
                        "{\n" +
                                "                       int a = 1 + 2 + 3 + 4 + 5 +5;\n" +
                                "                        String b = \"singleStr\";\n" +
                                "                    }",
                        new Offsets(85, 99),
                        "boolean param3",
                        Direction.BACKWARD,
                        "Backward: searching in the next element recursively"
                ),
                new ArgumentParameterListTestData(
                        new Offsets(264, 265),
                        "y",
                        new Offsets(261, 262),
                        "x",
                        Direction.BACKWARD,
                        "Backward: finding the prev function call argument"
                )
        );
    }

    static Stream<ArgumentParameterListTestData> edgeCaseTestCases() {
        return Stream.of(
                // TODO: Add edge cases
                new ArgumentParameterListTestData(
                        new Offsets(0, 0), // beginning of a file
                        "",
                        new Offsets(0, 0), // should stay at the beginning
                        "",
                        Direction.BACKWARD,
                        "Edge case: no previous parameter/argument list"
                ),

                new ArgumentParameterListTestData(
                        new Offsets(300, 300), // end of a file
                        "",
                        new Offsets(300, 300), // should stay at the end
                        "",
                        Direction.FORWARD,
                        "Edge case: no next parameter/argument list"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("forwardNavigationTestCases")
    void testForwardNavigation(ArgumentParameterListTestData testData) {
        runArgumentParameterListTest(testData);
    }

    @ParameterizedTest
    @MethodSource("backwardNavigationTestCases")
    void testBackwardNavigation(ArgumentParameterListTestData testData) {
        runArgumentParameterListTest(testData);
    }

    @ParameterizedTest
    @MethodSource("edgeCaseTestCases")
    void testEdgeCases(ArgumentParameterListTestData testData) {
        runArgumentParameterListTest(testData);
    }

    class TestClass {
        public void methodWithParams(int param1, String param2, boolean param3) {

        }

        public double anotherMethod(double x, double y) {
            return (int) Math.max(x, 2);
        }
    }

    private void runArgumentParameterListTest(ArgumentParameterListTestData testData) {
        // TODO: Create appropriate test Java code with parameter lists and argument lists
        String javaCode = """
                public class TestClass {
                    public void methodWithParams(int param1, String param2, boolean param3) {
                        int a = 1 + 2 + 3 + 4 + 5 +5;
                        String b = "singleStr";
                    }
                
                    public int anotherMethod(double x, double y) {
                        return (int) Math.max(x, y);
                    }
                }
                """;

        PsiFile javaFile = myFixture.configureByText("TestClass.java", javaCode);
        ArgumentParameterListMotionHandler handler = new ArgumentParameterListMotionHandler(
                new PsiSyntaxTreeAdapter(javaFile), testData.direction);

        ApplicationManager.getApplication().runReadAction(() -> {
            Optional<Offsets> result = handler.findNext(testData.initialOffsets);

            if (testData.expectedOffsets.equals(testData.initialOffsets)) {
                // Expecting no movement - either no result or the same position
                assertTrue(result.isEmpty() || result.get().equals(testData.initialOffsets),
                        "Should not move from initial position: " + testData.explanation);
            } else {
                assertTrue(result.isPresent(), "Handler should return a result: " + testData.explanation);
                assertEquals(testData.expectedOffsets, result.get(), testData.explanation);
            }

            // TODO: Add verification for the selected text if needed
            if (result.isPresent() && !testData.expectedText.isEmpty()) {
                String actualSelectedText = javaCode.substring(
                        result.get().leftOffset(),
                        result.get().rightOffset()
                );
                assertEquals(testData.expectedText, actualSelectedText.trim());
            }
        });
    }

}