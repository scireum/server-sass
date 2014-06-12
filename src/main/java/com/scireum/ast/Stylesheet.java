/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package com.scireum.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 13.02.14
 * Time: 22:22
 * To change this template use File | Settings | File Templates.
 */
public class Stylesheet {

    private String name;
    private List<Variable> variables = new ArrayList<Variable>();
    private List<Mixin> mixins = new ArrayList<Mixin>();
    private List<Section> sections = new ArrayList<Section>();
    private List<String> imports = new ArrayList<String>();

    public Stylesheet(String name) {
        this.name = name;

    }

    public void addImport(String name) {
        imports.add(name);
    }

    public void addVariable(Variable variable) {
        variables.add(variable);
    }

    public void addSection(Section section) {
        sections.add(section);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Variable var : variables) {
            sb.append(var);
            sb.append(";\n");
        }
        for (Section s : sections) {
            sb.append("\n");
            sb.append(s);
        }
        return sb.toString();
    }

    public void addMixin(Mixin mixin) {
        mixins.add(mixin);
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public List<Mixin> getMixins() {
        return mixins;
    }

    public List<Section> getSections() {
        return sections;
    }

    public List<String> getImports() {
        return imports;
    }

    public String getName() {
        return name;
    }
}
