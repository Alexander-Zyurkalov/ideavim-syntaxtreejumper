package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection.BACKWARD;
import static com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection.FORWARD;

public class SubWordFinderTest {
    public SubWordFinderTest() {
    }

    record SubWordTestCase(
            String text,
            MotionDirection direction,
            Offsets startPosition,
            Offsets expectedOffsets,
            String explanation
    ) {
        @Override
        public @NotNull String toString() {
            return explanation;
        }
    }

    static Stream<SubWordTestCase> nextSubWordTestCase() {
        return Stream.of(
                new SubWordTestCase("PascalCaseTest", FORWARD, new Offsets(0, 7), new Offsets(6, 10), "PascalCase: Navigate forward from 'Pascal' to 'Case'"),
                new SubWordTestCase("PascalCaseTest", FORWARD, new Offsets(0, 0), new Offsets(0, 6), "PascalCase: Navigate forward from `P` to 'Case'"),
                new SubWordTestCase("PascalCaseTest", BACKWARD, new Offsets(10, 14), new Offsets(6, 10), "PascalCase: Navigate backward from 'Test' to 'Case'"),
                new SubWordTestCase("PascalCaseTest", BACKWARD, new Offsets(10, 10), new Offsets(10, 11), "PascalCase: Navigate backward from 'T' to 'T' selected"),
                new SubWordTestCase("PascalCaseTest", BACKWARD, new Offsets(10, 11), new Offsets(6, 10), "PascalCase: Navigate backward from 'T' to 'Case'"),
                new SubWordTestCase("PascalCaseTest", FORWARD, new Offsets(6, 10), new Offsets(10, 14), "PascalCase: Navigate forward from 'Case' to 'Test'"),
                new SubWordTestCase("PascalCaseTest", FORWARD, new Offsets(7, 10), new Offsets(10, 14), "PascalCase: Navigate forward from within 'Case' to 'Test'"),
                new SubWordTestCase("PascalCaseTest", FORWARD, new Offsets(7, 7), new Offsets(7, 10), "PascalCase: Navigate forward from within 'a' to 'Case'"),
                new SubWordTestCase("PascalCaseTest", FORWARD, new Offsets(9, 10), new Offsets(10, 14), "PascalCase: Navigate forward from end of 'Case' to 'Test'"),
                new SubWordTestCase("PascalCaseTest", FORWARD, new Offsets(9, 9), new Offsets(9, 10), "PascalCase: Navigate forward from end of 'Case' to 'Test'"),
                new SubWordTestCase("PascalCaseTest", FORWARD, new Offsets(10, 10), new Offsets(10, 14), "PascalCase: Navigate forward from `T` to 'Test'"),
                new SubWordTestCase("PascalCaseTest", FORWARD, new Offsets(10, 11), new Offsets(10, 14), "PascalCase: Navigate forward from `T` selected to 'Test'"),
                new SubWordTestCase("PascalCaseTest", BACKWARD, new Offsets(6, 10), new Offsets(0, 6), "PascalCase: Navigate backward from 'Case' to 'Pascal'"),
                new SubWordTestCase("PascalCaseTest", FORWARD, new Offsets(13, 13), new Offsets(13, 14), "PascalCase: staying at the place when there is nowhere to go"),
                new SubWordTestCase("camelCaseTest", FORWARD, new Offsets(0, 5), new Offsets(5, 9), "camelCase: Navigate forward from 'camel' to 'Case'"),
                new SubWordTestCase("camelCaseTest", BACKWARD, new Offsets(9, 13), new Offsets(5, 9), "camelCase: Navigate backward from 'Test' to 'Case'"),
                new SubWordTestCase("camelCaseTest", BACKWARD, new Offsets(5, 9), new Offsets(0, 5), "camelCase: Navigate backward from 'Case' to 'camel'"),
                new SubWordTestCase("snake_case_test", FORWARD, new Offsets(0, 5), new Offsets(5, 6), "snake_case: Navigate forward from 'snake' to underscore"),
                new SubWordTestCase("snake_case_test", FORWARD, new Offsets(6, 10), new Offsets(10, 11), "snake_case: Navigate forward from 'case' to underscore"),
                new SubWordTestCase("snake_case_test", BACKWARD, new Offsets(6, 10), new Offsets(5, 6), "snake_case: Navigate backward from 'case' to first underscore"),
                new SubWordTestCase("snake_case_test", FORWARD, new Offsets(5, 5), new Offsets(5, 6), "snake_case: Navigate forward from underscore to select the underscore"),
                new SubWordTestCase("snake_case_test", BACKWARD, new Offsets(5, 5), new Offsets(5, 6), "snake_case: Navigate backward from underscore to select the underscore"),
                new SubWordTestCase("snake_case_test", FORWARD, new Offsets(5, 6), new Offsets(6, 10), "snake_case: Navigate forward from underscore to 'case'"),
                new SubWordTestCase("snake_case_test", BACKWARD, new Offsets(5, 6), new Offsets(0, 5), "snake_case: Navigate backward from underscore to 'snake'"),
                new SubWordTestCase("snake_1234_test", FORWARD, new Offsets(5, 6), new Offsets(6, 10), "snake_case: Navigate forward from underscore to digits"),
                new SubWordTestCase("snake_1234_test", FORWARD, new Offsets(6, 7), new Offsets(10, 11), "snake_case: Navigate forward from underscore to digits"),
                new SubWordTestCase("Pascal1CaseTest", FORWARD, new Offsets(0, 6), new Offsets(6, 7), "Mixed with digits: Navigate forward from 'Pascal' to single digit"),
                new SubWordTestCase("Pascal123CaseTest", FORWARD, new Offsets(0, 6), new Offsets(6, 9), "Mixed with digits: Navigate forward from 'Pascal' to multiple digits"),
                new SubWordTestCase("Pascal123CaseTest", FORWARD, new Offsets(6, 9), new Offsets(9, 13), "Mixed with digits: Navigate forward from digits to 'Case'"),
                new SubWordTestCase("Pascal123CaseTest", BACKWARD, new Offsets(6, 9), new Offsets(0, 6), "Mixed with digits: Navigate backward from digits to 'Pascal'"),
                new SubWordTestCase("1234PascalCaseTest", BACKWARD, new Offsets(4, 9), new Offsets(0, 4), "Mixed with digits: Navigate backward from 'Pascal' to leading digits"),
                new SubWordTestCase("My custom  test", FORWARD, new Offsets(3, 9), new Offsets(9, 11), "Custom text: from a word to a space"),
                new SubWordTestCase("My custom test", FORWARD, new Offsets(2, 3), new Offsets(3, 9), "Custom text: from a space to a word"),
                new SubWordTestCase("My кастом test", FORWARD, new Offsets(2, 3), new Offsets(3, 9), "Custom text in Russian: from a space to a word")
        );
    }

    @ParameterizedTest
    @MethodSource("nextSubWordTestCase")
    void testFindNext(SubWordTestCase testCase) {
        SubWordFinder subWordFinder = new SubWordFinder(testCase.direction());
        Offsets offsets = subWordFinder.findNext(testCase.startPosition(), testCase.text());
        Assertions.assertEquals(testCase.expectedOffsets(), offsets, testCase.explanation());
    }
}
