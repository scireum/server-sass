/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package org.serversass.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 16.02.14
 * Time: 21:17
 * To change this template use File | Settings | File Templates.
 */
public class MixinReference {
    private String name;
    private List<Expression> parameters = new ArrayList<Expression>();

    public MixinReference() {
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("@include ");
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

    public void setName(String name) {
        this.name = name;
    }

    public void addParameter(Expression expression) {
        parameters.add(expression);
    }

    public List<Expression> getParameters() {
        return parameters;
    }
}
