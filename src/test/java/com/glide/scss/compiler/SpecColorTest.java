package com.glide.scss.compiler;

import org.testng.annotations.Test;

import static java.lang.String.format;

public class SpecColorTest extends AbstractTestCase {

	@Test(dataProvider = "sass-spec", dataProviderClass = SpecDataProvider.class)
	public void testSpec(final SpecData spec) {
		Log.info(format("spec: %s (%s)", spec.getName(), spec.getPath().toString()));
	}
}
