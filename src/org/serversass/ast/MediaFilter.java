/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package org.serversass.ast;

import org.serversass.Generator;
import org.serversass.Scope;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 16.02.14
 * Time: 20:31
 * To change this template use File | Settings | File Templates.
 */
public class MediaFilter extends Expression {
    private String name;
    private Expression expression;

    public MediaFilter(String name) {
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
        return "(" + name + ": " + expression + ")";
    }

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
