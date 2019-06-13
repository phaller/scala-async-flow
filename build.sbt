lazy val base = (project in file("base"))
  .settings(
    name := "scala-async-flow",
    scalaVersion := "0.16.0-RC3",
    parallelExecution in Test := false,
    libraryDependencies += "junit" % "junit" % "4.12" % "test",
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v", "-s"))
