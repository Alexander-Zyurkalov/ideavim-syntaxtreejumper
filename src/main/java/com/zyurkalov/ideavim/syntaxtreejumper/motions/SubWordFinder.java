package com.zyurkalov.ideavim.syntaxtreejumper.motions;

import com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection;
import com.zyurkalov.ideavim.syntaxtreejumper.Offsets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection.BACKWARD;
import static com.zyurkalov.ideavim.syntaxtreejumper.MotionDirection.FORWARD;

public record SubWordFinder(MotionDirection direction) {

    class BoundaryFinder {
        private final String str;
        Map<Integer, Integer> memoization;
        List<Function<Integer, Integer>> delimiters;

        private Function<Integer, Integer> createDelimiter(
                Function<Character, Boolean> firstChar,
                Function<Character, Boolean> secondChar) {
            return position -> position < str.length() - 1 &&
                    firstChar.apply(str.charAt(position)) &&
                    secondChar.apply(str.charAt(position + 1))
                    ? position + 1 : position;
        }

        BoundaryFinder(String str) {
            this.str = str;
            this.delimiters = new ArrayList<>();
            if (direction == FORWARD) { //TODO: what shall I do here?
                delimiters.add(createDelimiter(
                        Character::isLowerCase,
                        Character::isUpperCase));
            } else {
                delimiters.add(createDelimiter(
                        Character::isUpperCase,
                        Character::isLowerCase));
            }
            delimiters.add(createDelimiter(c -> c != '_', c -> c == '_'));
            delimiters.add(createDelimiter(c -> c == '_', c -> c != '_'));
            delimiters.add(createDelimiter(c -> !Character.isDigit(c), Character::isDigit));
            delimiters.add(createDelimiter(Character::isDigit, c -> !Character.isDigit(c)));
            delimiters.add(createDelimiter(Character::isLetter, c -> !Character.isLetter(c)));
            delimiters.add(createDelimiter(c -> !Character.isLetter(c), Character::isLetter));
            memoization = new HashMap<>(str.length());
        }


        int findBorder(int startWith) {
            if (startWith < 0) {
                return 0;
            }
            if (startWith >= str.length()) {
                return startWith;
            }
            if (memoization.containsKey(startWith)) {
                return memoization.get(startWith);
            }

            for (var delimiter : delimiters) {
                int potentialBorder = delimiter.apply(startWith);
                if (potentialBorder > startWith) {
                    memoization.put(startWith, potentialBorder);
                    return potentialBorder;
                }
            }
            int potentialBorder = findBorder(startWith + 1);
            int border = potentialBorder <= str.length() ? potentialBorder : str.length() + 1;
            memoization.put(startWith, border);
            return border;
        }
    }

    public Offsets findNext(Offsets strPosition, String elementText) {
        if (elementText == null) {
            return strPosition;
        }
        if (direction == BACKWARD) {
            elementText = new StringBuilder(elementText).reverse().toString();
            strPosition = new Offsets(
                    elementText.length() - strPosition.rightOffset(),
                    elementText.length() - strPosition.leftOffset()
            );
            if (strPosition.leftOffset() == strPosition.rightOffset()) {
                strPosition = new Offsets(strPosition.leftOffset() - 1, strPosition.rightOffset() - 1);
            }
        }

        var finder = new BoundaryFinder(elementText);
        int nextWordStart = finder.findBorder(strPosition.leftOffset());
        if (strPosition.leftOffset() == strPosition.rightOffset() || nextWordStart == elementText.length()) {
            nextWordStart = strPosition.leftOffset();
        }

        int nextWordEnd = finder.findBorder(nextWordStart);


        return switch (direction) {
            case FORWARD -> new Offsets(nextWordStart, nextWordEnd);
            case BACKWARD -> new Offsets(
                    elementText.length() - nextWordEnd,
                    elementText.length() - nextWordStart);
            case EXPAND -> null; //TODO: what shall I do here?
            case SHRINK -> null;
        };
    }

}