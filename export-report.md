# Public Export Dry Run Report

## Summary

- Status: passed
- Generated at: `2026-11-15T08:36:21.736029+00:00`
- Mode: `dry-run`
- Dry run flag: `False`
- Source ref recorded in public tree: `false`
- Manifest: `internal export manifest (not copied)`
- Manifest SHA-256: `93ce24bc900d448bdc5ee19f29e4f1129956ccd1df5a59e086723c02231071e6`
- Target: `public-tree-local-fixture`
- Remote actions: none

## Forward Sync Policy

- Policy: `manifest-scoped-forward-sync`
- Public history policy: `orphan-from-first-public-release`
- Internal commit messages copied: no
- Projection commit message: `Initial public data model dry-run export`
- Target branch action: dry-run only
- Remote push / merge / publish / release / deploy: not performed

## Included Modules

- `core/ouroboros-data/ouroboros-data-core`
- `core/ouroboros-data/ouroboros-data-plugins`
- `core/ouroboros-data/ouroboros-data-typed-core`
- `core/ouroboros-data/ouroboros-data-typed-meta-processor`
- `core/ouroboros-data/ouroboros-data-sql`
- `core/ouroboros-data/ouroboros-data-sql-migration`
- `core/ouroboros-data/ouroboros-data-builders`
- `core/ouroboros-data/ouroboros-data-test-support`
- `core/ouroboros-data/ouroboros-data-pkgen-coding`

## Public Build Inputs

- `.gitignore`
- `pom.xml`
- `docs/examples/automation-checks/checkstyle.xml`
- `docs/examples/automation-checks/pmd-ruleset.xml`
- `bom`
- `core/pom.xml`
- `core/ouroboros-data/pom.xml`
- `libs/pom.xml`
- `libs/util/ouroboros-util`

## Public Dependency Guard

- Result: `passed`
- Allowed public artifacts:

- `ouroboros-bom`
- `ouroboros-data-builders`
- `ouroboros-data-core`
- `ouroboros-data-pkgen-coding`
- `ouroboros-data-plugins`
- `ouroboros-data-sql`
- `ouroboros-data-sql-migration`
- `ouroboros-data-test-support`
- `ouroboros-data-typed-core`
- `ouroboros-data-typed-meta-processor`
- `ouroboros-util`

- Out-of-scope internal dependencies:

- none

## Public Profile Build

- Mockito javaagent: `$HOME/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar`
- Result: `passed`

### Public profile smoke test

- Command: `mvn -Ppublic -pl core/ouroboros-data -am test`
- Exit code: `0`

#### stdout tail

```text
 from [public-central (https://repo.maven.apache.org/maven2/, default, releases+snapshots)]
[INFO] Artifact org.codehaus.plexus:plexus-xml:jar:3.0.2 is present in the local repository, but cached from a remote repository ID that is unavailable in current build context, verifying that is downloadable from [public-central (https://repo.maven.apache.org/maven2/, default, releases+snapshots)]
[INFO] Artifact org.apache.maven:maven-archiver:jar:3.6.6 is present in the local repository, but cached from a remote repository ID that is unavailable in current build context, verifying that is downloadable from [public-central (https://repo.maven.apache.org/maven2/, default, releases+snapshots)]
[INFO] Artifact org.codehaus.plexus:plexus-interpolation:jar:1.29 is present in the local repository, but cached from a remote repository ID that is unavailable in current build context, verifying that is downloadable from [public-central (https://repo.maven.apache.org/maven2/, default, releases+snapshots)]
[INFO] Artifact org.codehaus.plexus:plexus-xml:jar:3.0.2 is present in the local repository, but cached from a remote repository ID that is unavailable in current build context, verifying that is downloadable from [public-central (https://repo.maven.apache.org/maven2/, default, releases+snapshots)]
Downloading from public-central: https://repo.maven.apache.org/maven2/org/apache/maven/maven-archiver/3.6.6/maven-archiver-3.6.6.jar
Downloaded from public-central: https://repo.maven.apache.org/maven2/org/apache/maven/maven-archiver/3.6.6/maven-archiver-3.6.6.jar (0 B at 0 B/s)
Downloading from public-central: https://repo.maven.apache.org/maven2/org/codehaus/plexus/plexus-interpolation/1.29/plexus-interpolation-1.29.jar
Downloading from public-central: https://repo.maven.apache.org/maven2/org/codehaus/plexus/plexus-xml/3.0.2/plexus-xml-3.0.2.jar
Downloaded from public-central: https://repo.maven.apache.org/maven2/org/codehaus/plexus/plexus-xml/3.0.2/plexus-xml-3.0.2.jar (0 B at 0 B/s)
Downloaded from public-central: https://repo.maven.apache.org/maven2/org/codehaus/plexus/plexus-interpolation/1.29/plexus-interpolation-1.29.jar (0 B at 0 B/s)
[INFO] No sources specified for compilation. Skipping.
[INFO] 
[INFO] --- jacoco:0.8.15:report (report) @ ouroboros-data ---
[INFO] Skipping JaCoCo execution due to missing execution data file.
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for Ouroboros Root 2.0.0-rc.1-SNAPSHOT:
[INFO] 
[INFO] Ouroboros Root ..................................... SUCCESS [01:32 min]
[INFO] Ouroboros Data ..................................... SUCCESS [  4.544 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:08 min
[INFO] Finished at: 2026-11-15T16:35:17+08:00
[INFO] ------------------------------------------------------------------------

```

#### stderr tail

```text
NOTE: Picked up JDK_JAVA_OPTIONS: -javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar

```

### Manifest module package build

- Command: `mvn -Ppublic -pl core/ouroboros-data/ouroboros-data-core,core/ouroboros-data/ouroboros-data-plugins,core/ouroboros-data/ouroboros-data-typed-core,core/ouroboros-data/ouroboros-data-typed-meta-processor,core/ouroboros-data/ouroboros-data-sql,core/ouroboros-data/ouroboros-data-sql-migration,core/ouroboros-data/ouroboros-data-builders,core/ouroboros-data/ouroboros-data-test-support,core/ouroboros-data/ouroboros-data-pkgen-coding -am -DskipTests package`
- Exit code: `0`

#### stdout tail

```text
forcer.rules.dependency.BannedDependencies passed
[INFO] Rule 3: org.apache.maven.enforcer.rules.dependency.DependencyConvergence passed
[INFO] 
[INFO] >>> pmd:3.28.0:check (pmd-check) > :pmd @ ouroboros-data-pkgen-coding >>>
[INFO] 
[INFO] --- pmd:3.28.0:pmd (pmd) @ ouroboros-data-pkgen-coding ---
[WARNING] Unable to locate Source XRef to link to -- DISABLED
[WARNING] Unable to locate Source XRef to link to -- DISABLED
[INFO] PMD version: 7.17.0
[INFO] Rendering content with org.apache.maven.skins:maven-fluido-skin:jar:2.0.0-M9 skin
[INFO] 
[INFO] <<< pmd:3.28.0:check (pmd-check) < :pmd @ ouroboros-data-pkgen-coding <<<
[INFO] 
[INFO] 
[INFO] --- pmd:3.28.0:check (pmd-check) @ ouroboros-data-pkgen-coding ---
[INFO] 
[INFO] --- jacoco:0.8.15:prepare-agent (prepare-agent) @ ouroboros-data-pkgen-coding ---
[INFO] argLine set to -javaagent:$HOME/.m2/repository/org/jacoco/org.jacoco.agent/0.8.15/org.jacoco.agent-0.8.15-runtime.jar=destfile=$PUBLIC_TREE/core/ouroboros-data/ouroboros-data-pkgen-coding/target/jacoco.exec
[INFO] 
[INFO] <<< source:3.4.0:jar (default) < generate-sources @ ouroboros-data-pkgen-coding <<<
[INFO] 
[INFO] 
[INFO] --- source:3.4.0:jar (default) @ ouroboros-data-pkgen-coding ---
[INFO] Building jar: $PUBLIC_TREE/core/ouroboros-data/ouroboros-data-pkgen-coding/target/ouroboros-data-pkgen-coding-2.0.0-rc.1-SNAPSHOT-sources.jar
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for Ouroboros Root 2.0.0-rc.1-SNAPSHOT:
[INFO] 
[INFO] Ouroboros Root ..................................... SUCCESS [  4.387 s]
[INFO] Ouroboros Util Bundle .............................. SUCCESS [ 30.851 s]
[INFO] Ouroboros Data ..................................... SUCCESS [  0.265 s]
[INFO] Ouroboros Data Core ................................ SUCCESS [ 16.414 s]
[INFO] Ouroboros Data Typed Core .......................... SUCCESS [  0.819 s]
[INFO] Ouroboros Data Plugins ............................. SUCCESS [  1.585 s]
[INFO] Ouroboros Data Typed Meta Processor ................ SUCCESS [  0.392 s]
[INFO] Ouroboros Data SQL Migration ....................... SUCCESS [  1.764 s]
[INFO] Ouroboros Data SQL ................................. SUCCESS [  1.799 s]
[INFO] ouroboros-data-builders ............................ SUCCESS [  0.373 s]
[INFO] ouroboros-data-test-support ........................ SUCCESS [  1.135 s]
[INFO] ouroboros-data-pkgen-coding ........................ SUCCESS [  0.495 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:01 min
[INFO] Finished at: 2026-11-15T16:36:21+08:00
[INFO] ------------------------------------------------------------------------

```

#### stderr tail

```text
NOTE: Picked up JDK_JAVA_OPTIONS: -javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar

```

## History Policy

- Internal `.git` copied: no
- Public history: generated locally by this script after projection
- Internal `.codestable` copied: no

## Pkgen Projection Guard

- `data-pkgen-coding` projected: `true`
- Forbidden matches: `0`

- none

## Public Metadata, CI, And Release Gates

- README generated: `true`
- English README generated: `true`
- Apache-2.0 LICENSE generated: `true`
- NOTICE generated: `true`
- CONTRIBUTING generated: `true`
- English CONTRIBUTING generated: `true`
- Public CI template generated: `true`
- API docs entry generated: `true`
- English API docs entry generated: `true`
- Adapter integration guide generated: `true`
- English adapter integration guide generated: `true`
- Minimal example generated: `true`
- English minimal example generated: `true`
- Release gate report: `release-gate-report.md`
- Release gate result: `passed`
- Sensitive content matches: `0`
- Forbidden public docs matches: `0`

## Cleanup

- Build output directories removed after build: yes
- Ignored private/runtime directories: `.git`, `.codestable`, `target`, `tmp`, logs, node_modules, AI worktrees
