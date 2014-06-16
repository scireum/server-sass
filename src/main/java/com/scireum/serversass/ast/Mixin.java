/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package com.scireum.serversass.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a parsed mixin.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/02
 */
public class Mixin {

    private List<String> parameters = new ArrayList<String>();
    private List<Attribute> attributes = new ArrayList<Attribute>();
    private String name;

    /**
     * Adds a parameter of the mixin
     *
     * @param name the name of the parameter to add (without $)
     */
    public void addParameter(String name) {
        parameters.add(name);
    }

    /**
     * Adds an attribute
     *
     * @param attr the attribute to add
     */
    public void addAttribute(Attribute attr) {
        attributes.add(attr);
    }

    /**
     * Sets the name of the mixin
     *
     * @param name the name of the mixin
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the mixin
     *
     * @return the name of the mixin
     */
    public String getName() {
        return name;
    }

    /**
     * Returns all parameters of the mixin
     *
     * @return a list of parameter names of the mixin
     */
    public List<String> getParameters() {
        return parameters;
    }

    /**
     * Returns all attributes defined by the mixin
     *
     * @return a list of all defined attributes
     */
    public List<Attribute> getAttributes() {
        return attributes;
    }
}
