# ----------------------------------------------------------------------------------------------------------------------
# Ansible: Paths and Variables
# ----------------------------------------------------------------------------------------------------------------------
PYTHON_PATH ?= /usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
PIP = $(call findpath,$(PYTHON_PATH),pip)

ANSIBLE_DIR        ?= $(SCRIPTS_DIR)/ansible
ANSIBLE_GALAXY     ?= /usr/local/bin/ansible-galaxy
ANSIBLE_PLAYBOOK   ?= /usr/local/bin/ansible-playbook
ANSIBLE_INVENTORY  ?= hosts/local.yml
ANSIBLE_FILE       ?= playbook.yml
ANSIBLE_HOSTS      ?= localhost
ANSIBLE_GROUP      ?=
ANSIBLE_LIMIT      ?=
ANSIBLE_OPTS       ?=
#ANSIBLE_OPTS       ?= --verbose

TAGS       ?=
CONNECTION ?= ssh
EXTRA_VARS ?=

# group and host in inventory file
ENV_HOST  ?= localhost
ENV_GROUP ?= localhost
LIMIT     ?= localhost

ANSIBLE_OPTS += --limit=$(LIMIT)
ANSIBLE_INVENTORY := hosts/$(HOSTS).yml

EXTRA_VARS += HOSTS=$(HOSTS)
EXTRA_VARS += TARGET=$(LIMIT)
EXTRA_VARS += ENV_GROUP=$(ENV_GROUP)
EXTRA_VARS += ENV_HOST=$(ENV_HOST)


# ----------------------------------------------------------------------------------------------------------------------
# Ansible: Install
# ----------------------------------------------------------------------------------------------------------------------
.PHONY: role ansible-init ansible-test ansible

role-%:
	$(ANSIBLE_GALAXY) init ansible/roles/$*


ansible-init:
	sudo $(PIP) install ansible

ansible-test: ANSIBLE_OPTS += --check --list-hosts
ansible-test:
	$(MAKE) --silent ansible ANSIBLE_OPTS="$(ANSIBLE_OPTS)"

ansible:
	@mkdir -p $(ANSIBLE_DIR)/logs
	cd $(ANSIBLE_DIR) && $(ANSIBLE_PLAYBOOK) $(ANSIBLE_OPTS) -i $(ANSIBLE_INVENTORY) $(ANSIBLE_FILE) \
	--extra-vars "$(strip $(EXTRA_VARS))" --tags=$(TAGS)
