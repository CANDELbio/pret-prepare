# pret

`pret` is a programmable ETL pipeline for the CANDEL database.
The input to `pret` is a collection of input data in flat tsv files, and a specification (in [edn](https://github.com/edn-format/edn) format) of how this data maps to the CANDEL schema.
Using this input, `pret` will:

1. parse the data according to the specification in the config file
2. produce a directory consisting of entity data in EDN files and matrix data in TSV blobs.

This directory serves as a common interface point for CANDEL transaction backends to use to import data into different databases, such as Datomic Cloud or On-Prem.


TODO: new CircleCI badge for prepare repo.

## Rationale

Pret provides a data-driven and declarative "no code" solution to a common ETL workflow: mapping data from tabular form into entity maps.
Pret's intended end-users are data scientists, who prepare and provide tabular data, and annotate this in an _edn_ file describing how this tabular data should be mapped into a schema.
Pret uses a `schema` (derived from Datomic's representation) and `metamodel` -- a set of annotations concerning entity kinds and how they relate to each other -- and performs this mapping in an entirely data-driven processing path.
Because this is all data-driven, Pret can be adapted to different schemas (and even entirely new domains) without code changes.
Only the data files defining the schema and metamodel need to be changed.


Pret was developed at [PICI](https://www.parkerici.org/) as part of the CANDEL database to automate several tedious and frequently repeated data science workflows.
Because any schema is a moving target (especially in its early phases), Pret was written generically against the CANDEL schema as data.
This allowed a small team at PICI informatics to quickly adapt to the changing landscape of biological data.
This has included things like supporting new assays (and entire categories of assays, like spatially resolved single cell data) as well as  changing constraints around relations between biological entities.
The majority of downstream functionality has used this data-driven approach to regenerate constraints, visualizations, queries, and data analysis code to align with schema changes.
This has reduced the time to e.g. support new assays from weeks to hours.

Pret originally consolidated multiple CANDEL functions, including prepare (current functionality), transact, and validate.
Transact and validate in their original unified implementation both used and depended on Datomic.
These pieces have since been broken out to allow for alternate data store backends to be supported.
For the time being, the default backend used at PICI and partner orgs continues to be Datomic On-Prem.
The transaction path for Datomic from the entity maps is fairly simple, they merely need to be broken into batches.
Pret also uses Datomic at dev time to consolidate the schema and metamodel in a common representation; this is the basis for building the indexed form of the schema the prepare path uses.

The more nuanced pieces of Pret's processing handle the tedious and error-prone process of resolving references between known entity kinds (reference data) and other elements of the same dataset.
Reference data in the biological domain includes things like gene and protein ontologies (e.g. HGNC and Uniprot) as well as disease types and drugs.
Whenever an ontology or controlled vocabulary with widespread adoption is available, we defer to it for defining entity kinds, and recommend this path for anything defined as reference data in the Pret schema.
Other dataset elements include patients, samples, clinical observations, and timepoints, among other things.
Pret resolves context specific identifiers to unique text IDs. For example:

```
tcga-paad/patient-01/sample-01-timepoint-3
tcga-paad/rna-seq/batch-1/
```

Because transaction order matters for references to other entities in many data stores, 
Pret uses the metamodel's annotations to guarantee a transaction ordering that ensures any referenced entity will be transacted before the entities that refer to it.
This ordering is encoded in the lexicographical sort of the `XX-priority-` prefix of files generated in Pret's working directory.
Transaction backend implementers should use ascending string sort to determine which order files should be transacted in to accommodate reference resolution.
All files with identical levels of `XX-priority-` can be transacted in parallel.
For some attributes, note that entity identity resolution might result in one entity's attributes being transacted across several batches of transactions in different files.
Downstream consumers of data produced b Pret and validations that check Pret for referential integrity should ensure that an entire dataset or batch of prepared data has been transacted before assigning semantic value to the presence or absence of attributes on entities.


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
