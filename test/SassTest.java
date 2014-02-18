import org.junit.Assert;
import org.junit.Test;
import org.serversass.Generator;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;

/**
 * Created with IntelliJ IDEA.
 * User: aha
 * Date: 18.02.14
 * Time: 17:01
 * To change this template use File | Settings | File Templates.
 */
public class SassTest {
    @Test
    public void testVariables() {
        compare("variables.scss", "variables.css");
    }

    @Test
    public void testNesting() {
        compare("nesting.scss", "nesting.css");
    }

    @Test
    public void testImport() {
        compare("import.scss", "import.css");
    }

    @Test
    public void testMixin() {
        compare("mixin.scss", "mixin.css");
    }

    @Test
    public void testExtends() {
        compare("extends.scss", "extends.css");
    }

    @Test
    public void testOperators() {
        compare("operators.scss", "operators.css");
    }

    @Test
    public void testMedia() {
        compare("media.scss", "media.css");
    }

    private void compare(String scssFile, String cssFile) {
        try {
            Generator gen = new Generator();
            gen.importStylesheet(scssFile);
            gen.compile();

            StringWriter writer = new StringWriter();
            InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/" + cssFile));
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
            String result = gen.toString();

            String[] expectedLines = expected.split("\n");
            String[] resultLines = result.split("\n");
            for (int i = 0; i < expectedLines.length; i++) {
                String exp = expectedLines[i];
                String res = resultLines.length > i ? resultLines[i] : "";

                if (!exp.equals(res)) {
                    Assert.fail(String.format("%s - Line %d: '%s' vs '%s'", scssFile, i + 1, exp, res));
                }
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

    }
}
