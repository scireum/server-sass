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
 * References a variable like "$test".
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/02
 */
public class VariableReference extends Expression {
    private String name;

    /**
     * Creates a new reference for the given variable.
     *
     * @param name the name of the variable to reference (without $).
     */
    public VariableReference(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the variable.
     *
     * @return the name of the variable (without $)
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "$" + name;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public Expression eval(Scope scope, Generator gen) {
        return scope.get(name).eval(scope, gen);
    }
}
