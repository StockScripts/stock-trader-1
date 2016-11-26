name := "stock-trader"

version := "1.0"

scalaVersion := "2.11.8"

flywayDriver := "org.postgresql.Driver"

flywayUrl := s"jdbc:postgresql://${sys.env.getOrElse("DATABASE_URL", "null")}:${sys.env.getOrElse("DATABASE_PORT", 5432)}/${sys.env.getOrElse("DATABASE_NAME", "null")}"

flywayUser := s"${sys.env.getOrElse("DATABASE_USER", "null")}"

flywayPassword := s"${sys.env.getOrElse("DATABASE_PASSWORD", "null")}"

updateOptions := updateOptions.value.withCachedResolution(true)

libraryDependencies ++= Seq(
  "com.github.tototoshi" %% "scala-csv" % "1.3.3",
  "io.getquill" %% "quill-jdbc" % "1.0.0",
  "org.postgresql" % "postgresql" % "9.4.1208",
  "com.typesafe.play" %% "play-ws" % "2.4.3",
  "org.scala-lang.modules" %% "scala-async" % "0.9.5",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "com.twitter" % "util-eval_2.11" % "6.38.0"
)

enablePlugins(JavaAppPackaging)

compile := ((compile in Compile) dependsOn flywayMigrate).value
