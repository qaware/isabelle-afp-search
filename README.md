<p align="center">
  <img title="FindFacts" src="search-webapp/public/images/android-chrome-384x384.png" width="200px" />
</p>

[![CircleCI](https://circleci.com/gh/qaware/isabelle-afp-search/tree/master.svg?style=svg)](https://circleci.com/gh/qaware/isabelle-afp-search/tree/master)

# isabelle-afp-search
Project to make Isabelle and the AFP easily searchable. Structured in:
- **common**: common modules
- **search**: search application, with core module (**search-core**), web application (**search-webapp**), and frontend ui (**search-webapp-ui**).
- **importer**: importer pipeline to import Isabelle `dump` into search index

## Usage
Requirements: `java` 11

### Importer Isabelle tool

#### From source
1. Check out and `cd` into repo
2. Check out, initialize and build **Isabelle** submodule
   ```shell
   git submodule init
   git submodule update
   ci-scripts/install-isabelle
   ```
   
3. Register `importer-isabelle` external component (for local installation, use your own `settings` file)
   ```shell
   echo 'init_component "path/to/isabelle-afp-search/importer-isabelle/target/scala-2.12"' >> isabelle/etc/settings
   ```
   
4. Run
    ```shell
    ./sbt "project importer-isabelle" "run <OPTIONS>"
   ```
    Use `-?` to get information about the tool usage. Example invocation:
    ```shell
   ./sbt "project importer-isabelle" "run ../dump localhost 8983" 
   ``` 

#### Using local Isabelle installation
1. Download published artifact (TODO publish)
2. Add Isabelle component (to Isabelle `etc/settings` file):
   ```shell
   init_component /path/to/download/folder
   ```
3. Run
   ```env
   isabelle dump_importer -?
   ```

### Search webapp
Run:
```shell
./sbt "project search-webapp" run
```

Build and publish docker image:
```shell
./sbt "project search-webapp" "docker:publish"
```

For deployment, see the [deployment repo](https://github.com/qaware/findfacts-deployment).

## Code style
This project uses the [databricks style guide](https://github.com/databricks/scala-style-guide) with some changes:

- __column width__: use 120.
- __implicits__: Only avoid them outside of well-known patterns, such as type-classes, implicit context, and pimp-my-library.
- __monadic chaining__: Use for-comprehensions to easily chain monads in an understandable and readable way.
- __multiple parameter lists__: Use multiple parameter list for partially applicable functions or to improve type inference.

(Most) formatting is automated via scalafmt.

The `importer-isabelle` submodule instead adheres to the Isabelle code style.