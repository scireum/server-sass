/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package org.serversass;

import org.serversass.ast.*;
import parsii.tokenizer.ParseException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 17.02.14
 * Time: 15:34
 * To change this template use File | Settings | File Templates.
 */
public class Generator {

    private Set<String> importedSheets = new TreeSet<String>();
    private List<Section> sections = new ArrayList<Section>();
    private Map<String, Section> extensibleSections = new HashMap<String, Section>();
    private Map<String, Section> mediaQueries = new HashMap<String, Section>();
    private Map<String, Mixin> mixins = new HashMap<String, Mixin>();
    private Scope scope = new Scope();

    public void warn(String message) {
        System.out.println(message);
    }

    public void debug(String message) {

    }

    protected Stylesheet resolve(String sheet) {
        try {
            InputStream is = getClass().getResourceAsStream((sheet.startsWith("/") ? "" : "/") + sheet + (sheet.endsWith(
                    ".scss") ? "" : ".scss"));
            if (is == null) {
                is = getClass().getResourceAsStream((sheet.startsWith("/") ? "" : "/") + "_" + sheet + (sheet.endsWith(
                        ".scss") ? "" : ".scss"));
                if (is == null) {
                    warn("Cannot resolve '" + sheet + "'. Skipping import.");
                    return null;
                }
            }
            try {
                Parser p = new Parser(sheet, new InputStreamReader(is));
                return p.compile();
            } finally {
                is.close();
            }
        } catch (ParseException e) {
            warn(String.format("Error parsing: %s%n%s", sheet, e.toString()));
        } catch (Throwable e) {
            warn(String.format("Error importing: %s: %s (%s)", sheet, e.getMessage(), e.getClass().getName()));
        }
        return null;
    }

    public void importStylesheet(String sheet) {
        if (importedSheets.contains(sheet)) {
            return;
        }
        importStylesheet(resolve(sheet));
    }

    public void importStylesheet(Stylesheet sheet) {
        if (sheet == null) {
            return;
        }
        if (importedSheets.contains(sheet.getName())) {
            return;
        }
        importedSheets.add(sheet.getName());
        for (String imp : sheet.getImports()) {
            importStylesheet(imp);
        }
        for (Mixin mix : sheet.getMixins()) {
            mixins.put(mix.getName(), mix);
        }
        for (Variable var : sheet.getVariables()) {
            if (!scope.has(var.getName()) || !var.isDefaultValue()) {
                scope.set(var.getName(), var.getValue());
            } else {
                debug("Skipping redundant variable definition: '" + var + "'");
            }
        }
        for (Section section : sheet.getSections()) {
            List<Section> stack = new ArrayList<Section>();
            expand(null, section, stack);
        }
    }

    private void expand(String mediaQueryPath, Section section, List<Section> stack) {
        stack = new ArrayList<Section>(stack);
        if (!section.getSelectors().isEmpty()) {
            if (mediaQueryPath == null) {
                sections.add(section);
            } else {
                addResultSection(mediaQueryPath, section);
            }
            for (List<String> selector : section.getSelectors()) {
                for (int i = stack.size() - 1; i >= 0; i--) {
                    Section parent = stack.get(i);
                    if (!parent.getSelectors().isEmpty()) {
                        selector.addAll(0, parent.getSelectors().get(0));
                    }
                }
                if (selector.size() == 1) {
                    extensibleSections.put(selector.get(0), section);
                }
            }
            stack.add(section);
        } else {
            if (mediaQueryPath == null) {
                mediaQueryPath = "@media " + section.getMediaQuery(scope, this);
            } else {
                mediaQueryPath += " and " + section.getMediaQuery(scope, this);
            }
            if (!section.getAttributes().isEmpty()) {
                Section copy = new Section();
                for (int i = stack.size() - 1; i >= 0; i--) {
                    Section parent = stack.get(i);
                    if (copy.getSelectors().isEmpty()) {
                        copy.getSelectors().addAll(parent.getSelectors());
                    } else if (!parent.getSelectors().isEmpty()) {
                        for (List<String> selector : copy.getSelectors()) {
                            selector.addAll(0, parent.getSelectors().get(0));
                        }
                    }
                }
                if (copy.getSelectors().isEmpty()) {
                    warn(String.format("Cannot define attributes in @media selector '%s'",
                                       section.getMediaQuery(scope, this)));
                } else {
                    copy.getAttributes().addAll(section.getAttributes());
                    addResultSection(mediaQueryPath, copy);
                }
            }
        }

        for (Section child : section.getSubSections()) {
            expand(mediaQueryPath, child, stack);
        }
        section.getSubSections().clear();
    }

    private void addResultSection(String mediaQueryPath, Section section) {
        Section qry = mediaQueries.get(mediaQueryPath);
        if (qry == null) {
            qry = new Section();
            qry.getSelectors().add(Collections.singletonList(mediaQueryPath));
            mediaQueries.put(mediaQueryPath, qry);
        }
        qry.addSubSection(section);
    }

    public void compile() {
        sections.addAll(mediaQueries.values());
        for (Section section : sections) {
            for (String extend : section.getIncludes()) {
                Section toBeExtended = extensibleSections.get(extend);
                if (toBeExtended != null) {
                    toBeExtended.getSelectors().addAll(section.getSelectors());
                } else {
                    warn(String.format("Skipping unknown @extend '%s' referenced by selector '%s'",
                                       extend,
                                       section.getSelectorString()));
                }
            }
            for (MixinReference ref : section.getReferences()) {
                Scope subScope = new Scope(scope);
                Mixin mixin = mixins.get(ref.getName());
                if (mixin != null) {
                    if (mixin.getParameters().size() != ref.getParameters().size()) {
                        warn(String.format(
                                "@mixin call '%s' by selector '%s' does not match expected number of parameters. Found: %d, expected: %d",
                                ref.getName(),
                                section.getSelectorString(),
                                ref.getParameters().size(),
                                mixin.getParameters().size()));
                    }
                    int i = 0;
                    for (String name : mixin.getParameters()) {
                        if (ref.getParameters().size() > i) {
                            subScope.set(name, ref.getParameters().get(i));
                        }
                        i++;
                    }
                    for (Attribute attr : mixin.getAttributes()) {
                        if (attr.getExpression().isConstant()) {
                            section.addAttribute(attr);
                        } else {
                            Attribute copy = new Attribute(attr.getName());
                            copy.setExpression(attr.getExpression().eval(subScope, this));
                            section.addAttribute(copy);
                        }
                    }
                } else {
                    warn(String.format("Skipping unknown @mixin '%s' referenced by selector '%s'",
                                       ref.getName(),
                                       section.getSelectorString()));
                }
            }
            for (Attribute attr : section.getAttributes()) {
                attr.setExpression(attr.getExpression().eval(scope, this));
            }
        }
        Iterator<Section> iter = sections.iterator();
        while (iter.hasNext()) {
            Section section = iter.next();
            if (section.getSubSections().isEmpty() && section.getAttributes().isEmpty()) {
                iter.remove();
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Section section : sections) {
            sb.append(section);
            sb.append("\n");
        }

        return sb.toString();
    }

    public Expression evaluateFunction(FunctionCall call) {
        return new Value(call.toString());
    }
}
