import sbt._
import Keys._

import com.typesafe.sbtscalariform.ScalariformPlugin
import ScalariformPlugin.{ format, formatPreferences }

object setakBuild extends Build {
  
  lazy val core = Project("setak",
                          file("."),
                          settings = coreSettings)  

    val coreSettings = Defaults.defaultSettings ++ ScalariformPlugin.settings ++ Seq(
    scalaVersion := "2.9.1",
    crossScalaVersions := Seq("2.9.0-1", "2.9.1"),
    name := "setak",
    organization := "edu.illinois",
    version := "1.0-SNAPSHOT",
    resolvers += "Akka Repo" at "http://akka.io/repository",
    libraryDependencies ++= Seq("se.scalablesolutions.akka" % "akka" % "1.2-RC6",
                                "junit"                   % "junit"               % "4.5",
                                "org.scalatest"           % "scalatest_2.9.0"     % "1.6.1"),
    parallelExecution in Test := false,
    formatPreferences in Compile := formattingPreferences,
    formatPreferences in Test :=  formattingPreferences,
    publishTo <<= (version) { version: String =>
      val repo = (s: String) =>
        Resolver.ssh(s, "mir.cs.uiuc.edu", "/mounts/mir/disks/0/marinov/tasharo1/www/setak/" + s + "/") as ("tasharo1", file("C:/Users/tasharo1/Desktop/keymir.ppk")) withPermissions("0644")
        
        //Resolver.ssh(s, "sal.cs.uiuc.edu", "/home/cs/tasharo1/public_html/setak/" + s + "/") as("tasharo1") withPermissions("0644")
      Some(if (version.trim.endsWith("SNAPSHOT")) repo("snapshots") else repo("releases"))
    })

  
  val formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
    .setPreference(RewriteArrowSymbols, true)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
  }

}