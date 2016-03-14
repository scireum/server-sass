# ----------------------------------------------------------------------------------------------------------------------
# JDK: Paths and Variables
# ----------------------------------------------------------------------------------------------------------------------

JAVA_VERSION ?= 1.8.0_60-b27
JAVA_HOME    ?= /usr/lib/jvm/java-8-oracle
JAVA         ?= $(JAVA_HOME)/bin/javac
JAVAC        ?= $(JAVA_HOME)/bin/javac

export JAVA_HOME