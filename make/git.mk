# ----------------------------------------------------------------------------------------------------------------------
# Repo: PATHS and Variables
# ----------------------------------------------------------------------------------------------------------------------
# https://gerrit.googlesource.com/git-repo/+/master
# https://source.android.com/source/using-repo.html

GIT         ?= /usr/local/bin/git
GIT         ?= /usr/bin/git


REPO        ?= /usr/local/bin/repo
REPO_VERS   ?= 1.22
REPO_SHASUM ?= da0514e484f74648a890c0467d61ca415379f791

REPO_PORT   ?=
REPO_DOMAIN ?=
REPO_URL    ?=

REPO_DIR    ?= .repo
REPO_MFST   ?= $(REPO_DIR)/manifest.xml

# ----------------------------------------------------------------------------------------------------------------------
# Repo: Rules to manange repositories
# ----------------------------------------------------------------------------------------------------------------------
.PHONY: repo-init repo-sync repo-status repo-prune repo-checkout

$(REPO):
	sudo curl -o $@ https://storage.googleapis.com/git-repo-downloads/repo
	shasum $@ | grep $(REPO_SHASUM)
	sudo chmod a+x $@

$(REPO_MFST): $(REPO)
	$(RM) -rf $(REPO_DIR)
	$(REPO) init -u $(REPO_URL)

repo-init: $(REPO_MFST)

repo-sync: repo-init
	$(REPO) sync

repo-status:
	$(REPO) status

repo-prune:
	$(REPO) prune

# Rule for Bamboo, used to checkout revision
repo-checkout:
	$(GIT) checkout origin/$bamboo_planRepository_branchName
	$(GIT) checkout $bamboo_planRepository_revision
