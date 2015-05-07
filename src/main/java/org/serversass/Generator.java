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

import java.io.*;
import java.util.*;

/**
 * Generates CSS code from one or more SASS stylesheets.
 * <p>
 * A subclass can be created to override {@link #resolve(String)} to change the way SASS files are
 * resolved (The default is to use the classpath). Also {@link #warn(String)} and {@link #debug(String)} can be
 * overridden to process messages which are generated while processing the code.
 * </p>
 * <p>
 * The resulting css code can be obtained by calling the {@link #toString()} method.
 * </p>
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/02
 */
public class Generator {

    /*
     * Prevents circular imports
     */
    private Set<String> importedSheets = new TreeSet<String>();

    /*
     * Contains all sections which will be part of the output
     */
    private List<Section> sections = new ArrayList<Section>();

    /*
     * Contains all sections which can be referenced via @extend
     */
    private Map<String, Section> extensibleSections = new HashMap<String, Section>();

    /*
     * Contains all media queries
     */
    private Map<String, Section> mediaQueries = new LinkedHashMap<String, Section>();

    /*
     * Contains all known mixins
     */
    private Map<String, Mixin> mixins = new HashMap<String, Mixin>();

    /*
     * Contains the evaluation context (all variables)
     */
    private Scope scope = new Scope();

    private File baseDir;

    /**
     * Generates a new Generator without a directory used for lookups.
     * <p>
     * This generator will resolve all imports using the classpath.
     * </p>
     */
    public Generator() {

    }

    /**
     * Generates a new Generator using the given directory for lookups.
     * <p>
     * This generator will resolve all imports using the given directory or the  classpath.
     * </p>
     *
     * @param baseDir the directory with contains the imports
     */
    public Generator(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Called to signal a warning, like an invalid operation or parse errors in a source file.
     * <p>
     * By default all messages are discarded.
     * </p>
     *
     * @param message the message which is reported
     */
    public void warn(String message) {
    }

    /**
     * Contains a message which might be helpful in development systems but are generally not of great
     * interest in production systems.
     *
     * @param message the message which is reported
     */
    public void debug(String message) {

    }

    /**
     * Resolves a given name into a template.
     * <p>
     * By default the classloader is used to resolve the template. Also .scss or _ are added as post-/prefix
     * if required.
     * </p>
     *
     * @param sheet the name of the file to resolve
     * @return the resolved stylesheet or <tt>null</tt> if the file is not found
     */
    protected Stylesheet resolve(String sheet) {
        try {
            sheet = sheet.replace("\\", "/");
            if (!sheet.endsWith(".scss")) {
                sheet += ".scss";
            }

            InputStream is = resolveIntoStream(sheet);
            if (is == null) {
                warn("Cannot resolve '" + sheet + "'. Skipping import.");
                return null;
            }
            try {
                Parser p = new Parser(sheet, new InputStreamReader(is));
                return p.parse();
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

    /**
     * Resolves the given file name into an {@link InputStream}
     *
     * @param sheet the file to resolve (already cleaned up by replacing \ with / and appending .scss if necessary).
     * @return an InputStream for the resolved data or <tt>null</tt> to indicate that the resource cannot be found
     * @throws IOException in case of an error while resolving or reading the contents
     */
    protected InputStream resolveIntoStream(String sheet) throws IOException {
        if (baseDir != null) {
            File file = baseDir;
            for (String part : sheet.split("/")) {
                file = new File(file, part);
            }
            if (file.exists() && file.isFile()) {
                return new FileInputStream(file);
            } else {
                return null;
            }
        } else {
            InputStream is = getClass().getResourceAsStream((sheet.startsWith("/") ? "" : "/") + sheet);
            if (is == null) {
                is = getClass().getResourceAsStream((sheet.startsWith("/") ? "" : "/") + "_" + sheet);
            }
            return is;
        }
    }

    /**
     * Instructs the generator to include a scss file. This is also used to load the initial file.
     *
     * @param sheet the scss file to load.
     */
    public void importStylesheet(String sheet) {
        if (importedSheets.contains(sheet)) {
            return;
        }
        importStylesheet(resolve(sheet));
    }

    /**
     * Imports an already parsed stylesheet.
     *
     * @param sheet the stylesheet to import
     */
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

    /*
     * Expands nested sections / media queries into a flat structure as expected by CSS
     */
    private void expand(String mediaQueryPath, Section section, List<Section> stack) {
        stack = new ArrayList<Section>(stack);
        if (!section.getSelectors().isEmpty()) {
            // We have selectors -> we're a normal section no a media query
            if (mediaQueryPath == null) {
                // Add to output
                sections.add(section);
            } else {
                // We're already inside a media query, add to the appropriate result section
                addResultSection(mediaQueryPath, section);
            }
            // Expand all selectors with those of the parents (flatten nesting)
            for (List<String> selector : section.getSelectors()) {
                for (int i = stack.size() - 1; i >= 0; i--) {
                    Section parent = stack.get(i);
                    if (!parent.getSelectors().isEmpty()) {
                        selector.addAll(0, parent.getSelectors().get(0));
                    }
                }
                // Selectors with only one element can be referenced by @extend
                if (selector.size() == 1) {
                    extensibleSections.put(selector.get(0), section);
                }
            }
            // Add to nesting stack used by children
            stack.add(section);
        } else {
            // We're a media query - update path
            if (mediaQueryPath == null) {
                mediaQueryPath = "@media " + section.getMediaQuery(scope, this);
            } else {
                mediaQueryPath += " and " + section.getMediaQuery(scope, this);
            }

            // We have implicit attributes - copy the next non-media-query parent
            // and create a pseudo-secion covering these attributes
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

        // Unfold subsections
        for (Section child : section.getSubSections()) {
            expand(mediaQueryPath, child, stack);
        }

        // Delete subsections - no longer necessary (and not supported by css)
        section.getSubSections().clear();
    }

    /*
     * Adds a section to the given media query section - creates if necessary
     */
    private void addResultSection(String mediaQueryPath, Section section) {
        Section qry = mediaQueries.get(mediaQueryPath);
        if (qry == null) {
            qry = new Section();
            qry.getSelectors().add(Collections.singletonList(mediaQueryPath));
            mediaQueries.put(mediaQueryPath, qry);
        }
        qry.addSubSection(section);
    }

    /**
     * Compiles the parsed sources.
     * <p>
     * This will evaluate all @mixin and @extends statements and evaluate all expressions. Needs to be called before
     * the sources are retrieved via {@link #toString()}.
     * </p>
     */
    public void compile() {
        // Treat media queries as "normal" sections as they are supported by CSS
        sections.addAll(mediaQueries.values());
        for (Section section : sections) {
            // Handle and perform all @extend instructions
            for (String extend : section.getExtendedSections()) {
                Section toBeExtended = extensibleSections.get(extend);
                if (toBeExtended != null) {
                    toBeExtended.getSelectors().addAll(section.getSelectors());
                } else {
                    warn(String.format("Skipping unknown @extend '%s' referenced by selector '%s'",
                                       extend,
                                       section.getSelectorString()));
                }
            }

            // Handle and perform all @mixin instructions
            for (MixinReference ref : section.getReferences()) {
                // Create a sub scope which will have access to the parameter values
                Scope subScope = new Scope(scope);
                // Find mixin..
                Mixin mixin = mixins.get(ref.getName());
                if (mixin != null) {
                    // Check if number of parameters match
                    if (mixin.getParameters().size() != ref.getParameters().size()) {
                        warn(String.format(
                                "@mixin call '%s' by selector '%s' does not match expected number of parameters. Found: %d, expected: %d",
                                ref.getName(),
                                section.getSelectorString(),
                                ref.getParameters().size(),
                                mixin.getParameters().size()));
                    }
                    // Evaluate all parameters and populate sub scope
                    int i = 0;
                    for (String name : mixin.getParameters()) {
                        if (ref.getParameters().size() > i) {
                            subScope.set(name, ref.getParameters().get(i));
                        }
                        i++;
                    }
                    // Copy attributes and evaluate expression
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

            // Evaluate expressions of the section
            for (Attribute attr : section.getAttributes()) {
                attr.setExpression(attr.getExpression().eval(scope, this));
            }
        }

        // Delete empty selectors
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

    /**
     * Generates the final output into the given parameter.
     *
     * @param out the target for the generated output
     * @throws IOException in case of an io error in the underlying writer
     */
    public void generate(Output out) throws IOException {
        for (Section section : sections) {
            section.generate(out);
            out.lineBreak();
            out.optionalLineBreak();
        }
    }

    /**
     * Evaluates the given function.
     * <p>
     * Can be overridden by subclasses. If no matching function is found, the raw sources are output to handle
     * <code>url('..')</code> etc.
     * </p>
     *
     * @param call the function to evaluate
     * @return the result of the evaluation
     */
    public Expression evaluateFunction(FunctionCall call) {
        return new Value(call.toString());
    }
}
