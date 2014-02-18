/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package org.serversass.ast;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 13.02.14
 * Time: 22:51
 * To change this template use File | Settings | File Templates.
 */
public class Variable {

    private Expression value;
    private boolean defaultValue = false;
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Expression getValue() {
        return value;
    }

    public void setValue(Expression value) {
        this.value = value;
    }

    public boolean isDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString() {
        return name + ": " + value + (defaultValue ? " !default" : "");
    }
}
