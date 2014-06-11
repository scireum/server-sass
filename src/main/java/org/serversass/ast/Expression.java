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
 * Base class for all AST classes
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/02
 */
public abstract class Expression {

    /**
     * Determines if this expression is constant or if it depends on variables.
     *
     * @return <tt>true</tt> if the expression is constant, <tt>false</tt> otherwise
     */
    public abstract boolean isConstant();

    /**
     * If possible the expression is evaluated and a simplified expression is returned.
     *
     * @param scope the scope used to resolve variables.
     * @param gen   the generator used to evaluate functions
     * @return a possibly simplified version of the expression
     */
    public abstract Expression eval(Scope scope, Generator gen);

}
