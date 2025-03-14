ThisBuild / version := "0.1.0-SNAPSHOT"  // Or "0.1.0" for a release

ThisBuild / scalaVersion := "3.6.4"

lazy val root = (project in file("."))
  .settings(
    name                := "library", // CHANGED NAME
    resolvers           += "Bitwig Maven Repository" at "https://maven.bitwig.com",

    assembly / assemblyJarName := "library.jar",

    libraryDependencies += "com.bitwig"    % "extension-api" % "20" % "provided",
    libraryDependencies += "org.json"      % "json"          % "20231013",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % "test", //Added scalatest for unit test.

    logLevel := Level.Error,
    semanticdbEnabled := true,
    scalacOptions ++= Seq(
      "-deprecation",
      "-Wunused:all"
    )
  )
lazy val assemblySettings = Seq(
  assembly / assemblyJarName := "uber.jar",
)
scalacOptions ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) =>
      List(
        "-Yrangepos",
        "-P:semanticdb:synthetics:on",
        "-P:semanticdb:text:on"
      )
    case Some((3, _)) =>
      List(
        "-Xsemanticdb",
        "-sourcetype:tasty"
      )
    case _ =>
      List()
  }
}
