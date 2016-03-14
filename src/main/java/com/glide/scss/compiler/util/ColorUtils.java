package com.glide.scss.compiler.util;

import java.awt.*;
import java.beans.ConstructorProperties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.istack.internal.NotNull;

import static java.lang.String.format;

public class ColorUtils {

	private static final Pattern RGB_HEX_PATTERN =
			Pattern.compile("#?([\\da-fA-F]{2})([\\da-fA-F]{2})([\\da-fA-F]{2})");

	private static final Pattern SHORT_RGB_HEX_PATTERN =
			Pattern.compile("#?([\\da-fA-F]{1})([\\da-fA-F]{1})([\\da-fA-F]{1})");

	public static Color HEXtoRGB(@NotNull final String hex) {
		int r = 0;
		int g = 0;
		int b = 0;

		Matcher m = RGB_HEX_PATTERN.matcher(hex);
		if (m.matches()) {
			r = Integer.parseInt(m.group(1).toLowerCase(), 16);
			g = Integer.parseInt(m.group(2).toLowerCase(), 16);
			b = Integer.parseInt(m.group(3).toLowerCase(), 16);
			return new Color(r, g, b);
		}

		m = SHORT_RGB_HEX_PATTERN.matcher(hex);
		if (m.matches()) {
			r = Integer.parseInt(m.group(1).toLowerCase() + m.group(1).toLowerCase(), 16);
			g = Integer.parseInt(m.group(2).toLowerCase() + m.group(2).toLowerCase(), 16);
			b = Integer.parseInt(m.group(3).toLowerCase() + m.group(3).toLowerCase(), 16);
			return new Color(r, g, b);
		}

		throw new IllegalArgumentException(format("Cannot parse %s as hex color. Expected a pattern like #FF00FF", hex));
	}

	/**
	 * Create a Color from the specified hex string.
	 *
	 * @param hex  a hex color string
	 * @param def  a default Color
	 * @return     the Color object of the hex string,
	 *             or the default Color if the hex string is not valid or can not be parsed.
	 */
	public static Color HEXtoRGB(@NotNull final String hex, @NotNull final Color color) {
		if (!isHexColor(hex))
			return color;
		return HEXtoRGB(hex);
	}

	/**
	 *
	 * @param hex
	 * @return
	 */
	public static boolean isHexColor(@NotNull final String hex) {
		return RGB_HEX_PATTERN.matcher(hex).matches() || SHORT_RGB_HEX_PATTERN.matcher(hex).matches();
	}

	/**
	 * Creates an opaque sRGB color with the specified red, green,
	 * and blue values in the range (0 - 255).
	 * The actual color used in rendering depends
	 * on finding the best match given the color space
	 * available for a given output device.
	 * Alpha is defaulted to 255.
	 *
	 * @throws IllegalArgumentException if <code>r</code>, <code>g</code>
	 *        or <code>b</code> are outside of the range
	 *        0 to 255, inclusive
	 * @param r the red component
	 * @param g the green component
	 * @param b the blue component
	 * @see java.awt.Color#Color(int, int, int)
	 */
	public static Color RGB(int r, int g, int b) {
		return RGB(r, b, b, 255);
	}

	/**
	 * Creates an sRGB color with the specified red, green, blue, and alpha
	 * values in the range (0 - 255).
	 *
	 * @throws IllegalArgumentException if <code>r</code>, <code>g</code>,
	 *        <code>b</code> or <code>a</code> are outside of the range
	 *        0 to 255, inclusive
	 * @param r the red component
	 * @param g the green component
	 * @param b the blue component
	 * @param a the alpha component
	 * @see java.awt.Color#Color(int, int, int, int)
	 */
	public static Color RGB(int r, int g, int b, int a) {
		return new Color(r, g, b, a);
	}
}
