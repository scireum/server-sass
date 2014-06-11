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
 * Represents a plain value.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/02
 */
public class Value extends Expression {
    private String contents;

    /**
     * Creates a new value representing the given contents a value.
     *
     * @param contents the value to be represented
     */
    public Value(String contents) {
        super();
        this.contents = contents;
    }

    /**
     * Returns the represented value
     *
     * @return the value to represent
     */
    public String getContents() {
        return contents;
    }

    @Override
    public String toString() {
        return contents;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public Expression eval(Scope scope, Generator gen) {
        return this;
    }
}
