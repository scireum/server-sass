# ----------------------------------------------------------------------------------------------------------------------
# BREW: Paths and Variables
# ----------------------------------------------------------------------------------------------------------------------
BREW_HOME ?= /usr/local
BREW_DIR  ?= $(BREW_HOME)/homebrew-temp
BREW_BIN  ?= $(BREW_HOME)/bin/brew

GIT_BIN  ?= /usr/local/bin/git
GIT_BIN  ?= /usr/bin/git



# ----------------------------------------------------------------------------------------------------------------------
# BREW: Install
# ----------------------------------------------------------------------------------------------------------------------
.PHONY: brew-init brew-install

$(BREW_DIR):
	sudo mkdir -p $(BREW_HOME)
	sudo chgrp admin $(BREW_HOME)
	sudo chmod g+rwx $(BREW_HOME)

	$(GIT_BIN) clone -b master ssh://source.devsnc.com/homebrew-now $@

brew-init: $(BREW_DIR)
brew-init:


