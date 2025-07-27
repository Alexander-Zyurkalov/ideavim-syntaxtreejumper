package com.zyurkalov.ideavim.syntaxtreejumper.adapters;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.FakePsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class FakePsiElementTreeBuilder {
    public static class MyFakePsiElement extends FakePsiElement {
        private final String text;
        private final List<MyFakePsiElement> children;
        private final String type;
        private MyFakePsiElement parent;
        private MyFakePsiElement prevSibling;
        private MyFakePsiElement nextSibling;

        private MyFakePsiElement(String text, List<MyFakePsiElement> children) {
            this.text = text;
            this.children = children;
            this.type = switch (text) {
                case "for" -> "FOR_KEYWORD";
                case "while" -> "WHILE_KEYWORD";
                case "int" -> "INT_KEYWORD";
                case "" -> "DUMMY_NODE";
                case " " -> "WHITE_SPACE";
                case "(" -> "OCPunctuator:(";
                case ")" -> "OCPunctuator:)";
                case "=" -> "OCPunctuator:=";
                case ";" -> "OCPunctuator:;";
                case "<" -> "OCPunctuator:<";
                case "++" -> "OCPunctuator:++";
                case "[" -> "OCPunctuator:[";
                case "]" -> "OCPunctuator:]";
                case "*" -> "OCPunctuator:*";
                default -> {
                    // Check if it's an integer literal (all digits)
                    if (text.matches("\\d+")) {
                        yield "INTEGER_LITERAL";
                    }
                    // Check if it's an identifier (starts with letter/underscore, contains alphanumeric/underscore)
                    else if (text.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                        yield "IDENTIFIER";
                    }
                    else {
                        yield "UNKNOWN";
                    }
                }
            };
            MyFakePsiElement prev = null;
            for (int i = 0; i < children.size(); i++) {
                var child = children.get(i);
                child.parent = this;
                child.prevSibling = prev;
                child.nextSibling = (i + 1 < children.size()) ? children.get(i + 1) : null;
                prev = child;
            }

        }

        public MyFakePsiElement(String text) {
            this(text, new ArrayList<>());
        }

        public String getText() {
            return text;
        }

        @Override
        public PsiElement @NotNull [] getChildren() {
            return children.toArray(PsiElement[]::new);
        }

        @Override
        public PsiElement getParent() {
            return parent;
        }

        @Override
        public @Nullable PsiElement getNextSibling() {
            return nextSibling;
        }

        @Override
        public @Nullable PsiElement getPrevSibling() {
            return prevSibling;
        }

        public String getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            MyFakePsiElement that = (MyFakePsiElement) o;
            return Objects.equals(text, that.text) && Objects.equals(children, that.children);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("'").append(text).append("':     '").append(type).append("'");
            if (!children.isEmpty()) {
                for (MyFakePsiElement child : children) {
                    String[] childLines = child.toString().split("\n");
                    sb.append("\n    ").append(childLines[0]);
                    for (int i = 1; i < childLines.length; i++) {
                        sb.append("\n    ").append(childLines[i]);
                    }
                }
            }
            return sb.toString();
        }
    }

    public static MyFakePsiElement leaf(String text) {
        return new MyFakePsiElement(text, new ArrayList<>());
    }

    public static MyFakePsiElement branch(MyFakePsiElement... children) {
        List<MyFakePsiElement> psiList = Arrays.stream(children).toList();
        return branch(psiList);
    }

    private static @NotNull MyFakePsiElement branch(List<MyFakePsiElement> psiList) {
        // Apply the same flattening logic as in list()
        List<MyFakePsiElement> flattened = psiList.stream()
                .flatMap(element -> {
                    if ("list".equals(element.getText())) {
                        // If it's a list, return its children as a stream
                        return Arrays.stream(element.getChildren())
                                .map(child -> (MyFakePsiElement) child);
                    } else {
                        // If it's not a list, return the element itself as a single-element stream
                        return Stream.of(element);
                    }
                })
                .toList();

        return new MyFakePsiElement("", flattened);
    }

    public static MyFakePsiElement branch(String... children) {
        return branch(Arrays.stream(children).map(MyFakePsiElement::new).toList());
    }

    public static MyFakePsiElement list(MyFakePsiElement... elements) {
        return list(Arrays.stream(elements).toList());
    }

    public static MyFakePsiElement list(String... strs) {
        return list(Arrays.stream(strs).map(MyFakePsiElement::new).toList());
    }

    private static MyFakePsiElement list(List<MyFakePsiElement> elements) {
        List<MyFakePsiElement> flattened = elements.stream()
                .flatMap(element -> {
                    if ("list".equals(element.getText())) {
                        // If it's a list, return its children as a stream
                        return Arrays.stream(element.getChildren())
                                .map(child -> (MyFakePsiElement) child);
                    } else {
                        // If it's not a list, return the element itself as a single-element stream
                        return Stream.of(element);
                    }
                })
                .toList();

        return new MyFakePsiElement("list", flattened);
    }

    public static MyFakePsiElement listFromString(String str) {
        List<MyFakePsiElement> elements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (char c : str.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_') {
                current.append(c);
            } else {
                if (!current.isEmpty()) {
                    elements.add(new MyFakePsiElement(current.toString()));
                    current.setLength(0);
                }
                elements.add(new MyFakePsiElement(String.valueOf(c)));
            }
        }

        if (!current.isEmpty()) {
            elements.add(new MyFakePsiElement(current.toString()));
        }

        return new MyFakePsiElement("list", elements);
    }

    public static MyFakePsiElement makeForLoop(MyFakePsiElement initialization, MyFakePsiElement condition, MyFakePsiElement increment, MyFakePsiElement body) {
        return list(
                branch(listFromString("for ("), initialization, leaf(";")),
                leaf(" "),
                branch(condition, leaf(";")),
                branch(increment, leaf(")")),
                body
        );
    }

    public static MyFakePsiElement makeForLoop1To10() {
        return makeForLoop(listFromString("int i = 0"), listFromString("i<10"), list(leaf("++"), leaf("i")), listFromString("a[i] = 2 * i;"));
    }


}