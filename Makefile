.PHONY: test cov ci check-build eastwood fmt outdated outdated-bump revcount graalvm-test

lint: fmt eastwood build-check
all: lint ci

test:
	clojure -X:test

cov:
	clojure -X:test:cloverage

ci:
	clojure -T:build ci :snapshot false

build-check:
	clojure -M:check

eastwood:
	clojure -M:eastwood

fmt:
	clojure -X:fmt:fmtfix

outdated:
	clojure -T:outdated

outdated-bump:
	clojure -T:outdated :upgrade true :force treu

revcount:
	git rev-list HEAD --count

# GraalVM native-image smoke test.
# Verifies the library works when compiled to a native binary.
# Requires GraalVM with native-image:
#   brew install --cask graalvm-jdk          (macOS)
#   sdk install java 21.0.7-graalce          (sdkman)
#   https://www.graalvm.org/downloads/       (manual)
GRAALVM_HOME ?= $(shell /usr/libexec/java_home -v "25" 2>/dev/null || /usr/libexec/java_home -v "21" 2>/dev/null || echo "")
NATIVE_IMAGE := $(if $(wildcard $(GRAALVM_HOME)/bin/native-image),$(GRAALVM_HOME)/bin/native-image,$(shell command -v native-image 2>/dev/null))

graalvm-test:
	@test -n "$(NATIVE_IMAGE)" || { echo "native-image not found. Install GraalVM: https://www.graalvm.org/downloads/"; exit 1; }
	mkdir -p classes
	JAVA_HOME=$(GRAALVM_HOME) clojure -Sdeps '{:paths ["src" "test"]}' -M -e "(compile 'org.pilosus.kairos-graalvm-test)"
	$(NATIVE_IMAGE) --no-fallback \
		-cp "$$(JAVA_HOME=$(GRAALVM_HOME) clojure -Sdeps '{:paths ["src" "test"]}' -Spath):classes" \
		--initialize-at-build-time \
		-H:Class=org.pilosus.kairos_graalvm_test \
		-o kairos-graalvm-test
	./kairos-graalvm-test
	rm -f kairos-graalvm-test
