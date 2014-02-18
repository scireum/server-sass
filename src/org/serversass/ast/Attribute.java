/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package org.serversass.ast;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 16.02.14
 * Time: 20:31
 * To change this template use File | Settings | File Templates.
 */
public class Attribute {
    private String name;
    private Expression expression;

    public Attribute(String name) {
        this.name = name;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return name+": "+expression+";";
    }

    public String getName() {
        return name;
    }
}
