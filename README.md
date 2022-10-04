# pret Overview

`pret` is a programmable ETL pipeline for the CANDEL database. The input to `pret` is a collection of input data in flat tsv files, and a specification (in [EDN](https://github.com/edn-format/edn) format) of how this data maps to the CANDEL schema. Using this input, `pret` will:

1. parse the data according to the specification in the config file
2. produce a directory consisting of entity data in EDN files and matrix data in TSV blobs.

This directory serves as a common interface point for CANDEL transaction backends to use to import data into different databases, such as Datomic Cloud or On-Prem.

## Test Status

TODO: new CircleCI badge for prepare repo.

NOTE: The above status badge reflects the master branch ONLY. All other branches must be checked via the CircleCI console.

## Environment setup and installation

### Prerequisites

`pret` requires the following installed:
1. Java version 1.11 See [OpenJDK](https://openjdk.java.net/install/) for installation instructions.

## Running

Invoke `pret` in the directory you downloaded the `pret.jar` file with:

`./pret` in linux or macOS

`pretw.bat` in Windows

This will echo the command line usage options

### Example usage

The following example would provision a test database as specified in `~/repos/pret/example-data/datomic.conf.edn`, prepare data as specified in `~/repos/pret-datasets/tcga/config.edn` to the working directory `~/data/tcga-import/tmp-working`, and then transact the data into the database provisioned in the `provision` task.

```./pret prepare --import-config ~/repos/pret-datasets/tcga/config.edn --working-directory ~/data/tcga-import/tmp-working```

### Prepare

For detailed notes on the prepare process, see the [docs.](docs/prepare.md)

# Developer Notes

For developer notes see [DEVELOPMENT.md](DEVELOPMENT.md).

# Copyright

Copyright the Parker Institute for Cancer Immunotherapy. Released under terms of the [Apache License](LICENSE.md).
