server-sass
======

A simple implementation of a compiler which transforms SASS to CSS files - without requiring you to include jRuby,
JavaScript or other monsters.

The idea is to only implement those features of SASS, which we really need - but have these in superior quality.
Therefore we have a test coverage of 80%+ of all lines using automated tests. If there is a missing feature,
feel free to fork ;-)

For a good description on SASS go to: http://sass-lang.org

Using it is as simple as:

```java
Generator gen = new Generator();
// Parses and expands test.scss - Files are resolved from the classpath, subclass Generator and
// override the resolve method, to change this behavior.
gen.importStylesheet("test.scss");
// Finalize expansions and evaluate expressions
gen.compile();
// Access css sources
String css = gen.toString();
```

For your convenience: A pre-built jar can be found in the build directory.

If you also need a modern, rock solid web framework which has this module already built in, have
a look at SIRIUS: https://github.com/scireum/sirius

server-sass is part of the open source initiative of scireum GmbH (http://www.scireum.de)

## Maven

server-sass is available under:

    <dependency>
      <groupId>com.scireum</groupId>
      <artifactId>server-sass</artifactId>
      <version>1.0</version>
    </dependency>

