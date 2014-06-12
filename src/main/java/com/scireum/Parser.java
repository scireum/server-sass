/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package com.scireum;

import com.scireum.ast.*;
import parsii.tokenizer.Char;
import parsii.tokenizer.ParseException;
import parsii.tokenizer.Token;
import parsii.tokenizer.Tokenizer;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a given SASS source into a {@link Stylesheet}.
 *
 * @author Andreas Haufler (aha@scireum.de)
 * @since 2014/02
 */
public class Parser {

    /**
     * How to put that right: CSS is kind of "special gifted" - so tokenization is not always that straightforward.
     * <p>
     * Therefore we subclass the tokenizer to handle css selectors etc. right here in the tokenizer.
     * </p>
     */
    class SassTokenizer extends Tokenizer {

        public SassTokenizer(Reader input) {
            super(input);
            setLineComment("//");
            setBlockCommentStart("/*");
            setBlockCommentEnd("*/");
            addSpecialIdStarter('@');
            addSpecialIdStarter('$');
            addSpecialIdStarter('#');
            addKeyword("import");
            addKeyword("mixin");
            addKeyword("include");
            addKeyword("extend");
            addKeyword("media");
            addStringDelimiter('\'', '\'');
        }

        @Override
        protected Token fetchNumber() {
            Token token = super.fetchNumber();
            // If a number is immediately followed by % or a text like "px" - this belongs to the numeric token.
            if (input.current().is('%')) {
                token.addToContent(input.consume());
                return token;
            }
            while (input.current().isLetter()) {
                token.addToContent(input.consume());
            }

            return token;
        }

        @Override
        protected boolean handleStringEscape(char separator, char escapeChar, Token stringToken) {
            // All escaped characters will be kept in original form...
            stringToken.addToContent(escapeChar);
            stringToken.addToContent(input.consume());
            return true;
        }

        @Override
        protected boolean isAtBracket(boolean inSymbol) {
            // Treat % as single symbol so that 10%; is not tokenized to
            // "10", "%;" but to "10", "%", ";"
            // The title of this method might be a bit misleading
            return super.isAtBracket(inSymbol) || input.current().is('%');
        }

        @Override
        protected boolean isAtStartOfIdentifier() {
            if (super.isAtStartOfIdentifier()) {
                return true;
            }
            // Support vendor specific and class selectors like -moz-border-radius or .test
            if ((input.current().is('-') || input.current().is(':') || input.current().is('.')) && input.next()
                                                                                                        .isLetter()) {
                return true;
            }

            return false;
        }

        @Override
        protected boolean isIdentifierChar(Char current) {
            if (super.isIdentifierChar(current)) {
                return true;
            }
            // CSS selectors can contain "-", "." or "#" as long as it is not the last character of the token
            if ((current.is('-') || current.is('.') || current.is('#')) && !input.next().isWhitepace()) {
                return true;
            }

            return false;
        }
    }

    private final SassTokenizer tokenizer;
    private Stylesheet result;

    /**
     * Creates a new tokenizer parsing the input with the given name.
     *
     * @param name  name of the file being parsed
     * @param input the data to parse
     */
    public Parser(String name, Reader input) {
        tokenizer = new SassTokenizer(input);
        result = new Stylesheet(name);
    }

    /**
     * Parses the given input returning the parsed stylesheet.
     *
     * @return the AST representation of the parsed input
     * @throws ParseException if one or more problems occurred while parsing
     */
    public Stylesheet parse() throws ParseException {
        while (tokenizer.more()) {
            if (tokenizer.current().isKeyword("import")) {
                // Handle @import
                parseImport();
            } else if (tokenizer.current().isKeyword("mixin")) {
                // Handle @mixin
                Mixin mixin = parseMixin();
                if (mixin.getName() != null) {
                    result.addMixin(mixin);
                }
            } else if (tokenizer.current().isKeyword("media")) {
                // Handle @media
                result.addSection(parseSection(true));
            } else if (tokenizer.current().isSpecialIdentifier("$") && tokenizer.next().isSymbol(":")) {
                // Handle variable definition
                parseVariableDeclaration();
            } else {
                // Everything else is a "normal" section  with selectors and attributes
                result.addSection(parseSection(false));
            }
        }

        // Something went wrong? Throw an exception
        if (!tokenizer.getProblemCollector().isEmpty()) {
            throw ParseException.create(tokenizer.getProblemCollector());
        }

        return result;
    }

    /**
     * Parses a "section" which is either a media query or a css selector along with a set of attributes.
     *
     * @param mediaQuery determines if we're about to parse a media query or a "normal" section
     * @return the parsed section
     */
    private Section parseSection(boolean mediaQuery) {
        Section result = new Section();
        if (mediaQuery) {
            // Parse a media query like @media screen and (min-width: 1200px)
            tokenizer.consumeExpectedKeyword("media");
            while (true) {
                if (tokenizer.current().isIdentifier()) {
                    // Handle plain identifiers like "screen" or "print"
                    result.addMediaQuery(new Value(tokenizer.consume().getContents()));
                } else if (tokenizer.current().isSymbol("(")) {
                    // Handle filters like (orientation: landscape)
                    tokenizer.consumeExpectedSymbol("(");
                    if (tokenizer.current().isIdentifier() && tokenizer.next().isSymbol(":")) {
                        MediaFilter attr = new MediaFilter(tokenizer.consume().getContents());
                        tokenizer.consumeExpectedSymbol(":");
                        attr.setExpression(parseExpression(true));
                        result.addMediaQuery(attr);
                    } else {
                        tokenizer.addError(tokenizer.current(),
                                           "Unexpected symbol: '%s'. Expected an attribute filter.",
                                           tokenizer.current().getSource());
                    }
                    tokenizer.consumeExpectedSymbol(")");
                } else {
                    break;
                }
                // We only handle "and" as conjunction between two filters
                if (!tokenizer.current().isIdentifier("and")) {
                    break;
                } else {
                    tokenizer.consume();
                }
            }
        } else {
            // Parse selectors like "b div.test"
            while (tokenizer.more()) {
                List<String> selector = parseSelector();
                result.getSelectors().add(selector);
                // If another selector is given, swallow the "," and parse the next selector, else we're done.
                if (!tokenizer.current().isSymbol(",")) {
                    break;
                } else {
                    tokenizer.consumeExpectedSymbol(",");
                }
            }
        }
        tokenizer.consumeExpectedSymbol("{");
        while (tokenizer.more()) {
            if (tokenizer.current().isSymbol("}")) {
                tokenizer.consumeExpectedSymbol("}");
                return result;
            }
            // Parse "normal" attributes like "font-weight: bold;"
            if (tokenizer.current().isIdentifier() && tokenizer.next().isSymbol(":")) {
                Attribute attr = new Attribute(tokenizer.consume().getContents());
                tokenizer.consumeExpectedSymbol(":");
                attr.setExpression(parseExpression(true));
                result.addAttribute(attr);

                if (tokenizer.current().isSymbol(";") || !tokenizer.next().isSymbol("}")) {
                    tokenizer.consumeExpectedSymbol(";");
                }
            } else if (tokenizer.current().isKeyword("media")) {
                // Take care of @media sub sections
                result.addSubSection(parseSection(true));
            } else if (tokenizer.current().isKeyword("include")) {
                // Take care of included mixins like "@include border(15px);"
                tokenizer.consumeExpectedKeyword("include");
                MixinReference ref = new MixinReference();
                if (tokenizer.current().isIdentifier()) {
                    ref.setName(tokenizer.consume().getContents());
                } else {
                    tokenizer.addError(tokenizer.current(),
                                       "Unexpected token: '" + tokenizer.current()
                                                                        .getSource() + "'. Expected a mixin to use");
                }
                if (tokenizer.current().isSymbol("(")) {
                    tokenizer.consumeExpectedSymbol("(");
                    // Parse parameters - be as error tolerant as possible
                    while (tokenizer.more() && !tokenizer.current().isSymbol(")", ";", "{", "}")) {
                        ref.addParameter(parseExpression(false));
                        if (tokenizer.current().isSymbol(",")) {
                            tokenizer.consumeExpectedSymbol(",");
                        } else if (!tokenizer.current().isSymbol(")")) {
                            tokenizer.addError(tokenizer.current(),
                                               "Unexpected token: '" + tokenizer.consume()
                                                                                .getSource() + "'. Expected a comma between the parameters.");
                        }
                    }
                    tokenizer.consumeExpectedSymbol(")");
                }
                if (tokenizer.current().isSymbol(";") || !tokenizer.next().isSymbol("}")) {
                    tokenizer.consumeExpectedSymbol(";");
                }
                if (ref.getName() != null) {
                    result.addMixinReference(ref);
                }
            } else if (tokenizer.current().isKeyword("extend")) {
                // Parse @extend instructions like "@extend .warning"
                tokenizer.consumeExpectedKeyword("extend");
                if (tokenizer.current().isIdentifier() || tokenizer.current().isSpecialIdentifier("#")) {
                    result.addExtends(tokenizer.consume().getSource());
                } else {
                    tokenizer.addError(tokenizer.current(),
                                       "Unexpected token: '" + tokenizer.current()
                                                                        .getSource() + "'. Expected a selector to include.");
                }
                if (tokenizer.current().isSymbol(";") || !tokenizer.next().isSymbol("}")) {
                    tokenizer.consumeExpectedSymbol(";");
                }
            } else {
                // If it is neither an attribute, nor a media query or instruction - it is probably a sub section...
                result.addSubSection(parseSection(false));
            }
        }
        tokenizer.consumeExpectedSymbol("}");
        return result;
    }

    /*
     * CSS and therefore als SASS supports a wide range or complex selector strings. The following method
     * parses such selectors while performing basic consistency checks
     */
    private List<String> parseSelector() {
        List<String> selector = new ArrayList<String>();
        boolean lastWasId = false;
        while (tokenizer.more()) {
            if (tokenizer.current().isSymbol("{", ",")) {
                if (selector.isEmpty()) {
                    tokenizer.addError(tokenizer.current(), "Unexpected end of CSS selector");
                }
                return selector;
            } else if (tokenizer.current().isIdentifier() || tokenizer.current().isSpecialIdentifier("#")) {
                StringBuilder sb = new StringBuilder(tokenizer.consume().getSource());
                if (tokenizer.current().isSymbol(":") && tokenizer.next().isIdentifier()) {
                    sb.append(tokenizer.consume());
                    sb.append(tokenizer.consume());
                }
                while (tokenizer.current().isSymbol("[")) {
                    sb.append(tokenizer.consume().getContents());
                    if (!tokenizer.current().isSymbol("]")) {
                        if (!tokenizer.current().isIdentifier()) {
                            tokenizer.addError(tokenizer.current(),
                                               "Unexpected token: '%s'. Expected an attribute name.",
                                               tokenizer.current().getSource());
                        }
                        sb.append(tokenizer.consume().getContents());
                    }
                    if (!tokenizer.current().isSymbol("]")) {
                        if (!tokenizer.current().isSymbol("=", "~=", "|=", "^=", "$=", "*=")) {
                            tokenizer.addError(tokenizer.current(),
                                               "Unexpected token: '%s'. Expected an operation.",
                                               tokenizer.current().getSource());
                        }
                        sb.append(tokenizer.consume().getTrigger());
                    }
                    if (!tokenizer.current().isSymbol("]")) {
                        sb.append(tokenizer.consume().getSource());
                    }
                    if (!tokenizer.current().isSymbol("]")) {
                        tokenizer.addError(tokenizer.current(),
                                           "Unexpected token: '%s'. Expected: ']'",
                                           tokenizer.current().getSource());
                    } else {
                        sb.append(tokenizer.consume().getContents());
                    }
                }
                selector.add(sb.toString());
                lastWasId = true;
            } else if (tokenizer.current().isSymbol(">", "+", "~")) {
                if (!lastWasId || !tokenizer.next().isIdentifier()) {
                    tokenizer.addError(tokenizer.current(),
                                       "Unexpected token: '%s'. A selector path must not contain two consecutive operators.",
                                       tokenizer.current().getSource());
                }
                selector.add(tokenizer.consume().getContents());
                lastWasId = false;
            } else {
                tokenizer.addError(tokenizer.current(), "Unexpected Token: %s", tokenizer.consume().getSource());
                lastWasId = false;
            }
        }
        return selector;
    }

    /*
     * Parses a variable declaration in form of "$variable: value;" or "$variable: value !default;"
     */
    private void parseVariableDeclaration() {
        Variable var = new Variable();
        var.setName(tokenizer.consume().getContents());
        tokenizer.consumeExpectedSymbol(":");
        var.setValue(parseExpression(true));
        if (tokenizer.current().isSymbol("!") && tokenizer.next().hasContent("default")) {
            var.setDefaultValue(true);
            tokenizer.consume();
            tokenizer.consume();
        }
        result.addVariable(var);
        tokenizer.consumeExpectedSymbol(";");
    }

    /*
     * Parses an expression which can be the value of an attribute or media query. Basic numeric operations
     * like +,-,*,/,% are supported. Also " " separated lists will be parsed as ValueList
     */
    private Expression parseExpression(boolean accepLists) {
        Expression result = parseAtom(accepLists);
        while (tokenizer.more()) {
            if (tokenizer.current().isSymbol("+", "-")) {
                result = new Operation(tokenizer.consume().getTrigger(), result, parseAtom(false));
            } else if (tokenizer.current().isSymbol("*", "/", "%")) {
                String operation = tokenizer.consume().getTrigger();
                Expression next = parseAtom(false);
                result = joinOperations(result, operation, next);
            } else {
                if (tokenizer.current().isSymbol() && !tokenizer.current().isSymbol("!")) {
                    break;
                }
                if (result instanceof ValueList) {
                    ((ValueList) result).add(parseAtom(accepLists));
                } else {
                    ValueList list = new ValueList();
                    list.add(result);
                    list.add(parseAtom(accepLists));
                    result = list;
                }
            }
        }
        return result;
    }

    /*
     * Takes care of operator precedence by modifying the AST appropriately
     */
    private Expression joinOperations(Expression result, String operation, Expression next) {
        if (!(result instanceof Operation)) {
            return new Operation(operation, result, next);
        }
        Operation farRight = (Operation) result;
        while (farRight.getRight() instanceof Operation) {
            farRight = (Operation) farRight.getRight();
        }
        if (!farRight.isProtect() && ("+".equals(farRight.getOperation()) || "-".equals(farRight.getOperation()))) {
            farRight.setRight(new Operation(operation, farRight.getRight(), next));
            return result;
        }

        return new Operation(operation, result, next);
    }

    /*
     * Parses an atom. This is either an identifier ("bold"), a number (15px), a string ('OpenSans'), a color
     * (#454545) or another expression in braces.
     */
    private Expression parseAtom(boolean acceptLists) {
        // Parse a number
        if (tokenizer.current().isNumber()) {
            return new com.scireum.ast.Number(tokenizer.consume().getContents());
        }
        // Parse a color
        if (tokenizer.current().isSpecialIdentifier("#")) {
            return new Value(tokenizer.consume().getSource()); //TODO encode color
        }
        // Parse an identifier or function call
        if (tokenizer.current().isIdentifier()) {
            if (tokenizer.next().isSymbol("(")) {
                // An identifier followed by '(' is a function call...
                FunctionCall fun = new FunctionCall();
                fun.setName(tokenizer.consume().getContents());
                tokenizer.consumeExpectedSymbol("(");
                while (tokenizer.more() && !tokenizer.current().isSymbol(")", ";", "{", "}")) {
                    fun.addParameter(parseExpression(false));
                    if (tokenizer.current().isSymbol(",")) {
                        tokenizer.consumeExpectedSymbol(",");
                    } else if (!tokenizer.current().isSymbol(")")) {
                        tokenizer.addError(tokenizer.current(),
                                           "Unexpected token: '" + tokenizer.consume()
                                                                            .getSource() + "'. Expected a comma between the parameters.");
                    }
                }
                tokenizer.consumeExpectedSymbol(")");
                return fun;
            } else if (tokenizer.next().isSymbol(",") && acceptLists) {
                // Parse a value list like Arial,Helvetica
                StringBuilder sb = new StringBuilder(tokenizer.consume().getSource());
                while (tokenizer.current().isSymbol(",")) {
                    sb.append(tokenizer.consume().getSource());
                    if (!tokenizer.current().isSymbol(";", "(", ")", "{", "}")) {
                        sb.append(tokenizer.consume().getSource());
                    }
                }
                return new Value(sb.toString());
            }

            // Neither function or value list -> simple value
            return new Value(tokenizer.consume().getSource());
        }

        // Parse as variable reference
        if (tokenizer.current().isSpecialIdentifier("$")) {
            return new VariableReference(tokenizer.consume().getContents());
        }

        // Parse as string constant
        if (tokenizer.current().isString()) {
            return new Value(tokenizer.consume().getSource());
        }

        // Parse as expression in braces
        if (tokenizer.current().isSymbol("(")) {
            tokenizer.consumeExpectedSymbol("(");
            Expression result = parseExpression(true);
            tokenizer.consumeExpectedSymbol(")");
            if (result instanceof Operation) {
                ((Operation) result).protect();
            }
            return result;
        }

        // Attribute values can be followed by things like "!import" -> make a value list
        if (tokenizer.current().isSymbol("!") && tokenizer.next().isIdentifier()) {
            tokenizer.consumeExpectedSymbol("!");
            return new Value("!" + tokenizer.consume().getContents());
        }

        // We failed! Report an error and return "" as value (we will fail anyway...)
        tokenizer.addError(tokenizer.current(),
                           "Unexpected token: '" + tokenizer.consume().getSource() + "'. Expected an expression.");
        return new Value("");

    }

    /*
     * Parse @mixin which are essentially template secions...
     */
    private Mixin parseMixin() {
        tokenizer.consumeExpectedKeyword("mixin");
        Mixin mixin = new Mixin();
        // Parse name
        if (tokenizer.current().isIdentifier()) {
            mixin.setName(tokenizer.consume().getContents());
        } else {
            tokenizer.addError(tokenizer.current(),
                               "Unexpected token: '" + tokenizer.current()
                                                                .getSource() + "'. Expected the name of the mixin as identifier.");
        }
        // Parse parameter names...
        tokenizer.consumeExpectedSymbol("(");
        while (tokenizer.more()) {
            if (tokenizer.current().isSymbol("{")) {
                tokenizer.addError(tokenizer.current(),
                                   "Unexpected token: '" + tokenizer.current()
                                                                    .getSource() + "'. Expected ')' to complete the parameter list.");
                break;
            }
            if (tokenizer.current().isSymbol(")")) {
                tokenizer.consumeExpectedSymbol(")");
                break;
            }
            if (tokenizer.current().isSpecialIdentifier("$")) {
                mixin.addParameter(tokenizer.consume().getContents());
            } else {
                tokenizer.addError(tokenizer.current(),
                                   "Unexpected token: '" + tokenizer.consume()
                                                                    .getSource() + "'. Expected a parameter name like $parameter.");
            }
            if (tokenizer.current().isSymbol(",")) {
                tokenizer.consumeExpectedSymbol(",");
            } else if (!tokenizer.current().isSymbol(")")) {
                tokenizer.addError(tokenizer.current(),
                                   "Unexpected token: '" + tokenizer.consume()
                                                                    .getSource() + "'. Expected a comma between the parameter names.");
            }
        }

        // Parse attributes - sub sections are not expected...
        tokenizer.consumeExpectedSymbol("{");
        while (tokenizer.more()) {
            if (tokenizer.current().isSymbol("}")) {
                tokenizer.consumeExpectedSymbol("}");
                return mixin;
            }
            if (tokenizer.current().isIdentifier() && tokenizer.next().isSymbol(":")) {
                Attribute attr = new Attribute(tokenizer.consume().getContents());
                tokenizer.consumeExpectedSymbol(":");
                attr.setExpression(parseExpression(true));
                mixin.addAttribute(attr);

                if (tokenizer.current().isSymbol(";") || !tokenizer.next().isSymbol("}")) {
                    tokenizer.consumeExpectedSymbol(";");
                }
            } else {
                tokenizer.addError(tokenizer.current(),
                                   "Unexpected token: '" + tokenizer.consume()
                                                                    .getSource() + "'. Expected an attribute definition.");
            }
        }
        tokenizer.consumeExpectedSymbol("}");
        return mixin;
    }

    /*
     * Parses an import statement like "@import 'test';"
     */
    private void parseImport() {
        tokenizer.consumeExpectedKeyword("import");
        if (!tokenizer.current().isString()) {
            tokenizer.addError(tokenizer.current(),
                               "Unexpected token: '" + tokenizer.current()
                                                                .getSource() + "'. Expected a string constant naming an import file.");
        } else {
            result.addImport(tokenizer.consume().getContents());
        }
        tokenizer.consumeExpectedSymbol(";");
    }
}
