/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package org.serversass;

import org.serversass.ast.Color;
import org.serversass.ast.Expression;
import org.serversass.ast.FunctionCall;
import org.serversass.ast.Number;

/**
 * Contains all functions which can be called from sass.
 */
@SuppressWarnings("squid:S1172")
public class Functions {

    private Functions() {
    }

    private static Expression changeLighteness(Color color, int changeInPercent) {
        Color.HSL hsl = color.getHSL();
        hsl.setL(Math.max(Math.min(hsl.getL() * (1 + (changeInPercent / 100d)), 1d), 0d));
        return hsl.getColor();
    }

    private static Expression changeSaturation(Color color, int changeInPercent) {
        Color.HSL hsl = color.getHSL();
        hsl.setS(Math.max(Math.min(hsl.getS() * (1 + (changeInPercent / 100d)), 1d), 0d));
        return hsl.getColor();
    }

    /**
     * Creates a color from given RGB values.
     *
     * @param generator the surrounding generator
     * @param input     the function call to evaluate
     * @return the result of the evaluation
     */
    public static Expression rgb(Generator generator, FunctionCall input) {
        return new Color(input.getExpectedIntParam(0), input.getExpectedIntParam(1), input.getExpectedIntParam(2));
    }

    /**
     * Creates a color from given RGB and alpha values.
     *
     * @param generator the surrounding generator
     * @param input     the function call to evaluate
     * @return the result of the evaluation
     */
    public static Expression rgba(Generator generator, FunctionCall input) {
        if (input.getParameters().size() == 4) {
            return new Color(input.getExpectedIntParam(0),
                             input.getExpectedIntParam(1),
                             input.getExpectedIntParam(2),
                             input.getExpectedFloatParam(3));
        }
        if (input.getParameters().size() == 2) {
            Color color = input.getExpectedColorParam(0);
            float newA = input.getExpectedFloatParam(1);
            return new Color(color.getR(), color.getG(), color.getB(), newA);
        }
        throw new IllegalArgumentException("rgba must be called with either 2 or 4 parameters. Function call: "
                                           + input);
    }

    /**
     * Adjusts the hue of the given color by the given number of degrees.
     *
     * @param generator the surrounding generator
     * @param input     the function call to evaluate
     * @return the result of the evaluation
     */
    public static Expression adjusthue(Generator generator, FunctionCall input) {
        Color color = input.getExpectedColorParam(0);
        int changeInDegrees = input.getExpectedIntParam(1);
        Color.HSL hsl = color.getHSL();
        hsl.setH(hsl.getH() + changeInDegrees);
        return hsl.getColor();
    }

    /**
     * Increases the lightness of the given color by N percent.
     *
     * @param generator the surrounding generator
     * @param input     the function call to evaluate
     * @return the result of the evaluation
     */
    public static Expression lighten(Generator generator, FunctionCall input) {
        Color color = input.getExpectedColorParam(0);
        int increase = input.getExpectedIntParam(1);
        return changeLighteness(color, increase);
    }

    /**
     * Returns the alpha value of the gioven color
     *
     * @param generator the surrounding generator
     * @param input     the function call to evaluate
     * @return the result of the evaluation
     */
    public static Expression alpha(Generator generator, FunctionCall input) {
        Color color = input.getExpectedColorParam(0);
        return new Number(color.getA(), String.valueOf(color.getA()), "");
    }

    /**
     * Returns the alpha value of the given color
     *
     * @param generator the surrounding generator
     * @param input     the function call to evaluate
     * @return the result of the evaluation
     * @see #alpha(Generator, FunctionCall)
     */
    public static Expression opacity(Generator generator, FunctionCall input) {
        return alpha(generator, input);
    }

    /**
     * Decreases the lightness of the given color by N percent.
     *
     * @param generator the surrounding generator
     * @param input     the function call to evaluate
     * @return the result of the evaluation
     */
    public static Expression darken(Generator generator, FunctionCall input) {
        Color color = input.getExpectedColorParam(0);
        int decrease = input.getExpectedIntParam(1);
        return changeLighteness(color, -decrease);
    }

    /**
     * Increases the saturation of the given color by N percent.
     *
     * @param generator the surrounding generator
     * @param input     the function call to evaluate
     * @return the result of the evaluation
     */
    public static Expression saturate(Generator generator, FunctionCall input) {
        Color color = input.getExpectedColorParam(0);
        int increase = input.getExpectedIntParam(1);
        return changeSaturation(color, increase);
    }

    /**
     * Decreases the saturation of the given color by N percent.
     *
     * @param generator the surrounding generator
     * @param input     the function call to evaluate
     * @return the result of the evaluation
     */
    public static Expression desaturate(Generator generator, FunctionCall input) {
        Color color = input.getExpectedColorParam(0);
        int decrease = input.getExpectedIntParam(1);
        return changeSaturation(color, -decrease);
    }

    /**
     * Increases the opacity of the given color by the given amount.
     *
     * @param generator the surrounding generator
     * @param input     the function call to evaluate
     * @return the result of the evaluation
     */
    public static Expression opacify(Generator generator, FunctionCall input) {
        Color color = input.getExpectedColorParam(0);
        float decrease = input.getExpectedFloatParam(1);
        return new Color(color.getR(), color.getG(), color.getB(), color.getA() + decrease);
    }

    /**
     * Increases the opacity of the given color by the given amount.
     *
     * @param generator the surrounding generator
     * @param input     the function call to evaluate
     * @return the result of the evaluation
     */
    public static Expression fade_in(Generator generator, FunctionCall input) {
        return opacify(generator, input);
    }

    /**
     * Decreases the opacity of the given color by the given amount.
     *
     * @param generator the surrounding generator
     * @param input     the function call to evaluate
     * @return the result of the evaluation
     */
    public static Expression transparentize(Generator generator, FunctionCall input) {
        Color color = input.getExpectedColorParam(0);
        float decrease = input.getExpectedFloatParam(1);
        return new Color(color.getR(), color.getG(), color.getB(), color.getA() - decrease);
    }

    /**
     * Decreases the opacity of the given color by the given amount.
     *
     * @param generator the surrounding generator
     * @param input     the function call to evaluate
     * @return the result of the evaluation
     */
    public static Expression fade_out(Generator generator, FunctionCall input) {
        return opacify(generator, input);
    }

    /**
     * Decreases the opacity of the given color by the given amount.
     *
     * @param generator the surrounding generator
     * @param input     the function call to evaluate
     * @return the result of the evaluation
     */
    public static Expression mix(Generator generator, FunctionCall input) {
        Color color1 = input.getExpectedColorParam(0);
        Color color2 = input.getExpectedColorParam(1);
        float weight = input.getParameters().size() > 2 ? input.getExpectedFloatParam(2) : 0.5f;
        return new Color((int) Math.round(color1.getR() * weight + color2.getR() * (1.0 - weight)),
                         (int) Math.round(color1.getG() * weight + color2.getG() * (1.0 - weight)),
                         (int) Math.round(color1.getB() * weight + color2.getB() * (1.0 - weight)),
                         (float) (color1.getA() * weight + color2.getA() * (1.0 - weight)));
    }
}
