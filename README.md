# scala-async-flow

This library component depends on the extensions to scala-async
implemented in branch
[`async-flow`](https://github.com/phaller/async/tree/async-flow) at
[phaller/async](https://github.com/phaller/async). A build of the
`async-flow` branch must be published locally (command `publishLocal`
in sbt) before building scala-async-flow.

## Building

```
$ sbt
> project base
> compile
```

## Testing

The test suite (based on [ScalaTest](http://www.scalatest.org)) is run
as follows:

```
$ sbt
> project base
> test
```
