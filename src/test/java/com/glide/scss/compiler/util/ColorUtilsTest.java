package com.glide.scss.compiler.util;

import java.awt.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.glide.scss.compiler.AbstractTestCase;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class ColorUtilsTest extends AbstractTestCase {

	static final String HEX_BLACK = "000000";
	static final String HEX_BLACK_HASH = format("#%s", HEX_BLACK);
	static final String HEX_NULL = "";

	@DataProvider(name = "dp-colors", parallel = true)
	public static Object[][] dataColors() {
		return new Object[][] {
				{ HEX_BLACK, Color.BLACK, true },
				{ HEX_BLACK_HASH, Color.BLACK, true },
				{ HEX_NULL, null, false }
		};
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testHEXtoRGBException() {
		ColorUtils.HEXtoRGB(HEX_NULL);
	}

	@Test(dataProvider = "dp-colors")
	public void testHEXtoRGB(final String hex, final Color expectedColor, boolean isColor) {

		if (!isColor) return;

		Color color = ColorUtils.HEXtoRGB(hex);
		assertThat(format("%s should have have return %s", hex, expectedColor.toString()),
				color, equalTo(expectedColor));
	}

	@Test(dataProvider = "dp-colors")
	public void testIsHexColor(final String hex, final Color expectedColor, boolean isColor) {
		assertThat(format("%s isHexColor == %s ", hex, isColor),
				ColorUtils.isHexColor(hex), equalTo(isColor));
	}
}
