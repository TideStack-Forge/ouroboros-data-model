# Public Forward Sync Dry Run Report

## Summary

- Status: passed
- Generated at: `2026-08-20T12:02:50.512607+00:00`
- Mode: `sync`
- Dry run flag: `False`
- Source ref recorded in public tree: `false`
- Manifest: `internal export manifest (not copied)`
- Manifest SHA-256: `4e14ff7412b973929966184f3b362455f03c2c2e8297e3527ce97bc6cb601604`
- Target: `public-tree-local-fixture`
- Remote actions: none

## Forward Sync Policy

- Policy: `manifest-scoped-forward-sync`
- Public history policy: `orphan-from-first-public-release`
- Internal commit messages copied: no
- Projection commit message: `Sync public data model projection`
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
ingual/target/jacoco.exec
[INFO] 
[INFO] <<< source:3.4.0:jar (default) < generate-sources @ ouroboros-root <<<
[INFO] 
[INFO] 
[INFO] --- source:3.4.0:jar (default) @ ouroboros-root ---
[INFO] 
[INFO] --- jacoco:0.8.15:report (report) @ ouroboros-root ---
[INFO] Skipping JaCoCo execution due to missing execution data file.
[INFO] 
[INFO] --------------------< com.ouroboros:ouroboros-data >--------------------
[INFO] Building Ouroboros Data 2.0.0-rc.1-SNAPSHOT                        [2/2]
[INFO]   from core/ouroboros-data/pom.xml
[INFO] --------------------------------[ pom ]---------------------------------
[INFO] 
[INFO] --- checkstyle:3.6.0:check (validate) @ ouroboros-data ---
[INFO] 开始检查……
检查完成。
[INFO] You have 0 Checkstyle violations.
[INFO] 
[INFO] --- enforcer:3.6.3:enforce (enforce-architecture) @ ouroboros-data ---
[INFO] Rule 2: org.apache.maven.enforcer.rules.dependency.BannedDependencies passed
[INFO] Rule 3: org.apache.maven.enforcer.rules.dependency.DependencyConvergence passed
[INFO] 
[INFO] >>> pmd:3.28.0:check (pmd-check) > :pmd @ ouroboros-data >>>
[INFO] 
[INFO] --- pmd:3.28.0:pmd (pmd) @ ouroboros-data ---
[INFO] Skipping org.apache.maven.plugins:maven-pmd-plugin:3.28.0:pmd report goal
[INFO] 
[INFO] <<< pmd:3.28.0:check (pmd-check) < :pmd @ ouroboros-data <<<
[INFO] 
[INFO] 
[INFO] --- pmd:3.28.0:check (pmd-check) @ ouroboros-data ---
[INFO] 
[INFO] --- jacoco:0.8.15:prepare-agent (prepare-agent) @ ouroboros-data ---
[INFO] argLine set to -javaagent:$HOME/.m2/repository/org/jacoco/org.jacoco.agent/0.8.15/org.jacoco.agent-0.8.15-runtime.jar=destfile=$PUBLIC_TREE/core/ouroboros-data/target/jacoco.exec
[INFO] 
[INFO] --- resources:3.3.1:copy-resources (copy-metadata-sources) @ ouroboros-data ---
[INFO] skip non existing resourceDirectory $PUBLIC_TREE/core/ouroboros-data/src/main/metadata
[INFO] 
[INFO] --- flatten:1.7.3:flatten (flatten) @ ouroboros-data ---
[INFO] Generating flattened POM of project com.ouroboros:ouroboros-data:pom:2.0.0-rc.1-SNAPSHOT...
[INFO] 
[INFO] --- gplus:5.1.0:compileTests (default) @ ouroboros-data ---
[INFO] No sources specified for compilation. Skipping.
[INFO] 
[INFO] --- jacoco:0.8.15:report (report) @ ouroboros-data ---
[INFO] Skipping JaCoCo execution due to missing execution data file.
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for Ouroboros Root 2.0.0-rc.1-SNAPSHOT:
[INFO] 
[INFO] Ouroboros Root ..................................... SUCCESS [ 58.841 s]
[INFO] Ouroboros Data ..................................... SUCCESS [  9.535 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:23 min
[INFO] Finished at: 2026-08-20T20:00:28+08:00
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
NFO] 开始检查……
检查完成。
[INFO] You have 0 Checkstyle violations.
[INFO] 
[INFO] --- enforcer:3.6.3:enforce (enforce-architecture) @ ouroboros-data-pkgen-coding ---
[INFO] Rule 2: org.apache.maven.enforcer.rules.dependency.BannedDependencies passed
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
[INFO] Ouroboros Root ..................................... SUCCESS [ 13.575 s]
[INFO] Ouroboros Data ..................................... SUCCESS [  1.352 s]
[INFO] Ouroboros Data Core ................................ SUCCESS [01:00 min]
[INFO] Ouroboros Data Typed Core .......................... SUCCESS [  5.789 s]
[INFO] Ouroboros Data Plugins ............................. SUCCESS [  9.764 s]
[INFO] Ouroboros Data Typed Meta Processor ................ SUCCESS [  2.091 s]
[INFO] Ouroboros Data SQL Migration ....................... SUCCESS [  2.540 s]
[INFO] Ouroboros Data SQL ................................. SUCCESS [ 10.261 s]
[INFO] ouroboros-data-builders ............................ SUCCESS [  2.147 s]
[INFO] ouroboros-data-test-support ........................ SUCCESS [  1.881 s]
[INFO] ouroboros-data-pkgen-coding ........................ SUCCESS [  6.869 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:00 min
[INFO] Finished at: 2026-08-20T20:02:49+08:00
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
