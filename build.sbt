name := "snowplow_exercise"
 
version := "1.0" 
      
lazy val snowplowExercise = (project in file(".")).enablePlugins(PlayScala)

      
resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"
      
scalaVersion := "2.13.5"

libraryDependencies += guice
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.20.13-play28",
  "org.reactivemongo" %% "reactivemongo-play-json-compat" % "1.0.1-play28",
)
libraryDependencies += "com.github.java-json-tools" % "json-schema-validator" % "2.2.14"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
      