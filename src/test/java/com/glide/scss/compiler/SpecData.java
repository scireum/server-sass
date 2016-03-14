package com.glide.scss.compiler;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SpecData {

	static final String SASS_SPEC_DIR = System.getProperty("sass.spec.dir", "sass-spec/spec");
	static final String INPUT_SCSS_FILENAME = System.getProperty("sass.spec.input.filename", "input.scss");
	static final String EXPECTED_CSS_FILENAME = System.getProperty("sass.spec.expected.css.filename", "expected.compact.css");
	static final String GLOB_PATTERN = System.getProperty("sass.spec.glob.pattern", "*");

	private final Path fRootPath;
	private final Path fPath;

	private final String fName;
	private final String fPattern;
	private final String fInputFileName;
	private final String fExpectedFileName;

	SpecData(final Path rootPath, final Path target, final String pattern,
	         final String inputFileName, final String expectedFileName) {

		Path path = target;
		if (target.endsWith(inputFileName))
			path = path.getParent();

		fRootPath = (rootPath == null) ? Paths.get(SASS_SPEC_DIR) : rootPath;

		fName = path.getFileName().toString();
		fPath = path;
		fPattern = pattern;
		fInputFileName = inputFileName;
		fExpectedFileName = expectedFileName;
	}

	static SpecData create(final String path) {
		return create(null, Paths.get(path));
	}

	static SpecData create(final Path path) {
		return create(null, path);
	}

	static SpecData create(final Path rootPath, final Path path) {
		return create(rootPath, path, GLOB_PATTERN);
	}

	static SpecData create(final Path rootPath, final Path path, final String pattern) {
		return new SpecData(rootPath, path, pattern, INPUT_SCSS_FILENAME, EXPECTED_CSS_FILENAME);
	}

	static SpecData create(final String path, final String pattern) {
		return create(null, Paths.get(path), pattern);
	}

	Path getRootPath() {
		return fRootPath;
	}

	Path getPath() {
		return fPath;
	}

	String getName() {
		return fName;
	}

	String getExpectedFileName() {
		return fExpectedFileName;
	}

	String getPattern() {
		return fPattern;
	}

	String getInputFileName() {
		return fInputFileName;
	}

	@Override
	public String toString() {
		return fName;
	}
}
