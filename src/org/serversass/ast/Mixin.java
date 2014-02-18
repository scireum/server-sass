/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package org.serversass.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 13.02.14
 * Time: 22:51
 * To change this template use File | Settings | File Templates.
 */
public class Mixin {

    private List<String> parameters = new ArrayList<String>();
    private List<Attribute> attributes = new ArrayList<Attribute>();
    private String name;

    public void addParameter(String name) {
        parameters.add(name);
    }

    public void addAttribute(Attribute attr) {
        attributes.add(attr);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }
}
