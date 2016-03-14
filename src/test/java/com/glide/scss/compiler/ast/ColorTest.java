/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package com.glide.scss.compiler.ast;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests the color functions
 */
public class ColorTest {

    @Test
    public void testRGBtoHSLForBlack() {
        CSSColor.HSL hsl = new CSSColor("#000000").getHSL();
        assertEquals(0, hsl.getH());
        assertEquals(0, hsl.getS(), CSSColor.EPSILON);
        assertEquals(0, hsl.getL(), CSSColor.EPSILON);


        assertEquals("#000", hsl.getColor().toString());
    }

    @Test
    public void testRGBtoHSLForWhite() {
        CSSColor.HSL hsl = new CSSColor("#FFFFFF").getHSL();
        assertEquals(0, hsl.getH());
        assertEquals(0, hsl.getS(), CSSColor.EPSILON);
        assertEquals(1, hsl.getL(), CSSColor.EPSILON);

        assertEquals("#fff", hsl.getColor().toString());
    }

    @Test
    public void testRGBtoHSLForRed() {
        CSSColor.HSL hsl = new CSSColor("#FF0000").getHSL();
        assertEquals(0, hsl.getH());
        assertEquals(1, hsl.getS(), CSSColor.EPSILON);
        assertEquals(0.5, hsl.getL(), CSSColor.EPSILON);

        assertEquals("#f00", hsl.getColor().toString());
    }

    @Test
    public void testRGBtoHSLForMagenta() {
        CSSColor.HSL hsl = new CSSColor("#FF00FF").getHSL();
        assertEquals(300, hsl.getH());
        assertEquals(1, hsl.getS(), CSSColor.EPSILON);
        assertEquals(0.5, hsl.getL(), CSSColor.EPSILON);

        assertEquals("#f0f", hsl.getColor().toString());
    }

    @Test
    public void testRGBtoHSLForCyan() {
        CSSColor.HSL hsl = new CSSColor("#00FFFF").getHSL();
        assertEquals(180, hsl.getH());
        assertEquals(1, hsl.getS(), CSSColor.EPSILON);
        assertEquals(0.5, hsl.getL(), CSSColor.EPSILON);

        assertEquals("#0ff", hsl.getColor().toString());
    }

    @Test
    public void testRGBtoHSLForOlive() {
        CSSColor.HSL hsl = new CSSColor("#808000").getHSL();
        assertEquals(60, hsl.getH());
        assertEquals(1, hsl.getS(), CSSColor.EPSILON);
        assertEquals(0.25, hsl.getL(), CSSColor.EPSILON);

        assertEquals("#808000", hsl.getColor().toString());
    }

}
