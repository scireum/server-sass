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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 16.02.14
 * Time: 21:17
 * To change this template use File | Settings | File Templates.
 */
public class Number extends Expression {
    private final String value;
    private final String unit;

    private static final Pattern NUMBER = Pattern.compile("(\\d+)([a-z]+|%)");
    private static final Pattern DECIMAL_NUMBER = Pattern.compile("(\\.\\d+|\\d+\\.\\d+)([a-z]+|%)");
    private final Double numericValue;

    public Number(double numericValue, String value, String unit) {
        this.numericValue = numericValue;
        this.value = value;
        this.unit = unit;
    }

    public Number(String value) {
        numericValue = null;
        Matcher m = NUMBER.matcher(value);
        if (m.matches()) {
            this.value = m.group(1);
            this.unit = m.group(2);
        } else {
            m = DECIMAL_NUMBER.matcher(value);
            if (m.matches()) {
                this.value = m.group(1);
                this.unit = m.group(2);
            } else {
                this.value = value;
                this.unit = "";
            }
        }
    }

    public String getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public double getNumericValue() {
        if (numericValue != null) {
            return numericValue;
        }
        return Double.parseDouble(value);
    }

    @Override
    public String toString() {
        return value + unit;
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
