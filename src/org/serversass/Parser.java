/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package org.serversass;

import org.serversass.ast.*;
import org.serversass.ast.Number;
import parsii.tokenizer.Char;
import parsii.tokenizer.ParseException;
import parsii.tokenizer.Token;
import parsii.tokenizer.Tokenizer;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 13.02.14
 * Time: 22:16
 * To change this template use File | Settings | File Templates.
 */
public class Parser {

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
            stringToken.addToContent(escapeChar);
            stringToken.addToContent(input.consume());
            return true;
        }

        @Override
        protected boolean isAtBracket(boolean inSymbol) {
            return super.isAtBracket(inSymbol) || input.current().is('%');
        }

        @Override
        protected boolean isAtStartOfIdentifier() {
            if (super.isAtStartOfIdentifier()) {
                return true;
            }
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
            if ((current.is('-') || current.is('.') || current.is('#')) && !input.next().isWhitepace()) {
                return true;
            }

            return false;
        }
    }

    private final SassTokenizer tokenizer;
    private Stylesheet result;

    public Parser(String name, Reader input) {
        tokenizer = new SassTokenizer(input);
        result = new Stylesheet(name);
    }

    public Stylesheet compile() throws ParseException {
        while (tokenizer.more()) {
            if (tokenizer.current().isKeyword("import")) {
                parseImport();
            } else if (tokenizer.current().isKeyword("mixin")) {
                Mixin mixin = parseMixin();
                if (mixin.getName() != null) {
                    result.addMixin(mixin);
                }
            } else if (tokenizer.current().isKeyword("media")) {
                result.addSection(parseSection(true));
            } else if (tokenizer.current().isSpecialIdentifier("$") && tokenizer.next().isSymbol(":")) {
                parseVariableDeclaration();
            } else {
                result.addSection(parseSection(false));
            }
        }

        if (!tokenizer.getProblemCollector().isEmpty()) {
            throw ParseException.create(tokenizer.getProblemCollector());
        }

        return result;
    }

    private Section parseSection(boolean mediaQuery) {
        Section result = new Section();
        if (mediaQuery) {
            tokenizer.consumeExpectedKeyword("media");
            while (true) {
                if (tokenizer.current().isIdentifier()) {
                    result.addMediaQuery(new Value(tokenizer.consume().getContents()));
                } else if (tokenizer.current().isSymbol("(")) {
                    tokenizer.consumeExpectedSymbol("(");
                    if (tokenizer.current().isIdentifier() && tokenizer.next().isSymbol(":")) {
                        MediaFilter attr = new MediaFilter(tokenizer.consume().getContents());
                        tokenizer.consumeExpectedSymbol(":");
                        attr.setExpression(parseExpression());
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
                if (!tokenizer.current().isIdentifier("and")) {
                    break;
                } else {
                    tokenizer.consume();
                }
            }
        } else {
            while (tokenizer.more()) {
                List<String> selector = parseSelector();
                result.getSelectors().add(selector);
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
            if (tokenizer.current().isIdentifier() && tokenizer.next().isSymbol(":")) {
                Attribute attr = new Attribute(tokenizer.consume().getContents());
                tokenizer.consumeExpectedSymbol(":");
                attr.setExpression(parseExpression());
                result.addAttribute(attr);

                if (tokenizer.current().isSymbol(";") || !tokenizer.next().isSymbol("}")) {
                    tokenizer.consumeExpectedSymbol(";");
                }
            } else if (tokenizer.current().isKeyword("media")) {
                result.addSubSection(parseSection(true));
            } else if (tokenizer.current().isKeyword("include")) {
                tokenizer.consumeExpectedKeyword("include");
                MixinReference ref = new MixinReference();
                if (tokenizer.current().isIdentifier()) {
                    ref.setName(tokenizer.consume().getContents());
                } else {
                    tokenizer.addError(tokenizer.current(),
                                       "Unexpected token: '" + tokenizer.current()
                                                                        .getSource() + "'. Expected a mixin to use");
                }
                tokenizer.consumeExpectedSymbol("(");
                while (tokenizer.more() && !tokenizer.current().isSymbol(")", ";", "{", "}")) {
                    ref.addParameter(parseExpression());
                    if (tokenizer.current().isSymbol(",")) {
                        tokenizer.consumeExpectedSymbol(",");
                    } else if (!tokenizer.current().isSymbol(")")) {
                        tokenizer.addError(tokenizer.current(),
                                           "Unexpected token: '" + tokenizer.consume()
                                                                            .getSource() + "'. Expected a comma between the parameters.");
                    }
                }
                tokenizer.consumeExpectedSymbol(")");
                if (tokenizer.current().isSymbol(";") || !tokenizer.next().isSymbol("}")) {
                    tokenizer.consumeExpectedSymbol(";");
                }
                if (ref.getName() != null) {
                    result.addMixinReference(ref);
                }
            } else if (tokenizer.current().isKeyword("extend")) {
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
                result.addSubSection(parseSection(false));
            }
        }
        tokenizer.consumeExpectedSymbol("}");
        return result;
    }

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

    private void parseVariableDeclaration() {
        Variable var = new Variable();
        var.setName(tokenizer.consume().getContents());
        tokenizer.consumeExpectedSymbol(":");
        var.setValue(parseExpression());
        if (tokenizer.current().isSymbol("!") && tokenizer.next().hasContent("default")) {
            var.setDefaultValue(true);
            tokenizer.consume();
            tokenizer.consume();
        }
        result.addVariable(var);
        tokenizer.consumeExpectedSymbol(";");
    }

    private Expression parseExpression() {
        Expression result = parseAtom();
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
                if (result instanceof ValueList) {
                    ((ValueList) result).add(parseAtom());
                } else {
                    ValueList list = new ValueList();
                    list.add(result);
                    list.add(parseAtom());
                    result = list;
                }
            }
        }
        return result;
    }

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

    private Expression parseAtom() {
        if (tokenizer.current().isNumber()) {
            return new Number(tokenizer.consume().getContents());
        }
        if (tokenizer.current().isSpecialIdentifier("#")) {
            return new Value(tokenizer.consume().getSource()); //TODO encode color
        }
        if (tokenizer.current().isIdentifier()) {
            if (tokenizer.next().isSymbol("(")) {
                FunctionCall fun = new FunctionCall();
                fun.setName(tokenizer.consume().getContents());
                tokenizer.consumeExpectedSymbol("(");
                while (tokenizer.more() && !tokenizer.current().isSymbol(")", ";", "{", "}")) {
                    fun.addParameter(parseExpression());
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
            } else if (tokenizer.next().isSymbol(",")) {
                StringBuilder sb = new StringBuilder(tokenizer.consume().getSource());
                while (tokenizer.current().isSymbol(",")) {
                    sb.append(tokenizer.consume().getSource());
                    if (!tokenizer.current().isSymbol(";", "(", ")", "{", "}")) {
                        sb.append(tokenizer.consume().getSource());
                    }
                }
                return new Value(sb.toString());
            }
            return new Value(tokenizer.consume().getSource());
        }
        if (tokenizer.current().isSpecialIdentifier("$")) {
            return new VariableReference(tokenizer.consume().getContents());
        }
        if (tokenizer.current().isString()) {
            return new Value(tokenizer.consume().getSource());
        }
        if (tokenizer.current().isSymbol("(")) {
            tokenizer.consumeExpectedSymbol("(");
            Expression result = parseExpression();
            tokenizer.consumeExpectedSymbol(")");
            if (result instanceof Operation) {
                ((Operation) result).protect();
            }
            return result;
        }
        if (tokenizer.current().isSymbol("!") && tokenizer.next().isIdentifier()) {
            tokenizer.consumeExpectedSymbol("!");
            return new Value("!" + tokenizer.consume().getContents());
        }
        tokenizer.addError(tokenizer.current(),
                           "Unexpected token: '" + tokenizer.consume().getSource() + "'. Expected an expression.");
        return new Value("");

    }

    private Mixin parseMixin() {
        tokenizer.consumeExpectedKeyword("mixin");
        Mixin mixin = new Mixin();
        if (tokenizer.current().isIdentifier()) {
            mixin.setName(tokenizer.consume().getContents());
        } else {
            tokenizer.addError(tokenizer.current(),
                               "Unexpected token: '" + tokenizer.current()
                                                                .getSource() + "'. Expected the name of the mixin as identifier.");
        }
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

        tokenizer.consumeExpectedSymbol("{");
        while (tokenizer.more()) {
            if (tokenizer.current().isSymbol("}")) {
                tokenizer.consumeExpectedSymbol("}");
                return mixin;
            }
            if (tokenizer.current().isIdentifier() && tokenizer.next().isSymbol(":")) {
                Attribute attr = new Attribute(tokenizer.consume().getContents());
                tokenizer.consumeExpectedSymbol(":");
                attr.setExpression(parseExpression());
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
