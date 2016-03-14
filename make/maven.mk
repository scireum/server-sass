# ----------------------------------------------------------------------------------------------------------------------
# Maven: Paths and Variables
# ----------------------------------------------------------------------------------------------------------------------
MAVEN_VERSION  ?= 3.3.9
MAVEN_HOME     := $(wildcard /usr/local/homebrew/Cellar/maven/$(MAVEN_VERSION)/libexec)
MAVEN_HOME     ?= $(wildcard /usr/local/homebrew/Cellar/maven/3.1.1/libexec)

MAVEN          := $(wildcard $(MAVEN_HOME)/bin/mvn)
MAVEN          ?= $(call findpath,$(PATH),mvn)
ifeq (,$(MAVEN))
MAVEN          := mvn
endif

MVN            := $(MAVEN)

MAVEN_OPTS     ?=

ifneq (,$(PROJECT))
MAVEN_OPTS += -pl $(PROJECT)
endif

ifneq (,$(OFFLINE))
MAVEN_OPTS += --offline
endif

# ----------------------------------------------------------------------------------------------------------------------
# Maven: Rules
# ----------------------------------------------------------------------------------------------------------------------


# dependencies
# ----------------------------------------------------------------------------------------------------------------------
.PHONY: analyze go-offline resolve sources validate

# analyzes projects dependencies and lists mismatches between resolved dependencies
# and those listed in dependencyManagement
analyze: MAVEN_OPTS += -U
analyze:
	$(MAVEN) $(MAVEN_OPTS) dependency:tree dependency:analyze-dep-mgt

# resolve everything this project is dependent on (dependencies, plugins, reports) in preparation for going offline
go-offline:
	$(MAVEN) $(MAVEN_OPTS) dependency:go-offline

# resolve all dependencies and displays the version
resolve: MAVEN_OPTS += -U
resolve:
	$(MAVEN) $(MAVEN_OPTS) dependency:resolve

# resolve all dependencies and their source attachments, and displays the version
sources:
	$(MAVEN) $(MAVEN_OPTS) dependency:sources

validate:
	$(MAVEN) $(MAVEN_OPTS) validate

# versions
# ----------------------------------------------------------------------------------------------------------------------
.PHONY: versions-dependency-updates versions-plugin-updates

versions-dependency-updates:
	$(MAVEN) $(MAVEN_OPTS) versions:display-dependency-updates

versions-plugin-updates:
	$(MAVEN) $(MAVEN_OPTS) versions:display-plugin-updates

# purge
# ----------------------------------------------------------------------------------------------------------------------
.PHONY: purge-snapshot purge-all

purge-snapshot: MAVEN_OPTS += -U
purge-snapshot: MAVEN_OPTS += -DsnapshotsOnly=true
purge-snapshot: MAVEN_OPTS += -DactTransitively=false
purge-snapshot:
	$(MAVEN) $(MAVEN_OPTS) dependency:purge-local-repository

purge-all: MAVEN_OPTS += -DactTransitively=false
purge-all:
	$(MAVEN) $(MAVEN_OPTS) dependency:purge-local-repository
