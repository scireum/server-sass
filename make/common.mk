# ----------------------------------------------------------------------------------------------------------------------
# Common: Variables
# ----------------------------------------------------------------------------------------------------------------------
MKFILE_PATH := $(abspath $(firstword $(MAKEFILE_LIST)))
CWD := $(dir $(MKFILE_PATH))

GIT   ?= $(call findpath,$(PATH),git)
LN    ?= $(call findpath,$(PATH),ln)
LS    ?= $(call findpath,$(PATH),ls) -la
MKDIR ?= $(call findpath,$(PATH),mkdir) -p
RM    ?= $(call findpath,$(PATH),rm)
TAR   ?= $(call findpath,$(PATH),tar)
WGET  ?= $(call findpath,$(PATH),wget)

JAVA  ?= $(call findpath,$(PATH),java)

ARCH  := x64
UNAME := $(shell uname | tr A-Z a-z)

NO_COLOR    := \x1b[0m
OK_COLOR    := \x1b[32;01m
ERROR_COLOR := \x1b[31;01m
WARN_COLOR  := \x1b[33;01m

OK_STRING=$(OK_COLOR)[OK]$(NO_COLOR)
ERROR_STRING=$(ERROR_COLOR)[ERRORS]$(NO_COLOR)
WARN_STRING=$(WARN_COLOR)[WARNINGS]$(NO_COLOR)

CLEAN ?=
DEBUG ?= 1

# ----------------------------------------------------------------------------------------------------------------------
# Common: Functions
# ----------------------------------------------------------------------------------------------------------------------
findpath   = $(call first,$(call map,wildcard,$(call addsuffix,/$2,$(call split,:,$1))))

# ----------------------------------------------------------------------------------------------------------------------
# Common: Rules
# ----------------------------------------------------------------------------------------------------------------------
.PHONY: concat clean silent

FILES  ?=
OUTPUT ?= /dev/null

concat: $(FILES)
ifneq (,$(wildcard $(FILES)))
	cat $^ > $(OUTPUT)
endif

silent:
	@:

# ----------------------------------------------------------------------------------------------------------------------
# Debug: Rules
# ----------------------------------------------------------------------------------------------------------------------
.PHONY: help-targets variables

# Helper: make print-SRC would print the variable
print-%:
	@echo $* = $($*)

help-targets:
	@$(MAKE) -qp | awk -F':' '/^[a-zA-Z0-9][^$$#\/\t=]*:([^=]|$$)/ {split($$1,A,/ /); for(i in A)print A[i]}' | sort | column

variables:
	$(MAKE) -pn | grep -A1 "^# makefile"| grep -v "^#\|^--" | sort | uniq

# ----------------------------------------------------------------------------------------------------------------------
# SSH: Rules
# ----------------------------------------------------------------------------------------------------------------------
.PHONY: ssh-init

ssh-init: |
	@rm -rf $(HOME)/.ssh/known_hosts

	@ssh-keyscan -H github.com >> $(HOME)/.ssh/known_hosts
	@ssh-keyscan -H stash.zephyr-intranet.com >> $(HOME)/.ssh/known_hosts

	@ssh -T git@github.com
	@ssh -T git@stash.zephyr-intranet.com

	@cat $(HOME)/.ssh/known_hosts
