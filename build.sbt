lazy val base = (project in file("base"))
  .settings(
    name := "scala-async-flow",
    scalaVersion := "2.12.3",
    parallelExecution in Test := false,
    scalacOptions += "-feature",
    libraryDependencies += "org.reactivestreams" % "reactive-streams" % "1.0.2",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.7-SNAPSHOT")

lazy val samples = (project in file("samples"))
  .settings(
    name := "scala-async-flow-samples",
    scalaVersion := "2.12.3",
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
    libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.7-SNAPSHOT")
  .dependsOn(base)
