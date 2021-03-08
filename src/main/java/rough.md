To regenerate tables from `doc_assets`: https://www.tablesgenerator.com/markdown_tables#

To build AAR: (AAR is located under - build/outputs/aar/FileX-release.aar)
```
export JAVA_HOME="$HOME/android-studio/jre/"
./gradlew assembleRelease -xtest -xlint
```

