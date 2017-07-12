/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package org.serversass;

import org.serversass.ast.Attribute;
import org.serversass.ast.Color;
import org.serversass.ast.Expression;
import org.serversass.ast.FunctionCall;
import org.serversass.ast.MediaFilter;
import org.serversass.ast.Mixin;
import org.serversass.ast.MixinReference;
import org.serversass.ast.NamedParameter;
import org.serversass.ast.Number;
import org.serversass.ast.Operation;
import org.serversass.ast.Section;
import org.serversass.ast.Stylesheet;
import org.serversass.ast.Value;
import org.serversass.ast.ValueList;
import org.serversass.ast.Variable;
import org.serversass.ast.VariableReference;
import parsii.tokenizer.Char;
import parsii.tokenizer.ParseException;
import parsii.tokenizer.Token;
import parsii.tokenizer.Tokenizer;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a given SASS source into a {@link Stylesheet}.
 */
public class Parser {

    /**
     * How to put that right: CSS is kind of "special gifted" - so tokenization is not always that straightforward.
     * <p>
     * Therefore we subclass the tokenizer to handle css selectors etc. right here in the tokenizer.
     */
    static class SassTokenizer extends Tokenizer {

        SassTokenizer(Reader input) {
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
            return (input.current().is('-') || input.current().is('.')) && input.next().isLetter();
        }

        @Override
        protected boolean isIdentifierChar(Char current) {
            if (super.isIdentifierChar(current)) {
                return true;
            }
            // CSS selectors can contain "-", "." or "#" as long as it is not the last character of the token
            return (current.is('-') || current.is('.') || current.is('#')) && !input.next().isWhitepace();
        }

        @Override
        protected boolean isSymbolCharacter(Char ch) {
            return super.isSymbolCharacter(ch) && !ch.is('#');
        }

        @Override
        protected Token fetchSymbol() {
            Token result = Token.create(Token.TokenType.SYMBOL, input.current());
            result.addToTrigger(input.consume());
            while (isSymbolCharacter(input.current()) && !input.current().is(',')) {
                result.addToTrigger(input.consume());
            }
            return result;
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
        parseSectionSelector(mediaQuery, result);
        tokenizer.consumeExpectedSymbol("{");
        while (tokenizer.more()) {
            if (tokenizer.current().isSymbol("}")) {
                tokenizer.consumeExpectedSymbol("}");
                return result;
            }
            // Parse "normal" attributes like "font-weight: bold;"
            if (isAtAttribute()) {
                Attribute attr = parseAttribute();
                result.addAttribute(attr);
            } else if (tokenizer.current().isKeyword("media")) {
                // Take care of @media sub sections
                result.addSubSection(parseSection(true));
            } else if (tokenizer.current().isKeyword("include")) {
                parseInclude(result);
            } else if (tokenizer.current().isKeyword("extend")) {
                parseExtend(result);
            } else {
                // If it is neither an attribute, nor a media query or instruction - it is probably a sub section...
                result.addSubSection(parseSection(false));
            }
        }
        tokenizer.consumeExpectedSymbol("}");
        return result;
    }

    private boolean isAtAttribute() {
        // an attribute has at least to start with x: y ...
        if (!tokenizer.current().isIdentifier() || !tokenizer.next().isSymbol(":")) {
            return false;
        }

        // Now as a:hover div span {
        // and
        // border: 1px solid red ; look almost the same to the tokenizer,
        // we have to actually search for the final ";" to determine if we're
        // really looking at an attribute....
        int i = 2;
        while (true) {
            Token next = tokenizer.next(i);
            if (next.isEnd() || next.isSymbol(";")) {
                return true;
            } else if (next.isSymbol("{")) {
                return false;
            } else {
                i++;
            }
        }
    }

    private void parseExtend(Section result) {
        // Parse @extend instructions like "@extend .warning"
        tokenizer.consumeExpectedKeyword("extend");
        if (tokenizer.current().isIdentifier() || tokenizer.current().isSpecialIdentifier("#")) {
            result.addExtends(tokenizer.consume().getSource());
        } else {
            tokenizer.addError(tokenizer.current(),
                               "Unexpected token: '"
                               + tokenizer.current().getSource()
                               + "'. Expected a selector to include.");
        }
        if (tokenizer.current().isSymbol(";") || !tokenizer.next().isSymbol("}")) {
            tokenizer.consumeExpectedSymbol(";");
        }
    }

    private void parseInclude(Section result) {
        // Take care of included mixins like "@include border(15px);"
        tokenizer.consumeExpectedKeyword("include");
        MixinReference ref = new MixinReference();
        if (tokenizer.current().isIdentifier()) {
            ref.setName(tokenizer.consume().getContents());
        } else {
            tokenizer.addError(tokenizer.current(),
                               "Unexpected token: '" + tokenizer.current().getSource() + "'. Expected a mixin to use");
        }
        if (tokenizer.current().isSymbol("(")) {
            tokenizer.consumeExpectedSymbol("(");
            // Parse parameters - be as error tolerant as possible
            while (tokenizer.more() && !tokenizer.current().isSymbol(")", ";", "{", "}")) {
                ref.addParameter(parseExpression(false));
                consumeExpectedComma();
            }
            tokenizer.consumeExpectedSymbol(")");
        }
        if (tokenizer.current().isSymbol(";") || !tokenizer.next().isSymbol("}")) {
            tokenizer.consumeExpectedSymbol(";");
        }
        if (ref.getName() != null) {
            result.addMixinReference(ref);
        }
    }

    private void parseSectionSelector(boolean mediaQuery, Section result) {
        if (mediaQuery) {
            // Parse a media query like @media screen and (min-width: 1200px)
            tokenizer.consumeExpectedKeyword("media");
            while (true) {
                if (tokenizer.current().isIdentifier()) {
                    // Handle plain identifiers like "screen" or "print"
                    result.addMediaQuery(new Value(tokenizer.consume().getContents()));
                } else if (tokenizer.current().isSymbol("(")) {
                    parseMediaQueryFilters(result);
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
    }

    private void parseMediaQueryFilters(Section result) {
        // Handle filters like (orientation: landscape)
        tokenizer.consumeExpectedSymbol("(");
        if (tokenizer.current().isIdentifier() && tokenizer.next().isSymbol(":")) {
            parseMediaQueryFilter(result);
            while (tokenizer.next().hasContent("and")) {
                tokenizer.consumeExpectedSymbol(")");
                tokenizer.consume();
                tokenizer.consumeExpectedSymbol("(");
                parseMediaQueryFilter(result);
            }
        } else {
            tokenizer.addError(tokenizer.current(),
                               "Unexpected symbol: '%s'. Expected an attribute filter.",
                               tokenizer.current().getSource());
        }
        tokenizer.consumeExpectedSymbol(")");
    }

    private void parseMediaQueryFilter(Section result) {
        MediaFilter attr = new MediaFilter(tokenizer.consume().getContents());
        tokenizer.consumeExpectedSymbol(":");
        attr.setExpression(parseExpression(true));
        result.addMediaQuery(attr);
    }

    private Attribute parseAttribute() {
        Attribute attr = new Attribute(tokenizer.consume().getContents());
        tokenizer.consumeExpectedSymbol(":");
        attr.setExpression(parseExpression(true));

        if (tokenizer.current().isSymbol(";") || !tokenizer.next().isSymbol("}")) {
            tokenizer.consumeExpectedSymbol(";");
        }
        return attr;
    }

    /*
     * CSS and therefore als SASS supports a wide range or complex selector strings. The following method
     * parses such selectors while performing basic consistency checks
     */
    private List<String> parseSelector() {
        List<String> selector = new ArrayList<>();
        if (tokenizer.more() && tokenizer.current().isSymbol("[")) {
            StringBuilder sb = new StringBuilder();
            parseFilterInSelector(sb);
            parseOperatorInSelector(sb);
            selector.add(sb.toString());
        }
        if (tokenizer.more() && tokenizer.current().isSymbol("&")) {
            selector.add(tokenizer.consume().getTrigger());
        }
        if (tokenizer.more() && tokenizer.current().isSymbol("&:")) {
            tokenizer.consume();
            if (tokenizer.current().is(Token.TokenType.ID)) {
                selector.add("&");
                selector.add(":" + tokenizer.consume().getContents());
            }
        }
        if (tokenizer.more() && tokenizer.current().isSymbol("::") && tokenizer.next().is(Token.TokenType.ID)) {
            tokenizer.consume();
            selector.add("::" + tokenizer.consume().getContents());
        }
        while (tokenizer.more()) {
            if (tokenizer.current().isSymbol("{", ",")) {
                if (selector.isEmpty()) {
                    tokenizer.addError(tokenizer.current(), "Unexpected end of CSS selector");
                }

                return selector;
            } else if (tokenizer.current().isIdentifier()
                       || tokenizer.current().isSpecialIdentifier("#", "@")
                       || tokenizer.current().isNumber()) {
                StringBuilder sb = new StringBuilder(tokenizer.consume().getSource());
                parseFilterInSelector(sb);
                parseOperatorInSelector(sb);
                selector.add(sb.toString());
            } else if (tokenizer.current().isSymbol("&") || tokenizer.current().isSymbol("*")) {
                selector.add(tokenizer.consume().getTrigger());
            } else if (tokenizer.current().isSymbol(">", "+", "~")) {
                selector.add(tokenizer.consume().getSource());
            } else {
                tokenizer.addError(tokenizer.current(), "Unexpected Token: %s", tokenizer.consume().getSource());
            }
        }
        return selector;
    }

    private void parseOperatorInSelector(StringBuilder sb) {
        while (tokenizer.current().isSymbol(":")) {
            sb.append(tokenizer.consume().getSource());
            sb.append(tokenizer.consume().getSource());
            // Consume arguments like :nth-child(2)
            if (tokenizer.current().isSymbol("(")) {
                sb.append(tokenizer.consume().getSource());
                int braces = 1;
                while (!tokenizer.current().isEnd() && braces > 0) {
                    if (tokenizer.current().isSymbol("(")) {
                        braces++;
                    }
                    if (tokenizer.current().isSymbol(")")) {
                        braces--;
                    }
                    sb.append(tokenizer.consume().getSource());
                }
            }
        }
    }

    private void parseFilterInSelector(StringBuilder sb) {
        while (tokenizer.current().isSymbol("[")) {
            // Consume [
            sb.append(tokenizer.consume().getContents());
            // Read attribute name
            if (!tokenizer.current().isSymbol("]")) {
                if (!tokenizer.current().isIdentifier()) {
                    tokenizer.addError(tokenizer.current(),
                                       "Unexpected token: '%s'. Expected an attribute name.",
                                       tokenizer.current().getSource());
                }
                sb.append(tokenizer.consume().getContents());
            }
            // Read operator
            if (!tokenizer.current().isSymbol("]")) {
                if (!tokenizer.current().isSymbol("=", "~=", "|=", "^=", "$=", "*=")) {
                    tokenizer.addError(tokenizer.current(),
                                       "Unexpected token: '%s'. Expected an operation.",
                                       tokenizer.current().getSource());
                }
                sb.append(tokenizer.consume().getTrigger());
            }
            // Read value
            if (!tokenizer.current().isSymbol("]")) {
                sb.append(tokenizer.consume().getSource());
            }
            // Consume ]
            if (!tokenizer.current().isSymbol("]")) {
                tokenizer.addError(tokenizer.current(),
                                   "Unexpected token: '%s'. Expected: ']'",
                                   tokenizer.current().getSource());
            } else {
                sb.append(tokenizer.consume().getContents());
            }
        }
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
        Expression result = accepLists ? parseAtomList() : parseAtom();
        while (tokenizer.more()) {
            if (tokenizer.current().isSymbol("+", "-")) {
                result = new Operation(tokenizer.consume().getTrigger(), result, parseAtom());
            } else if (tokenizer.current().isSymbol("*", "/", "%")) {
                String operation = tokenizer.consume().getTrigger();
                Expression next = parseAtom();
                result = joinOperations(result, operation, next);
            } else {
                if (tokenizer.current().isSymbol() && !tokenizer.current().isSymbol("!")) {
                    break;
                }
                ValueList list = new ValueList(false);
                list.add(result);
                list.add(accepLists ? parseAtomList() : parseAtom());
                result = list;
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
    private Expression parseAtomList() {
        Expression exp = parseAtom();
        if (!tokenizer.current().isSymbol(",")) {
            return exp;
        }

        ValueList result = new ValueList(true);
        result.add(exp);
        while (tokenizer.current().isSymbol(",")) {
            tokenizer.consume();
            result.add(parseAtom());
        }

        return result;
    }

    /*
     * Parses an atom. This is either an identifier ("bold"), a number (15px), a string ('OpenSans'), a color
     * (#454545) or another expression in braces.
     */
    private Expression parseAtom() {
        // Parse a number
        if (tokenizer.current().isNumber()) {
            return new Number(tokenizer.consume().getContents());
        }
        // Parse a color
        if (tokenizer.current().isSpecialIdentifier("#")) {
            return new Color(tokenizer.consume().getSource());
        }

        // Parse an identifier or function call
        if (tokenizer.current().isIdentifier() || tokenizer.current().isString()) {
            return parseIdentifierOrFunctionCall();
        }

        // Parse as variable reference
        if (tokenizer.current().isSpecialIdentifier("$")) {
            return new VariableReference(tokenizer.consume().getContents());
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

    private Expression parseIdentifierOrFunctionCall() {
        // Identifiers might contain ':' like "progid:DXImageTransform.Microsoft.gradient"
        String id = "";
        while (tokenizer.current().isIdentifier() && tokenizer.next().isSymbol(":")) {
            id += tokenizer.consume().getSource() + ":";
            tokenizer.consume();
        }
        id += tokenizer.consume().getSource();

        if (tokenizer.current().isSymbol("(")) {
            // An identifier followed by '(' is a function call...
            FunctionCall fun = new FunctionCall();
            fun.setName(id);
            tokenizer.consumeExpectedSymbol("(");
            while (tokenizer.more() && !tokenizer.current().isSymbol(")", ";", "{", "}")) {
                if (tokenizer.current().isIdentifier() && tokenizer.next().isSymbol("=")) {
                    String name = tokenizer.consume().getContents();
                    tokenizer.consume();
                    fun.addParameter(new NamedParameter(name, parseExpression(false)));
                } else {
                    fun.addParameter(parseExpression(false));
                }
                consumeExpectedComma();
            }
            tokenizer.consumeExpectedSymbol(")");
            return fun;
        }

        // Neither function or value list -> simple value
        return new Value(id);
    }

    private void consumeExpectedComma() {
        if (tokenizer.current().isSymbol(",")) {
            tokenizer.consumeExpectedSymbol(",");
        } else if (!tokenizer.current().isSymbol(")")) {
            tokenizer.addError(tokenizer.current(),
                               "Unexpected token: '"
                               + tokenizer.consume().getSource()
                               + "'. Expected a comma between the parameters.");
        }
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
                               "Unexpected token: '"
                               + tokenizer.current().getSource()
                               + "'. Expected the name of the mixin as identifier.");
        }
        parseParameterNames(mixin);

        // Parse attributes - sub sections are not expected...
        tokenizer.consumeExpectedSymbol("{");
        while (tokenizer.more()) {
            if (tokenizer.current().isSymbol("}")) {
                tokenizer.consumeExpectedSymbol("}");
                return mixin;
            }
            if (isAtAttribute()) {
                Attribute attr = parseAttribute();
                mixin.addAttribute(attr);
            } else {
                // If it isn't an attribute it is (hopefully) a subsection
                Section subSection = new Section();
                parseSectionSelector(false, subSection);
                tokenizer.consumeExpectedSymbol("{");
                while (tokenizer.more() && !tokenizer.current().isSymbol("}")) {
                    if (tokenizer.current().isIdentifier() && tokenizer.next().isSymbol(":")) {
                        Attribute attr = parseAttribute();
                        subSection.addAttribute(attr);
                    } else {
                        tokenizer.addError(tokenizer.current(),
                                           "Unexpected token: '"
                                           + tokenizer.current().getSource()
                                           + "'. Expected an attribute definition");
                        tokenizer.consume();
                    }
                }
                tokenizer.consumeExpectedSymbol("}");
                mixin.addSubSection(subSection);
            }
        }
        tokenizer.consumeExpectedSymbol("}");
        return mixin;
    }

    private void parseParameterNames(Mixin mixin) {
        tokenizer.consumeExpectedSymbol("(");
        while (tokenizer.more()) {
            if (tokenizer.current().isSymbol("{")) {
                tokenizer.addError(tokenizer.current(),
                                   "Unexpected token: '"
                                   + tokenizer.current().getSource()
                                   + "'. Expected ')' to complete the parameter list.");
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
                                   "Unexpected token: '"
                                   + tokenizer.consume().getSource()
                                   + "'. Expected a parameter name like $parameter.");
            }
            if (tokenizer.current().isSymbol(",")) {
                tokenizer.consumeExpectedSymbol(",");
            } else if (!tokenizer.current().isSymbol(")")) {
                tokenizer.addError(tokenizer.current(),
                                   "Unexpected token: '"
                                   + tokenizer.consume().getSource()
                                   + "'. Expected a comma between the parameter names.");
            }
        }
    }

    /*
     * Parses an import statement like "@import 'test';"
     */
    private void parseImport() {
        tokenizer.consumeExpectedKeyword("import");
        if (!tokenizer.current().isString()) {
            tokenizer.addError(tokenizer.current(),
                               "Unexpected token: '"
                               + tokenizer.current().getSource()
                               + "'. Expected a string constant naming an import file.");
        } else {
            result.addImport(tokenizer.consume().getContents());
        }
        tokenizer.consumeExpectedSymbol(";");
    }
}
