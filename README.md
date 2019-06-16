# scala-async-flow

This branch requires a build of OpenJDK which includes Project Loom. See the OpenJDK wiki for Project Loom build instructions. To enable the usage of your custom build of OpenJDK, start `sbt` using the `-java-home` flag passing the corresponding build directory. Example (paths need to be adjusted accordingly):

```
$ sbt -java-home '../loom/build/macosx-x86_64-server-release/images/jdk'
```

## Building

```
$ sbt
> project base
> compile
```

## Testing

The test suite is run
as follows:

```
$ sbt
> project base
> test
```
