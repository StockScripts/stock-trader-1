logLevel := Level.Warn

addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.0.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.4")

resolvers += "Flyway" at "https://flywaydb.org/repo"