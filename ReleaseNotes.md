# Release notes

## pallet-crates-test-0.7.0

- Remove debug from logback configuration

- Add sonatype releases to ensure parent pom resolution

## pallet-crates-test-0.6.0

- Update pallet-crates-test to use logback

## pallet-crates-0.5.0

No changes

## pallet-crates-0.4.4

No changes

## pallet-crates-0.4.3

- Update versions, and add pallet-crates-test dependencies

- Update snapshot versions

- Add pallet-crates-test to provide a test environment
  When running tests in pallet crates, it is good to have a log4j.xml
  setup. This allows one log4j.xml to be used for all crates.
