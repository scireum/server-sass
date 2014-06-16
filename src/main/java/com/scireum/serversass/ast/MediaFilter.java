/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package com.scireum.serversass.ast;

import com.scireum.serversass.Generator;
import com.scireum.serversass.Scope;

/**
 * Represents an attribute filter used in a media query like "(min-width: 13px)".
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/02
 */
public class MediaFilter extends Expression {
    private String name;
    private Expression expression;

    /**
     * Creates a new media filter for the given attribute (without ":").
     *
     * @param name the name of the attribute to filter on
     */
    public MediaFilter(String name) {
        this.name = name;
    }

    /**
     * Sets the filter expression
     *
     * @param expression the filter expression to set
     */
    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    /**
     * Returns the filter expression.
     *
     * @return the filter expression previously set
     */
    public Expression getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return "(" + name + ": " + expression + ")";
    }

    /**
     * Returns the name of the attribute being filtered on
     *
     * @return the name of the filtered attribute
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean isConstant() {
        return expression.isConstant();
    }

    @Override
    public Expression eval(Scope scope, Generator gen) {
        MediaFilter result = new MediaFilter(name);
        result.setExpression(expression.eval(scope, gen));
        return result;
    }
}
