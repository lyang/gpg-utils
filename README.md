# gpg-utils [![Build Status](https://travis-ci.com/lyang/saml-proxy.svg?branch=master)](https://travis-ci.com/lyang/saml-proxy) [![Maven Central](https://img.shields.io/maven-central/v/com.github.lyang/gpg-utils.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.lyang%22%20AND%20a:%22gpg-utils%22)
`GPGUtils` is a simple wrapper for `ProcessBuilder` to make shell out to `gpg` easier in Java.

## What
It provides a simple interface for encrypting/decrypting `String`, `File` and `InputStream`.

There's also generic method to run other `gpg` command.

## Why
Existing solutions for using `gpg` in Java have mostly involved `BouncyCastle`. It works, but, its API isn't the nicest to use, to say the least.

Using `ProcessBuilder` is much simpler compared with `BouncyCastle`. But it's not easy to get IO buffering right for external processes.

`GPGUtils` tries to provide a simple interface while hiding away the complexity of IO buffer handling.

## How to use
Refer to the [Unit Tests](src/test/java/com/github/lyang/gpgutils/GPGUtilsTest.java) for now.
