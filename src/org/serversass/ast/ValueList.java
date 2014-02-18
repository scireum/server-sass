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

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 16.02.14
 * Time: 21:17
 * To change this template use File | Settings | File Templates.
 */
public class ValueList extends Expression {
    private List<Expression> elements = new ArrayList<Expression>();

    public ValueList() {
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Expression expr : elements) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(expr);
        }
        return sb.toString();
    }

    public void add(Expression element) {
        elements.add(element);
    }

    public List<Expression> getElements() {
        return elements;
    }

    @Override
    public boolean isConstant() {
        for (Expression expr : elements) {
            if (!expr.isConstant()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Expression eval(Scope scope, Generator gen) {
        ValueList result = new ValueList();
        for (Expression expr : elements) {
            result.elements.add(expr.eval(scope, gen));
        }
        return result;
    }
}
