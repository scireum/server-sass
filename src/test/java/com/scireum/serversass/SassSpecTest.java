package com.scireum.serversass;

/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

import com.scireum.serversass.Generator;
import com.scireum.serversass.Output;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Tests the SASS to CSS compiler
 */
public class SassSpecTest {

    @Test
    public void scssRegressionTest() throws URISyntaxException {
        URL specUrl = getClass().getResource("/spec");
        scanFiles(new File(specUrl.getFile()));
    }

    private void scanFiles(File spec) throws URISyntaxException {
        for (File child : spec.listFiles()) {
            if (child.isDirectory()) {
                scanFiles(child);
            } else if ("input.scss".equals(child.getName())) {
                check(child);
            }
        }
    }

    private void check(final File scssFile) {
        try {
            Generator gen = new Generator(scssFile.getParentFile()) {
                @Override
                public void warn(String message) {
                    System.err.println(message);
                }

            };

            gen.importStylesheet(scssFile.getName());
            gen.compile();

            StringWriter writer = new StringWriter();
            FileReader reader = new FileReader(new File(scssFile.getParentFile(), "expected_output.css"));
            char[] buf = new char[8192];
            while (true) {
                int length = reader.read(buf);
                if (length < 0) {
                    break;
                }
                writer.write(buf, 0, length);
            }
            reader.close();

            String expected = writer.toString();
            StringWriter out = new StringWriter();
            Output output = new Output(out, false);
            gen.generate(output);
            String result = out.toString();

            String[] expectedLines = expected.split("\n");
            String[] resultLines = result.split("\n");
            for (int i = 0; i < expectedLines.length; i++) {
                String exp = expectedLines[i];
                String res = resultLines.length > i ? resultLines[i] : "";

                if (!exp.equals(res)) {
                    System.err.println(String.format("%s - Line %d: '%s' vs '%s'", scssFile, i + 1, exp, res));
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

}
