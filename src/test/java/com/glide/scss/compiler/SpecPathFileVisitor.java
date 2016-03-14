package com.glide.scss.compiler;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.logging.Logger;

import javax.validation.constraints.NotNull;

import static java.lang.String.format;

/**
 * @author pete.chanthamynavong
 */
class SpecPathFileVisitor extends SimpleFileVisitor<Path> {

	private static final Logger Log = Logger.getLogger(SpecPathFileVisitor.class.getName());
	private static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

	@NotNull private final List<SpecData> fSpecs;
	@NotNull private final String fGlob;
	@NotNull private final PathMatcher fMatcher;

	SpecPathFileVisitor(@NotNull final List<SpecData> specs, @NotNull final SpecData spec) {
		this(specs, Paths.get(SpecData.SASS_SPEC_DIR, spec.getName(), spec.getPattern(), spec.getInputFileName()));
	}

	SpecPathFileVisitor(@NotNull final List<SpecData> specs, @NotNull final Path glob) {
		this(specs, glob.toString());
	}

	SpecPathFileVisitor(@NotNull final List<SpecData> specs, @NotNull final String glob) {
		fSpecs = specs;
		fGlob = glob;
		fMatcher = FILE_SYSTEM.getPathMatcher("glob:" + fGlob);
		Log.info(format("glob: %s", glob));
	}

	@Override
	public FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
		if (dir.startsWith("sass-spec/spec/libsass")) {
			Log.info(format("exclude: %s", dir.toString()));
			return FileVisitResult.SKIP_SUBTREE;
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
		if (fMatcher.matches(file) && !attrs.isDirectory()) {
			Log.fine(format("%s, %s", file.getFileName().toString(), fMatcher.matches(file)));
			fSpecs.add(SpecData.create(file));
		}
		return FileVisitResult.CONTINUE;
	}
}
