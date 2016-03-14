/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package com.glide.scss.compiler.ast;

import com.glide.scss.compiler.Generator;
import com.glide.scss.compiler.Scope;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a function call like "lighten(#FFFFF, 1)".
 */
public class FunctionCall implements Expression {

    private String name;
    private List<Expression> parameters = new ArrayList<>();

    /**
     * Returns the name of the function.
     *
     * @return the name of the function
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendNameAndParameters(sb, name, parameters);
        return sb.toString();
    }

    /**
     * Appends the name and parameters to the given string builder.
     *
     * @param sb         the target to write the output to
     * @param name       the name of the function
     * @param parameters the list of parameters
     */
    protected static void appendNameAndParameters(StringBuilder sb, String name, List<Expression> parameters) {
        sb.append(name);
        sb.append("(");
        boolean first = true;
        for (Expression expr : parameters) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(expr);
        }
        sb.append(")");
    }

    /**
     * Sets the name of the function
     *
     * @param name the name of the function
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Adds the given expression as parameter
     *
     * @param expression the parameter to add
     */
    public void addParameter(Expression expression) {
        parameters.add(expression);
    }

    /**
     * Returns all parameters of the function
     *
     * @return a list of all parameters
     */
    public List<Expression> getParameters() {
        return parameters;
    }

    /**
     * Returns the parameter at the expected index.
     *
     * @param index the number of the parameter to access
     * @return the expression representing at the given index
     * @throws IllegalArgumentException if the index is out of bounds
     */
    public Expression getExpectedParam(int index) {
        if (parameters.size() <= index) {
            throw new IllegalArgumentException("Parameter index out of bounds: " + index + ". Function call: " + this);
        }

        return parameters.get(index);
    }

    /**
     * Returns the parameter casted as a {@link CSSColor} at the expected index.
     *
     * @param index the number of the parameter to access
     * @return the expression representing at the given index
     * @throws IllegalArgumentException if the index is out of bounds or if the parameter isn't a color
     */
    public CSSColor getExpectedColorParam(int index) {
        Expression expr = getExpectedParam(index);
        if (!(expr instanceof CSSColor)) {
            throw new IllegalArgumentException("Parameter " + index + " isn't a color. Function call: " + this);
        }

        return (CSSColor) expr;
    }

    /**
     * Returns the parameter at the expected index converted to an int.
     *
     * @param index the number of the parameter to access
     * @return the expression representing at the given index
     * @throws IllegalArgumentException if the index is out of bounds or not a valid integer
     */
    public int getExpectedIntParam(int index) {
        return Integer.parseInt(getExpectedParam(index).toString().replace("%", ""));
    }

    /**
     * Returns the parameter at the expected index converted to an float.
     *
     * @param index the number of the parameter to access
     * @return the expression representing at the given index
     * @throws IllegalArgumentException if the index is out of bounds or not a valid decimal number
     */
    public float getExpectedFloatParam(int index) {
        return Float.parseFloat(getExpectedParam(index).toString());
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Expression eval(Scope scope, Generator gen) {
        FunctionCall call = new FunctionCall();
        call.setName(name);
        for (Expression expr : parameters) {
            call.addParameter(expr.eval(scope, gen));
        }
        return gen.evaluateFunction(call);
    }
}
