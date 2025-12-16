.PHONY: test cov ci check-build eastwood fmt outdated outdated-bump revcount

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
