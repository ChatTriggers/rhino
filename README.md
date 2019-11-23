# Rhino: JavaScript in Java

![Rhino](https://developer.mozilla.org/@api/deki/files/832/=Rhino.jpg)

Rhino is an implementation of JavaScript in Java.

## License

Rhino is licensed under the [MPL 2.0](./LICENSE.txt).

## Fork

This fork was created and is being actively updated to increase the ES6 support of Rhino. You can track the progress [here](https://chattriggers.github.io/rhino/).

This is first and foremost a fun project, but we would like our version to be usable. That being said, there are some limitations that we acknowledge and will not attempt to support or fix:

- Rhino's XML system
- Any non-standard Rhino extension, such as function expressions
- Any Javascript version before ES6

## Building

### How to Build

Rhino builds with `Gradle`. Here are some useful tasks:

```
./gradlew jar
```

Build and create `Rhino` jar in the `buildGradle/libs` directory.

```
git submodule init
git submodule update
./gradlew test
```

Build and run all the tests.

```
./gradlew testBenchmark
```

Build and run benchmark tests.

## Running

Rhino can run as a stand-alone interpreter from the command line:

```
java -jar buildGradle/libs/rhino-1.7.11.jar -debug -version 200
Rhino 1.7.9 2018 03 15
js> print('Hello, World!');
Hello, World!
js>
```

There is also a "rhino" package for many Linux distributions as well as Homebrew for the Mac.

You can also embed it, as most people do. See below for more docs.
