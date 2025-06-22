package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.zyurkalov.ideavim.syntaxtreejumper.Direction;
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

class ArgumentMotionHandlerTest {

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

    record ArgumentTestData(
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

    // Forward navigation test cases
    static Stream<ArgumentTestData> forwardNavigationTestCases() {
        return Stream.of(
                // Method call arguments - forward navigation
                new ArgumentTestData(
                        new Offsets(67, 67), // cursor at 'a'
                        "",
                        new Offsets(70, 71), // select 'b'
                        "b",
                        Direction.FORWARD,
                        "Forward: cursor to first argument in method call"
                ),
                new ArgumentTestData(
                        new Offsets(67, 68), // 'a' selected
                        "a",
                        new Offsets(70, 71), // select 'b'
                        "b",
                        Direction.FORWARD,
                        "Forward: first to second argument in method call"
                ),
                new ArgumentTestData(
                        new Offsets(70, 71), // 'b' selected
                        "b",
                        new Offsets(73, 74), // select 'c'
                        "c",
                        Direction.FORWARD,
                        "Forward: second to third argument in method call"
                ),
                new ArgumentTestData(
                        new Offsets(73, 74), // 'c' selected (last argument)
                        "c",
                        new Offsets(73, 74), // stay at 'c'
                        "c",
                        Direction.FORWARD,
                        "Forward: stay at last argument when at boundary"
                ),

                // Constructor call arguments
                new ArgumentTestData(
                        new Offsets(109, 110), // 'd' in constructor
                        "d",
                        new Offsets(112, 113), // 'e' in constructor
                        "e",
                        Direction.FORWARD,
                        "Forward: constructor argument navigation"
                ),

                // Method declaration parameters
                new ArgumentTestData(
                        new Offsets(139, 144),
                        "int x",
                        new Offsets(146, 154),
                        "String y",
                        Direction.FORWARD,
                        "Forward: to the next param"
                ),
                new ArgumentTestData(
                        new Offsets(146, 154),
                        "String y",
                        new Offsets(146, 154),
                        "String y",
                        Direction.FORWARD,
                        "Forward: last param"
                )

//                // Lambda parameters
//                new ArgumentTestData(
//                        new Offsets(208, 209), // 'x' in lambda
//                        "x",
//                        new Offsets(211, 212), // 'y' in lambda
//                        "y",
//                        Direction.FORWARD,
//                        "Forward: lambda parameter navigation"
//                )
        );
    }

    // Backward navigation test cases
    static Stream<ArgumentTestData> backwardNavigationTestCases() {
        return Stream.of(
                // Method call arguments - backward navigation
                new ArgumentTestData(
                        new Offsets(101, 102), // 'c' selected (third argument)
                        "c",
                        new Offsets(98, 99), // select 'b'
                        "b",
                        Direction.BACKWARD,
                        "Backward: third to second argument in method call"
                ),
                new ArgumentTestData(
                        new Offsets(98, 99), // 'b' selected
                        "b",
                        new Offsets(95, 96), // select 'a'
                        "a",
                        Direction.BACKWARD,
                        "Backward: second to first argument in method call"
                ),
                new ArgumentTestData(
                        new Offsets(95, 96), // 'a' selected (first argument)
                        "a",
                        new Offsets(95, 96), // stay at 'a'
                        "a",
                        Direction.BACKWARD,
                        "Backward: stay at first argument when at boundary"
                ),

                // Constructor call arguments
                new ArgumentTestData(
                        new Offsets(132, 133), // 'e' in constructor
                        "e",
                        new Offsets(129, 130), // 'd' in constructor
                        "d",
                        Direction.BACKWARD,
                        "Backward: constructor argument navigation"
                ),

                // Method parameters
                new ArgumentTestData(
                        new Offsets(169, 170), // 'y' parameter name
                        "y",
                        new Offsets(162, 168), // 'String' parameter type
                        "String",
                        Direction.BACKWARD,
                        "Backward: method parameter name to type"
                ),

                // Lambda parameters
                new ArgumentTestData(
                        new Offsets(211, 212), // 'y' in lambda
                        "y",
                        new Offsets(208, 209), // 'x' in lambda
                        "x",
                        Direction.BACKWARD,
                        "Backward: lambda parameter navigation"
                )
        );
    }

    // Edge cases and boundary conditions
    static Stream<ArgumentTestData> edgeCaseTestCases() {
        return Stream.of(
                // Cursor between arguments
                new ArgumentTestData(
                        new Offsets(97, 97), // cursor at comma between 'a' and 'b'
                        "",
                        new Offsets(98, 99), // jump to 'b'
                        "b",
                        Direction.FORWARD,
                        "Edge case: cursor between arguments, forward to next"
                ),
                new ArgumentTestData(
                        new Offsets(97, 97), // cursor at comma between 'a' and 'b'
                        "",
                        new Offsets(95, 96), // jump to 'a'
                        "a",
                        Direction.BACKWARD,
                        "Edge case: cursor between arguments, backward to previous"
                ),

                // Empty argument list
                new ArgumentTestData(
                        new Offsets(239, 239), // cursor in empty parentheses
                        "",
                        new Offsets(239, 239), // stay in place
                        "",
                        Direction.FORWARD,
                        "Edge case: empty argument list"
                ),

                // Complex expressions as arguments
                new ArgumentTestData(
                        new Offsets(264, 275), // 'obj.getValue()' 
                        "obj.getValue()",
                        new Offsets(277, 291), // 'list.size() + 1'
                        "list.size() + 1",
                        Direction.FORWARD,
                        "Edge case: complex expression arguments"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("forwardNavigationTestCases")
    void testForwardNavigation(ArgumentTestData testData) {
        runArgumentNavigationTest(testData);
    }

    @ParameterizedTest
    @MethodSource("backwardNavigationTestCases")
    void testBackwardNavigation(ArgumentTestData testData) {
        runArgumentNavigationTest(testData);
    }

    @ParameterizedTest
    @MethodSource("edgeCaseTestCases")
    void testEdgeCases(ArgumentTestData testData) {
        runArgumentNavigationTest(testData);
    }

    private void runArgumentNavigationTest(ArgumentTestData testData) {
        String javaCode = """
                public class ArgumentTest {
                    void testMethod() {
                        method(a, b, c);
                        Object obj = new Object(d, e);
                    }
                    
                    void method(int x, String y) {
                        // Method with parameters
                    }
                    
                    void lambdaTest() {
                        BiFunction<String, String, String> func = (x, y) -> x + y;
                        emptyMethod();
                        complexMethod(obj.getValue(), list.size() + 1, "constant");
                    }
                    
                    void emptyMethod() {}
                }
                """;
        System.out.println(javaCode.indexOf( "method(int x"));

        // Verify test prerequisite - initial selection matches expected text (if any)
        if (!testData.initialText.isEmpty()) {
            String actualInitialText = javaCode.substring(
                    testData.initialOffsets.leftOffset(),
                    testData.initialOffsets.rightOffset()
            ).trim();
            assertTrue(
                    testData.initialText.equals(actualInitialText) ||
                    testData.initialText.contains(actualInitialText) ||
                    actualInitialText.contains(testData.initialText),
                    "Test prerequisite failed: expected initial text '" + testData.initialText +
                            "' but got '" + actualInitialText + "'"
            );
        }

        PsiFile javaFile = myFixture.configureByText("ArgumentTest.java", javaCode);
        ArgumentMotionHandler handler = new ArgumentMotionHandler(javaFile, testData.direction);

        ApplicationManager.getApplication().runReadAction(() -> {
            Optional<Offsets> result = handler.findNext(testData.initialOffsets);
            assertTrue(result.isPresent(), "Handler should return a result");

            Offsets actualOffsets = result.get();
            assertEquals(testData.expectedOffsets, actualOffsets, testData.explanation);

            // Verify the selected text matches expectations (if expected text is not empty)
            if (!testData.expectedText.isEmpty()) {
                String actualSelectedText = javaCode.substring(
                        actualOffsets.leftOffset(),
                        actualOffsets.rightOffset()
                ).trim();

                assertEquals(testData.expectedText, actualSelectedText,
                        "Selected text should match expected text for: " + testData.explanation);
            }
        });
    }
}