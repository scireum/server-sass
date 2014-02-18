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
 * Date: 18.02.14
 * Time: 14:49
 * To change this template use File | Settings | File Templates.
 */
public class Color extends Expression {

    private int r = 0;
    private int g = 0;
    private int b = 0;
    private double a = 0;

    public void setRGBA(int r, int g, int b, double a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public Expression eval(Scope scope, Generator gen) {
        return this;
    }

    @Override
    public String toString() {
        if (a != 0) {
            return "rgba(" + r + "," + g + "," + b + "," + a + ")";
        } else {
            return "#" + Integer.toHexString(r) + Integer.toHexString(g) + Integer.toHexString(b);
        }
    }
}
