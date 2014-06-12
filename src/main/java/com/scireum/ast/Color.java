/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package com.scireum.ast;

import com.scireum.Generator;
import com.scireum.Scope;

/**
 * Represents a color like #565656.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/02
 */
public class Color extends Expression {

    private int r = 0;
    private int g = 0;
    private int b = 0;
    private double a = 0;

    /**
     * Sets the RGBA values of the color
     *
     * @param r the red part 0..255
     * @param g the green part 0..255
     * @param b the blue part 0..255
     * @param a the alpha part 0..1
     */
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
