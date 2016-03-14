package com.glide.scss.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import javax.validation.constraints.NotNull;

import org.testng.annotations.DataProvider;

import static java.lang.String.format;

/**
 * Create data provider for sass-spec tests.
 *
 * @author pete.chanthamynavong
 */
public class SpecDataProvider {

	private static final Logger Log = Logger.getLogger(SpecDataProvider.class.getName());

	@DataProvider(name = "sass-spec")
	public static Iterator<Object[]> specs() throws IOException {
		final List<SpecData> specs = new LinkedList<>();
		final Path specPath = Paths.get(SpecData.SASS_SPEC_DIR);
		final String glob = Paths.get(SpecData.SASS_SPEC_DIR, "**", SpecData.INPUT_SCSS_FILENAME).toString();

		Files.walkFileTree(specPath, new SpecPathFileVisitor(specs, glob));

		final Collection<Object[]> params = new LinkedList<>();

		for (SpecData item : specs) {
			params.add(new Object[]{ item });
		}

		return params.iterator();
	}

	@DataProvider(name = "sass-spec/basic")
	public static Iterator<Object[]> specBasic() throws IOException {
		return createCollection("basic").iterator();
	}

	@DataProvider(name = "sass-spec/colors")
	public static Iterator<Object[]> specColors() throws IOException {
		return createCollection("colors").iterator();
	}

	@DataProvider(name = "sass-spec/css")
	public static Iterator<Object[]> specCss() throws IOException {
		return createCollection("css").iterator();
	}

	@DataProvider(name = "sass-spec/errors")
	public static Iterator<Object[]> specErrors() throws IOException {
		return createCollection("errors").iterator();
	}

	@DataProvider(name = "sass-spec/extend-tests")
	public static Iterator<Object[]> spexExtendTests() throws IOException {
		return createCollection("extend-tests").iterator();
	}

	@DataProvider(name = "sass-spec/maps")
	public static Iterator<Object[]> specMaps() throws IOException {
		return createCollection("maps").iterator();
	}

	@DataProvider(name = "sass-spec/media_import")
	public static Iterator<Object[]> specMediaImport() throws IOException {
		return createCollection("media_import").iterator();
	}

	@DataProvider(name = "sass-spec/misc")
	public static Iterator<Object[]> specMisc() throws IOException {
		return createCollection("misc").iterator();
	}

	@DataProvider(name = "sass-spec/number-functions")
	public static Iterator<Object[]> specNumberFunctions() throws IOException {
		return createCollection("number-functions").iterator();
	}

	@DataProvider(name = "sass-spec/parser")
	public static Iterator<Object[]> specParser() throws IOException {
		return createCollection("parser").iterator();
	}

	@DataProvider(name = "sass-spec/scope")
	public static Iterator<Object[]> specScope() throws IOException {
		return createCollection("scope").iterator();
	}

	@DataProvider(name = "sass-spec/scss")
	public static Iterator<Object[]> specScss() throws IOException {
		return createCollection("scss").iterator();
	}

	@DataProvider(name = "sass-spec/scss-tests")
	public static Iterator<Object[]> specScssTests() throws IOException {
		return createCollection("scss-tests").iterator();
	}

	@DataProvider(name = "sass-spec/selector-functions")
	public static Iterator<Object[]> specSelectorFunctions() throws IOException {
		return createCollection("selector-functions").iterator();
	}

	@DataProvider(name = "sass-spec/types")
	public static Iterator<Object[]> specTypes() throws IOException {
		return createCollection("types").iterator();
	}

	/**
	 * Create collection of SpecData from the following pattern:
	 * {spec dir}/{suite}/{test}/input.scss
	 *
	 * @param suiteName
	 * @return
	 * @throws IOException
	 */
	public static Collection<Object[]> createCollection(@NotNull final String suiteName) throws IOException {
		final List<SpecData> specs = createList(suiteName);
		final Collection<Object[]> params = new LinkedList<>();

		for (SpecData item : specs) {
			params.add(new Object[]{ item });
		}
		return params;
	}

	/**
	 * Create list of SpecData from the following pattern:
	 * {spec dir}/{suite}/{test}/input.scss
	 *
	 * @param suiteName
	 * @return
	 * @throws IOException
	 */
	public static List<SpecData> createList(@NotNull final String suiteName) throws IOException {
		final Path specPath = Paths.get(SpecData.SASS_SPEC_DIR, suiteName);
		return createList(suiteName, specPath);
	}

	public static List<SpecData> createList(@NotNull final String suiteName, @NotNull final Path specPath) throws IOException {
		Objects.requireNonNull(suiteName);
		Objects.requireNonNull(specPath);

		final List<SpecData> specs = new LinkedList<>();
		final SpecData spec = SpecData.create(suiteName);

		Files.walkFileTree(specPath, new SpecPathFileVisitor(specs, spec));

		Log.fine(format("Number of specs: %s for %s", specs.size(), suiteName));

		return specs;
	}
}
