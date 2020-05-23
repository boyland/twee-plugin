default : test
.PHONY: build-plugin test default

VERSION=`grep 'Twee Plugin version' README.md | head -1 | sed 's/^Twee Plugin version \([0-9a-z._]*\).*/\1/'`.v`date +'%Y%m%d'`

test:
	@echo No tests defined for project yet in ${VERSION}

TESTBIN= bin/edu/uwm/twee/editors/TweeEditor.class
build-plugin : ${TESTBIN} README.md
	jar cmf META-INF/MANIFEST.MF edu.uwm.twee-plugin_${VERSION}.jar plugin.xml README.md icons/*.png -C bin .

${TESTBIN}:
	@echo Unable to compile Eclipse plugin code in Makefile.
	@echo Load project into Eclipse and build.
	@echo Then come back and make build-plugin
	false
		