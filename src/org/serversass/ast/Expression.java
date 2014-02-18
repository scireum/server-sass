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
 * Date: 13.02.14
 * Time: 22:49
 * To change this template use File | Settings | File Templates.
 */
public abstract class Expression {

    public abstract boolean isConstant();

    public abstract Expression eval(Scope scope, Generator gen);

}
