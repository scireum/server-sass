# ----------------------------------------------------------------------------------------------------------------------
# Notes
# ----------------------------------------------------------------------------------------------------------------------
# $@ 	The name of the current target.
# $?	The list of dependencies newer than the target.
# $<	The name of the dependency file, as if selected by make for use with an implicit rule.
# $*	The base name of the current target (the target name stripped of its suffix).
# $%	the name of the item/object being processed

# $^ gives you all dependencies, regardless of whether they are more recent than the target.
# Duplicate names, however, will be removed. This might be useful if you produce transient output.

# $+ is like $^, but it keeps duplicates and gives you the entire list of dependencies in the order they appear.

# Variable Assignment
# var  = value - Recursive substitution ($(var) expands to value, like a macro); this is the only portable assignment.
# var := value - Simple assignment ($(var) acts like a conventional variable)
# var ?= value - Conditional assignment (value is assigned to var only if var is not defined)
# var += value - Append (value is appended to var's current value)

# Syntax
#
# Rule: Dependencies
#  Actions

# ----------------------------------------------------------------------------------------------------------------------
# Prologue
# ----------------------------------------------------------------------------------------------------------------------

MAKE  := /usr/bin/make
SHELL := /bin/bash

.SHELLFLAGS := -eu -o pipefail -c
.DEFAULT_GOAL := all
.SUFFIXES:
.DELETE_ON_ERROR:

# Allow % to match multiple directories.
percent_subdirs := 1


# ----------------------------------------------------------------------------------------------------------------------
# Make Version Check
# ----------------------------------------------------------------------------------------------------------------------
MAKE_VERSION := $(shell $(MAKE) --version)
ifneq ($(firstword $(MAKE_VERSION)),GNU)
$(error Use GNU Make)
endif


# ----------------------------------------------------------------------------------------------------------------------
# Variables
# ----------------------------------------------------------------------------------------------------------------------
MKFILE_PATH := $(abspath $(lastword $(MAKEFILE_LIST)))
CURRENT_DIR := $(notdir $(patsubst %/,%,$(dir $(MKFILE_PATH))))
WORKING_DIR := $(abspath $(dir $(MKFILE_PATH)))

# ----------------------------------------------------------------------------------------------------------------------
# Alias
# ----------------------------------------------------------------------------------------------------------------------
SCRIPTS_DIR ?= .
CURL        ?= curl
RM          ?= rm
RSYNC       ?= rsync
MKDIR       ?= mkdir
ECHO        ?= echo -e
GIT         ?= git

# ----------------------------------------------------------------------------------------------------------------------
# Java Variables and Settings
# ----------------------------------------------------------------------------------------------------------------------
JAVA_HOME   ?= $(shell /usr/libexec/java_home)
JAVA_HOME   ?= $(wildcard /Library/Java/JavaVirtualMachines/jdk1.8.0_73.jdk/Contents/Home)
JAVA_HOME   ?= /usr/lib/jvm/java-8-oracle
export JAVA_HOME

# ----------------------------------------------------------------------------------------------------------------------
# Maven Variables and Settings
# ----------------------------------------------------------------------------------------------------------------------
MAVEN_OPTS  ?=
MAVEN_OPTS  += -T 1C
MAVEN_OPTS  += --batch-mode
MAVEN_OPTS  += --fail-fast

ifneq (,$(PROJECT))
MAVEN_OPTS += -pl $(PROJECT)
endif

ifneq (,$(OFFLINE))
MAVEN_OPTS += --offline
endif

ifneq (,$(DEBUG))
MAVEN_OPTS += -X
endif

# ----------------------------------------------------------------------------------------------------------------------
# Includes: Common and Project
# ----------------------------------------------------------------------------------------------------------------------
include ./make/gmsl
include ./make/helper.mk
include ./make/common.mk
include ./make/jdk.mk
include ./make/maven.mk

INCLUDE_FILES ?=
include $(INCLUDES)

# ----------------------------------------------------------------------------------------------------------------------
# Project: Rules
# ----------------------------------------------------------------------------------------------------------------------

# clean rules
# ----------------------------------------------------------------------------------------------------------------------
.PHONY: clean
clean:
	$(MAVEN) $(MAVEN_OPTS) clean


# compile rules
# ----------------------------------------------------------------------------------------------------------------------
.PHONY: compile compile-sp process-classes process-resources
compile:
	$(MAVEN) $(MAVEN_OPTS) compile process-classes process-resources

# test rules
# ----------------------------------------------------------------------------------------------------------------------
.PHONY: test-spec test-ss

test-spec: MAVEN_OPTS += -DtestGroup="com/glide/scss/compiler"
test-spec: MAVEN_OPTS += -Dsass.spec.dir="sass-spec/spec/"
test-spec: MAVEN_OPTS += -Dsass.spec.pattern="$(SPEC_DIR)"
test-spec:
	$(MAVEN) $(MAVEN_OPTS) test

test-ss: MAVEN_OPTS += -DtestGroup="org/serversass"
test-ss:
	$(MAVEN) $(MAVEN_OPTS) test
