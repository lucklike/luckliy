package com.luckyframework.diff;

import com.luckyframework.common.ExpressionEngine;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * 组合多组比较算法形成的比较算法
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/6/4 06:51
 */
public class CombinationDiffAlgorithm<T> implements DiffAlgorithm<T> {

    private final List<Object> diffChains;

    private CombinationDiffAlgorithm(List<Object> diffChains) {
        this.diffChains = diffChains;
    }

    public List<Object> getDiffChains() {
        return diffChains;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean hasDiff(T obj1, T obj2) {
        StringBuilder result = new StringBuilder();
        for (Object operator : diffChains) {
            if (operator instanceof DiffAlgorithm) {
                result.append(((DiffAlgorithm<T>) operator).hasDiff(obj1, obj2));
            } else {
                result.append(operator);
            }
        }
        return ExpressionEngine.calculate(result.toString(), boolean.class);
    }

    @Override
    public String toString() {
        return Builder.operatorToString(diffChains);
    }

    static class Builder<T> {
        private static final String AND = " && ";
        private static final String OR = " || ";
        private static final String BRACKET_START = "(";
        private static final String BRACKET_END = ")";
        private static final Set<String> OPERATORS = new HashSet<>();

        static {
            OPERATORS.addAll(Arrays.asList(AND, OR, BRACKET_START, BRACKET_END));
        }

        private final List<Object> operatorChain = new LinkedList<>();

        public Builder<T> diffAnd(DiffAlgorithm<T> diffAlgorithm) {
            return diff(diffAlgorithm).and();
        }

        public Builder<T> andDiff(DiffAlgorithm<T> diffAlgorithm) {
            return and().diff(diffAlgorithm);
        }

        public Builder<T> diffOr(DiffAlgorithm<T> diffAlgorithm) {
            return diff(diffAlgorithm).or();
        }

        public Builder<T> orDiff(DiffAlgorithm<T> diffAlgorithm) {
            return or().diff(diffAlgorithm);
        }

        public Builder<T> diffEnd(DiffAlgorithm<T> diffAlgorithm) {
            return diff(diffAlgorithm).end();
        }

        public Builder<T> startDiff(DiffAlgorithm<T> diffAlgorithm) {
            return start().diff(diffAlgorithm);
        }

        @SafeVarargs
        public final Builder<T> ands(DiffAlgorithm<T>... diffAlgorithm) {
            if (lastOperatorIsBracketEnd() || lastOperatorIsDiff()) {
                and();
            }
            boolean lastOperatorIsBracketStart = lastOperatorIsBracketStart();
            if (!lastOperatorIsBracketStart && diffAlgorithm.length > 1) {
                start();
                lastOperatorIsBracketStart = true;
            }
            for (DiffAlgorithm<T> algorithm : diffAlgorithm) {
                diffAnd(algorithm);
            }
            removeLast();
            if (lastOperatorIsBracketStart) {
                end();
            }
            return this;
        }

        @SafeVarargs
        public final Builder<T> ors(DiffAlgorithm<T>... diffAlgorithm) {
            if (lastOperatorIsBracketEnd() || lastOperatorIsDiff()) {
                or();
            }
            boolean lastOperatorIsBracketStart = lastOperatorIsBracketStart();
            if (!lastOperatorIsBracketStart && diffAlgorithm.length > 1) {
                start();
                lastOperatorIsBracketStart = true;
            }
            for (DiffAlgorithm<T> algorithm : diffAlgorithm) {
                diffOr(algorithm);
            }
            removeLast();
            if (lastOperatorIsBracketStart) {
                end();
            }
            return this;
        }


        public Builder<T> diff(DiffAlgorithm<T> diffAlgorithm) {
            if (lastOperatorIsDiff()) {
                throw new IllegalArgumentException("The 'diff' operation cannot be used continuously, " + this);
            }
            if (lastOperatorIsBracketEnd()) {
                throw new IllegalArgumentException("Operation 'diff' cannot be performed after operation ')', " + this);
            }
            this.operatorChain.add(diffAlgorithm);
            return this;
        }

        public Builder<T> and() {
            if (lastIsNull()) {
                throw new IllegalArgumentException("The first operator cannot be 'and', " + this);
            }
            if (lastOperatorIsAnd()) {
                throw new IllegalArgumentException("The 'and' operation cannot be used continuously, " + this);
            }
            if (lastOperatorIsOr()) {
                throw new IllegalArgumentException("Operation 'and' cannot be performed after operation 'or', " + this);
            }
            if (lastOperatorIsBracketStart()) {
                throw new IllegalArgumentException("Operation 'and' cannot be performed after operation '(', " + this);
            }
            this.operatorChain.add(AND);
            return this;
        }

        public Builder<T> or() {
            if (lastIsNull()) {
                throw new IllegalArgumentException("The first operator cannot be 'or', " + this);
            }
            if (lastOperatorIsOr()) {
                throw new IllegalArgumentException("The 'or' operation cannot be used continuously, " + this);
            }
            if (lastOperatorIsAnd()) {
                throw new IllegalArgumentException("Operation 'or' cannot be performed after operation 'and', " + this);
            }
            if (lastOperatorIsBracketStart()) {
                throw new IllegalArgumentException("Operation 'or' cannot be performed after operation '(', " + this);
            }
            this.operatorChain.add(OR);
            return this;
        }

        public Builder<T> start() {
            if (lastOperatorIsDiff()) {
                throw new IllegalArgumentException("Operation '(' cannot be performed after operation 'diff', " + this);
            }
            if (lastOperatorIsBracketEnd()) {
                throw new IllegalArgumentException("Operation '(' cannot be performed after operation ')', " + this);
            }
            this.operatorChain.add(BRACKET_START);
            return this;
        }

        public Builder<T> end() {
            if (lastIsNull()) {
                throw new IllegalArgumentException("The first operator cannot be ')' " + this);
            }
            if (lastOperatorIsOr()) {
                throw new IllegalArgumentException("Operation ')' cannot be performed after operation 'or', " + this);
            }
            if (lastOperatorIsAnd()) {
                throw new IllegalArgumentException("Operation ')' cannot be performed after operation 'and', " + this);
            }
            if (lastOperatorIsBracketStart()) {
                throw new IllegalArgumentException("Operation ')' cannot be performed after operation '(', " + this);
            } else {
                this.operatorChain.add(BRACKET_END);
            }
            return this;
        }

        public CombinationDiffAlgorithm<T> build() {
            if (lastIsNull()) {
                throw new IllegalArgumentException("The operation chain is null.");
            }
            Stack<String> bracketStack = new Stack<>();
            for (Object operator : operatorChain) {
                String peek = bracketStack.isEmpty() ? null : bracketStack.peek();
                if (operator instanceof String) {
                    String operaStr = (String) operator;
                    if (BRACKET_START.equals(operaStr)) {
                        bracketStack.push(operaStr);
                    } else if (BRACKET_END.equals(operaStr)) {
                        if (BRACKET_START.equals(peek)) {
                            bracketStack.pop();
                        } else {
                            throw new IllegalArgumentException("Missing operator '(', " + this);
                        }
                    }
                }
            }
            if (!bracketStack.isEmpty()) {
                throw new IllegalArgumentException("The operator '(' did not end correctly, " + this);
            }
            if (lastOperatorIsAnd()) {
                throw new IllegalArgumentException("cannot end with the operator 'and', " + this);
            }
            if (lastOperatorIsOr()) {
                throw new IllegalArgumentException("cannot end with the operator 'or', " + this);
            }
            return new CombinationDiffAlgorithm<>(operatorChain);
        }

        private Object getLastOperator() {
            return lastIsNull() ? null : this.operatorChain.get(this.operatorChain.size() - 1);
        }

        private boolean lastOperatorIsAnd() {
            return AND.equals(getLastOperator());
        }

        private boolean lastOperatorIsOr() {
            return OR.equals(getLastOperator());
        }

        private boolean lastOperatorIsBracketStart() {
            return BRACKET_START.equals(getLastOperator());
        }

        private boolean lastOperatorIsBracketEnd() {
            return BRACKET_END.equals(getLastOperator());
        }

        private boolean lastIsOperator() {
            Object operator = getLastOperator();
            return (operator instanceof String) && OPERATORS.contains(operator);
        }

        private boolean lastIsNull() {
            return operatorChain.isEmpty();
        }

        private boolean lastOperatorIsDiff() {
            return getLastOperator() instanceof DiffAlgorithm;
        }

        private void removeLast() {
            if (!lastIsNull()) {
                operatorChain.remove(operatorChain.size() - 1);
            }
        }

        @Override
        public String toString() {
            return operatorToString(operatorChain);
        }

        public static String operatorToString(List<Object> operators) {
            StringBuilder sb = new StringBuilder("Operator: ");
            int i = 0;
            for (Object o : operators) {
                if (o instanceof String) {
                    sb.append(o);
                } else {
                    sb.append("#").append(i++);
                }
            }
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        Builder<String> builder = new Builder<>();
        DiffAlgorithm<String> algorithm = String::equals;
        CombinationDiffAlgorithm<String> diffAlgorithm = builder.diff(algorithm)
                .ands(algorithm, algorithm, algorithm)
                .and()
                .ors(algorithm, algorithm)
                .build();
        System.out.println(diffAlgorithm);
        System.out.println(diffAlgorithm.hasDiff("str", "str1"));
    }
}
