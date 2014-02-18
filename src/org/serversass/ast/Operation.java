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

import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 16.02.14
 * Time: 21:17
 * To change this template use File | Settings | File Templates.
 */
public class Operation extends Expression {
    private String operation;
    private Expression left;
    private Expression right;
    private boolean protect = false;

    public Operation(String operation, Expression left, Expression right) {
        this.operation = operation;
        this.left = left;
        this.right = right;
    }

    public void protect() {
        protect = true;
    }

    public String getOperation() {
        return operation;
    }

    public boolean isProtect() {
        return protect;
    }

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    public void setRight(Expression right) {
        this.right = right;
    }

    @Override
    public String toString() {
        return (protect ? "(" : "") + left + " " + operation + " " + right + (protect ? ")" : "");
    }

    @Override
    public boolean isConstant() {
        return left.isConstant() && right.isConstant();
    }

    @Override
    public Expression eval(Scope scope, Generator gen) {
        Expression newLeft = left.eval(scope, gen);
        Expression newRight = right.eval(scope, gen);
        if ((newRight instanceof Number) && (newRight instanceof Number)) {
            Number l = (Number) newLeft;
            Number r = (Number) newRight;

            double lVal = l.getNumericValue();
            String lUnit = l.getUnit();
            if ("%".equals(lUnit)) {
                lVal /= 100d;
                lUnit = "";
            }
            double rVal = r.getNumericValue();
            String rUnit = r.getUnit();
            if ("%".equals(rUnit)) {
                rVal /= 100d;
                rUnit = "";
            }

            double value = 0d;
            if ("/".equals(operation)) {
                if (rVal != 0) {
                    value = lVal / rVal;
                } else {
                    gen.warn(String.format("Cannot evaluate: '%s': division by 0. Defaulting to 0 as result", this));
                }
            } else if ("*".equals(operation)) {
                value = lVal * rVal;
            } else if ("%".equals(operation)) {
                value = lVal % rVal;
            } else if ("+".equals(operation)) {
                value = lVal + rVal;
            } else if ("-".equals(operation)) {
                value = lVal - rVal;
            }

            String unit = "";
            if (!"/".equals(operation)) {
                if ("%".equals(l.getUnit()) && ("%".equals(r.getUnit()) || "".equals(r.getUnit())) || "".equals(l.getUnit()) && "%"
                        .equals(r.getUnit())) {
                    value *= 100;
                    unit = "%";
                } else {
                    unit = lUnit;
                    if ("".equals(unit)) {
                        unit = rUnit;
                    } else if (!"".equals(rUnit) && !lUnit.equals(rUnit)) {
                        gen.warn(String.format("Incompatible units mixed in expression '%s': Using left unit for result", this));
                    }
                }
            }
            double rounded = Math.round(value);
            if (Math.abs(value - rounded) > 0.009) {
                return new Number(value, String.format(Locale.ENGLISH, "%1.2f", value), unit);
            }
            return new Number(value, String.valueOf(Math.round(value)), unit);
        } else {
            return new Value(newLeft.toString() + newRight.toString());
        }
    }
}
