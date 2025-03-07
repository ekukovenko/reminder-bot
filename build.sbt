ThisBuild / scalaVersion := "2.13.15"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "project",
    libraryDependencies ++= Seq(
      "com.bot4s" %% "telegram-core" % "5.8.3",
      "com.bot4s" %% "telegram-akka" % "5.8.3",
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC5",
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC5",
      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC5",
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "com.github.nscala-time" %% "nscala-time" % "2.32.0",
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.mockito" % "mockito-core" % "5.11.0" % Test,
      "org.mockito" %% "mockito-scala" % "1.17.37" % Test,
      "org.tpolecat" %% "doobie-h2" % "1.0.0-RC5" % Test,
      "com.h2database" % "h2" % "2.2.224" % Test,
    ),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) =>
        xs match {
          case ("MANIFEST.MF" :: Nil) => MergeStrategy.discard
          case ("INDEX.LIST" :: Nil)  => MergeStrategy.discard
          case ("DEPENDENCIES" :: Nil) => MergeStrategy.discard
          case _ => MergeStrategy.first
        }
      case _ => MergeStrategy.first
    }
  )
Compile / mainClass := Some("Main")

Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.ScalaLibrary