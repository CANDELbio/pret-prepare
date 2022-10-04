# Developer Guide

This doc details steps necessary to successfully run, build, and deploy pret from this repo.

## Build version info edn file

Pret requires a version info edn file to run. Build it with:

```
make version-info
```


## Dependencies

This project depends on `datomic-pro`. to download this dependency, this project is configured to use the private
Maven repo at `my.datomic.com`. From your [my.datomic account](https://my.datomic.com/account) you can get your
Maven repo credentials. These will need to go into your `~/.m2/settings.xml` file, added as an entry to the
`servers`:

Notes: the XML header is required, and PASSWORD is the download key available on the my.datomic.com page, not the login password.
```
<servers>
  ...
  <server>
    <id>my.datomic.com</id>
    <username>USERNAME</username>
    <password>PASSWORD</password>
  </server>
</servers>
```

## Clojure Tools/Deps Notes

Note: for all following steps, the quickest sanity check on whether or not the Clojure Tools environment is configured correctly is running `deps` or starting a repl, via:

```clojure -Mdeps```

or:

```clojure```

### Installation

Not all packages will give you the latest version of Clojure or configure JVM, etc. for you. Your best bet is to follow the install instructions for your platform as described in the official
[Clojure docs](https://clojure.org/guides/getting_started).

## Re-caching the Schema

`pret` uses a cached schema for `prepare`. You should update the cached schema anytime you make changes to
the `schema.edn`, `metamodel.edn`, or `enums.edn` files. This can be done with the follow command line invocation:


```
clojure -M -m org.parkerici.pret.db.schema.cache
```

## Running Pret CLI

To call the pret CLI from the local environment in an arg for arg parity with the deployed version of the pret package, there is a convenience wrapper.

```
./pret-dev
```

This can be invoked as the pret CLI is, e.g.:

```
./pret-dev prepare ~/azure-datasets/template/config.edn ~/wds/template/
```

etc.

If you need to troubleshoot JVM args, etc. you can use the command invocation in this script as a starting point.

## Building and deploying

There are make targets for testing, caching the schema, and building a pret release.

```
make test
```

```
make uberjar
```

```
make package
```

