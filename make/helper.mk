# ----------------------------------------------------------------------------------------------------------------------
# Helper
# ----------------------------------------------------------------------------------------------------------------------
.PHONY: print-vars print-features

# Helper: make print-SRC would print the variable
print-%:
	@echo $* = $($*)

print-vars: | silent
	@$(MAKE) -pn | grep -A1 "^# makefile"| grep -v "^#\|^--" | sort | uniq

print-features:
	@echo $(.FEATURES)
