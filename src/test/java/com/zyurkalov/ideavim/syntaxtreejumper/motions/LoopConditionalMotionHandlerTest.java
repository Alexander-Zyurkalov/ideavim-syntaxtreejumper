package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.PsiSyntaxTreeAdapter;
import com.zyurkalov.ideavim.syntaxtreejumper.adapters.SyntaxTreeAdapter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoopConditionalMotionHandlerTest {

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

    record LoopConditionalTestData(
            Offsets initialOffsets,
            String initialText,
            Offsets expectedOffsets,
            String expectedText,
            MotionDirection direction,
            String explanation,
            AbstractFindNodeMotionHandler.WhileSearching whileSearching
    ) {
        @Override
        public @NotNull String toString() {
            return explanation;
        }
    }

    static Stream<LoopConditionalTestData> forwardNavigationTestCases() {
        return Stream.of(
//                new LoopConditionalTestData(
//                        new Offsets(29, 149), // Select the entire first function (methodWithLoop)
//                        """
//                                public void methodWithLoop() {
//                                    for (int i = 0; i < 10; i++) {
//                                        System.out.println(i);
//                                    }
//                                }""",
//                        new Offsets(68, 143), // Jump to while loop in the second function
//                        """
//                                for (int i = 0; i < 10; i++) {
//                                            System.out.println(i);
//                                        }""",
//                        SHRINK,
//                        "Forward: Should not skip loop inside selected function and jump to it",
//                        AbstractFindNodeMotionHandler.WhileSearching.DO_NOT_SKIP_INITIAL_SELECTION
//                )
//                new LoopConditionalTestData(
//                        new Offsets(29, 149), // Select the entire first function (methodWithLoop)
//                        """
//                                public void methodWithLoop() {
//                                    for (int i = 0; i < 10; i++) {
//                                        System.out.println(i);
//                                    }
//                                }""",
//                        new Offsets(224, 311), // Jump to while loop in the second function
//                        """
//                                while (count < 5) {
//                                            count++;
//                                            if (count == 3) {break;}
//                                        }""",
//                        FORWARD,
//                        "Forward: Should skip loop inside selected function and jump to loop in next function",
//                        AbstractFindNodeMotionHandler.WhileSearching.SKIP_INITIAL_SELECTION
//                ),
                new LoopConditionalTestData(
                        new Offsets(224, 311),
                        """
                                while (count < 5) {
                                            count++;
                                            if (count == 3) {break;}
                                        }""",
                        new Offsets(63, 147),
                        """
                                for (int i = 0; i < 10; i++) {
                                        System.out.println(i);
                                    }
                                """,
                        BACKWARD,
                        "Backward: Should skip loop inside selected function and jump to loop in next function",
                        AbstractFindNodeMotionHandler.WhileSearching.SKIP_INITIAL_SELECTION
                ),
                new LoopConditionalTestData(
                        new Offsets(277, 301), // Select the entire first function (methodWithLoop)
                        "if (count == 3) {break;}",
                        new Offsets(224, 311), // Jump to while loop in the second function
                        """
                                while (count < 5) {
                                            count++;
                                            if (count == 3) {break;}
                                        }""",

                        EXPAND,
                        "EXPAND: Should go from the inner condition to the outer loop",
                        AbstractFindNodeMotionHandler.WhileSearching.SKIP_INITIAL_SELECTION
                ),
                new LoopConditionalTestData(
                        new Offsets(278, 279),
                        "f",
                        new Offsets(224, 311),
                        """
                                while (count < 5) {
                                            count++;
                                            if (count == 3) {break;}
                                        }""",

                        EXPAND,
                        "EXPAND: Should go from one symbol to the outer loop",
                        AbstractFindNodeMotionHandler.WhileSearching.SKIP_INITIAL_SELECTION
                ),
                new LoopConditionalTestData(
                        new Offsets(224, 311),
                        """
                                while (count < 5) {
                                            count++;
                                            if (count == 3) {break;}
                                        }""",
                        new Offsets(277, 301),
                        "if (count == 3) {break;}",

                        SHRINK,
                        "SHRINK: Should go from the outer loop to the inner condition",
                        AbstractFindNodeMotionHandler.WhileSearching.SKIP_INITIAL_SELECTION
                )
        );
    }

    @ParameterizedTest
    @MethodSource("forwardNavigationTestCases")
    void testForwardNavigation(LoopConditionalTestData testData) {
        runLoopConditionalTest(testData);
    }

    private void runLoopConditionalTest(LoopConditionalTestData testData) {
        String javaCode = """
                public class TestClass {
                    public void methodWithLoop() {
                        for (int i = 0; i < 10; i++) {
                            System.out.println(i);
                        }
                    }
                
                    public void anotherMethodWithLoop() {
                        int count = 0;
                        while (count < 5) {
                            count++;
                            if (count == 3) {break;}
                        }
                    }
                }
                """;

        PsiFile javaFile = myFixture.configureByText("TestClass.java", javaCode);
        LoopConditionalMotionHandler handler = new LoopConditionalMotionHandler(
                new PsiSyntaxTreeAdapter(javaFile), testData.direction, testData.whileSearching);

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

            // Verify the selected text if provided
            if (result.isPresent() && !testData.expectedText.isEmpty()) {
                String actualSelectedText = javaCode.substring(
                        result.get().leftOffset(),
                        result.get().rightOffset()
                );
                assertEquals(testData.expectedText.trim(), actualSelectedText.trim());
            }
        });
    }
}