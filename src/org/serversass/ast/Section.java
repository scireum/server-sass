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

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 16.02.14
 * Time: 20:09
 * To change this template use File | Settings | File Templates.
 */
public class Section {
    private List<List<String>> selectors = new ArrayList<List<String>>();
    private List<Expression> mediaQueries = new ArrayList<Expression>();
    private List<String> includes = new ArrayList<String>();
    private List<Attribute> attributes = new ArrayList<Attribute>();
    private List<Section> subSections = new ArrayList<Section>();
    private List<MixinReference> references = new ArrayList<MixinReference>();

    public List<List<String>> getSelectors() {
        return selectors;
    }

    public void addAttribute(Attribute attr) {
        attributes.add(attr);
    }

    public void addMediaQuery(Expression query) {
        mediaQueries.add(query);
    }

    public void addSubSection(Section section) {
        subSections.add(section);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getSelectorString());
        sb.append(" {\n");
        for (Attribute attr : attributes) {
            sb.append(" ");
            sb.append(attr);
            sb.append("\n");
        }
        for (Section child : subSections) {
            sb.append(child);
            sb.append("\n");
        }
        sb.append("}");

        return sb.toString();
    }

    public void addExtends(String name) {
        includes.add(name);
    }

    public void addMixinReference(MixinReference ref) {
        references.add(ref);
    }

    public List<String> getIncludes() {
        return includes;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public List<Section> getSubSections() {
        return subSections;
    }

    public List<MixinReference> getReferences() {
        return references;
    }


    public String getMediaQuery(Scope scope, Generator gen) {
        StringBuilder sb = new StringBuilder();
        for (Expression expr : mediaQueries) {
            if (sb.length() > 0) {
                sb.append(" and ");
            }
            sb.append(expr.eval(scope, gen));
        }
        return sb.toString();
    }

    public String getSelectorString() {
        StringBuilder sb = new StringBuilder();
        for (List<String> selector : selectors) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            for (String s : selector) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(s);
            }
        }
        return sb.toString();
    }
}
