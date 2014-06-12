/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package com.scireum.ast;

import com.scireum.Generator;
import com.scireum.Scope;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a function call like "lighten(#FFFFF, 1)".
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/02
 */
public class FunctionCall extends Expression {

    private String name;
    private List<Expression> parameters = new ArrayList<Expression>();

    /**
     * Creates a new and empty function call
     */
    public FunctionCall() {
    }

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
        return sb.append(")").toString();
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
