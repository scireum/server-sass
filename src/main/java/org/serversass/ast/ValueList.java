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
 * Represents a list of values.
 */
public class ValueList implements Expression {
    private List<Expression> elements = new ArrayList<Expression>();
    private boolean keepCommas = false;

    /**
     * Creates a new and empty value list
     *
     * @param keepCommas determines if commas are kept in the output or not
     */
    public ValueList(boolean keepCommas) {
        this.keepCommas = keepCommas;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Expression expr : elements) {
            if (sb.length() > 0) {
                sb.append(keepCommas ? "," : " ");
            }
            sb.append(expr);
        }
        return sb.toString();
    }

    /**
     * Adds the given element to the list.
     *
     * @param element the element to add
     */
    public void add(Expression element) {
        elements.add(element);
    }

    /**
     * Returns the contents of the value list.
     *
     * @return a list of all elements in the value list
     */
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
        ValueList result = new ValueList(keepCommas);
        for (Expression expr : elements) {
            result.elements.add(expr.eval(scope, gen));
        }
        return result;
    }
}
