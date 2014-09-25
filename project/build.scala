import bintray.Plugin._
import bintray.Keys._
import com.typesafe.sbt.SbtScalariform.defaultScalariformSettings
import sbt.Keys._
import sbt.{ExclusionRule, _}

import scala.scalajs.sbtplugin.ScalaJSPlugin.ScalaJSKeys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._

import Deps._

object BuildSettings {

  val logger = ConsoleLogger()

  val buildSettings = Defaults.defaultSettings ++ publicationSettings ++ defaultScalariformSettings  ++ Seq (
    organization := "org.w3",
    version      := "0.7-SNAPSHOT",
    scalaVersion := "2.11.2",
    crossScalaVersions := Seq("2.11.2", "2.10.4"),
    javacOptions ++= Seq("-source","1.7", "-target","1.7"),
    fork := false,
    parallelExecution in Test := false,
    offline := true,
    // TODO
    testOptions in Test += Tests.Argument("-oD"),
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-optimize", "-feature", "-language:implicitConversions,higherKinds", "-Xmax-classfile-name", "140", "-Yinline-warnings"),
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    resolvers += "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
    resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
    description := "RDF framework for Scala",
    startYear := Some(2012),
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <url>https://github.com/w3c/banana-rdf</url>
      <developers>
        <developer>
          <id>betehess</id>
          <name>Alexandre Bertails</name>
          <url>http://bertails.org</url>
        </developer>
        <developer>
          <id>bblfish</id>
          <name>Henry Story</name>
          <url>http://bblfish.net/</url>
        </developer>
        <developer>
          <id>antoniogarrote</id>
          <name>Antonio Garrote</name>
          <url>https://github.com/antoniogarrote/</url>
        </developer>
      </developers>
      <scm>
        <url>git@github.com:w3c/banana-rdf.git</url>
        <connection>scm:git:git@github.com:w3c/banana-rdf.git</connection>
      </scm>
    ),
    licenses += ("W3C", url("http://opensource.org/licenses/W3C"))
  )

  //sbt -Dbanana.publish=bblfish.net:/home/hjs/htdocs/work/repo/
  //sbt -Dbanana.publish=bintray
  def publicationSettings =
    (Option(System.getProperty("banana.publish")) match {
      case Some("bintray") => Seq(
        // bintray
        repository in bintray := "banana-rdf",
        bintrayOrganization in bintray := None
      ) ++ bintrayPublishSettings
      case opt: Option[String] => {
        Seq(
          publishTo <<= version { (v: String) =>
            val nexus = "https://oss.sonatype.org/"
            val other = opt.map(_.split(":"))
            if (v.trim.endsWith("SNAPSHOT")) {
              val repo = other.map(p => Resolver.ssh("banana.publish specified server", p(0), p(1) + "snapshots"))
              repo.orElse(Some("snapshots" at nexus + "content/repositories/snapshots"))
            } else {
              val repo = other.map(p => Resolver.ssh("banana.publish specified server", p(0), p(1) + "resolver"))
              repo.orElse(Some("releases" at nexus + "service/local/staging/deploy/maven2"))
            }
          }
        )
      }
    }) ++ Seq( publishArtifact in Test := false)

//  val jenaTestWIPFilter = Seq (
//    testOptions in Test += Tests.Argument("-l", "org.w3.banana.jenaWIP")
//  )
//
//  val sesameTestWIPFilter = Seq (
//    testOptions in Test += Tests.Argument("-l", "org.w3.banana.sesameWIP")
//  )

}

object BananaRdfBuild extends Build {

  import BuildSettings._
  import Deps._

  // rdfstorew settings
//  skip in ScalaJSKeys.packageJSDependencies := false
  
  val pub = TaskKey[Unit]("pub")

  lazy val root = Project(
    id = "root",
    base = file("."),
    settings = buildSettings ++ Unidoc.settings ++ Seq(
      pub := (),
      pub <<= pub.dependsOn(publish in rdf, publish in jena, publish in sesame)
    ),
    aggregate = Seq(
      rdf,
      rdfTestSuite,
      jena,
      sesame,
      plantain,
      contribJodatime,
      examples
    )
  )
  
  lazy val rdf = Project(
    id = "rdf",
    base = file("rdf"),
    settings = buildSettings ++ Seq(
      libraryDependencies += scalaz,
      publishMavenStyle := true
    )
  )

//  lazy val rdf_js = Project(
//    id = "banana-rdf_js",
//    base = file("rdf/rdf_js"),
//    settings = buildSettings ++ testDeps ++ scalaJsDeps ++ Seq(
//      target := target.value / "js",
//      publishMavenStyle := true
//    )
//  ).dependsOn(rdf_common_js % "compile->compile;test->test")
//
//  lazy val rdf_common_js = Project(
//    id = "banana-rdf_common_js",
//    base = file("rdf"),
//    settings = buildSettings ++ testDeps ++ scalaJsDeps ++ Seq(
//      libraryDependencies += scalaz_js,
//      libraryDependencies += jodaTime,    //Will not work --- pure java
//      libraryDependencies += jodaConvert, //Will not work --- pure java
//      //target :=  "rdf/target",
//      target := target.value / "js",
//      publishMavenStyle := true
//    )
//  )

  lazy val contribJodatime = Project(
    id = "contrib-jodatime",
    base = file("contrib/jodatime"),
    settings = buildSettings ++ Seq(
      libraryDependencies += jodaTime,
      libraryDependencies += jodaConvert,
      libraryDependencies += scalatest % "test",
      publishMavenStyle := true
    )
  ).dependsOn(rdf, jena % "test")

  lazy val ldpatch = Project(
    id = "ldpatch",
    base = file("ldpatch"),
    settings = buildSettings ++ Seq(
      publishMavenStyle := true,
      libraryDependencies += parboiled2,
      // this will be needed until parboiled 2.0.1 gets released
      // see https://github.com/sirthias/parboiled2/issues/84#
      libraryDependencies <++= scalaVersion {
        case "2.11.2" => Seq("org.scala-lang" % "scala-reflect" % "2.11.2")
        case _ => Seq.empty
      },
      libraryDependencies += scalatest % "test"
    )
  ) dependsOn (rdf, jena, rdfTestSuite % "test")

  lazy val rdfTestSuite = Project(
    id = "rdf-test-suite",
    base = file("rdf-test-suite"),
    settings = buildSettings ++ Seq(
      libraryDependencies += scalatest
    )
  ) dependsOn (rdf)

//  lazy val rdfTestSuiteJS = Project(
//    id = "banana-scalajs-rdf-test-suite",
//    base = file("rdf-test-suite/rdf-test-suite_js"),
//    settings = buildSettings ++  scalaJSSettings ++ Seq(
//      libraryDependencies += scalatest,
//      libraryDependencies += "org.scala-lang.modules.scalajs" %% "scalajs-jasmine-test-framework" % scalaJSVersion
//    )
//  ) dependsOn (rdf_js)

  lazy val jena = Project(
    id = "jena",
    base = file("jena"),
    settings = buildSettings ++ Seq(
      resolvers += "apache-repo-releases" at "http://repository.apache.org/content/repositories/releases/",
      libraryDependencies += jenaLibs,
      libraryDependencies += logback,
      libraryDependencies += aalto
    )
  ) dependsOn (rdf, rdfTestSuite % "test")
  
  lazy val sesame = Project(
    id = "sesame",
    base = file("sesame"),
    settings = buildSettings ++ Seq(
      libraryDependencies += sesameQueryAlgebra,
      libraryDependencies += sesameQueryParser,
      libraryDependencies += sesameQueryResult,
      libraryDependencies += sesameRioTurtle,
      libraryDependencies += sesameRioRdfxml,
      libraryDependencies += sesameSailMemory,
      libraryDependencies += sesameSailNativeRdf,
      libraryDependencies += sesameRepositorySail
    )
  ) dependsOn (rdf, rdfTestSuite % "test")

  lazy val plantain = Project(
    id = "plantain",
    base = file("plantain"),
    settings = buildSettings ++  Seq(
      libraryDependencies += sesameRioTurtle,
      libraryDependencies += "com.typesafe.akka" %% "akka-http-core-experimental" % "0.4"
    )
  ) dependsOn (rdf, rdfTestSuite % "test")

//  lazy val pome = Project(
//    id = "banana-pome",
//    base = file("pome"),
//    settings =   buildSettings ++ testDeps ++ scalaJSSettings ++ Seq(
//      resolvers += "bblfish.net" at "http://bblfish.net/work/repo/releases/",
//      libraryDependencies += "com.github.japgolly.fork.scalaz" %%% "scalaz-core" % "7.1.0"
//    )
//  ) dependsOn (rdf_js, rdfTestSuite % "test", rdfTestSuiteJS % "test")

//  lazy val rdfstorew = Project(
//    id = "banana-rdfstorew",
//    base = file("rdfstorew"),
//    //settings =  buildSettings ++  testDeps ++ scalaJSSettings ++ Seq(
//    settings =  buildSettings ++ scalaJsDeps ++ Seq(
//    //settings =  scalaJSSettings ++ buildSettings ++ testDeps ++ Seq(
//      jsDependencies += ProvidedJS / "rdf_store.js",
//      jsDependencies += "org.webjars" % "momentjs" % "2.7.0" / "moment.js",
//      //resolvers += "bblfish.net" at "http://bblfish.net/work/repo/releases/",
//      libraryDependencies += scalaz_js,
//      skip in packageJSDependencies := false
//    )
//  ) dependsOn (rdf_js, rdfTestSuiteJS % "test")

  lazy val examples = Project(
    id = "examples",
    base = file("examples"),
    settings = buildSettings
  ) dependsOn (sesame, jena)

  // this is _experimental_
  // please do not add this projet to the main one
//  lazy val experimental = Project(
//    id = "experimental",
//    base = file("experimental"),
//    settings = buildSettings ++ testDeps ++ reactiveMongoDeps ++ sesameCoreDeps ++ Seq(
//      libraryDependencies += akka,
//      libraryDependencies += akkaTransactor,
//      libraryDependencies += iterateeDeps,
//      libraryDependencies += reactiveMongo,
//      libraryDependencies += playDeps,
//      libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.7" % "provided",
//      libraryDependencies += "log4j" % "log4j" % "1.2.16" % "provided"
//    )
//  ) dependsOn (rdfTestSuite % "test")
//
//  lazy val ldp = Project(
//    id = "ldp",
//    base = file("ldp"),
//    settings = buildSettings ++ testDeps ++ sesameCoreDeps ++ Seq(
//        libraryDependencies += akka,
//        libraryDependencies += asyncHttpClient,
//        libraryDependencies += akkaTransactor,
//        libraryDependencies += scalaz,
//        libraryDependencies += iterateeDeps,
//        libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.7" % "provided",
//        libraryDependencies += "log4j" % "log4j" % "1.2.16" % "provided"
//    )
//  ) dependsOn (rdfTestSuite % "test")

  
}

