/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package org.serversass;

import org.serversass.ast.Expression;
import org.serversass.ast.Value;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 17.02.14
 * Time: 16:03
 * To change this template use File | Settings | File Templates.
 */
public class Scope {
    private Scope parent;
    private Map<String, Expression> variables = new TreeMap<String, Expression>();

    public Scope() {

    }
    public Scope(Scope scope) {
        this.parent = scope;
    }

    public void set(String name, Expression value) {
        variables.put(name, value);
    }

    public Expression get(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        if (parent == null) {
            return new Value("");
        }
        return parent.get(name);
    }

    public boolean has(String name) {
        return variables.containsKey(name);
    }
}
