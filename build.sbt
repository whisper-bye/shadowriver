name := "ShadowRiver"

scalaVersion := "2.11.8"

androidBuild

platformTarget in Android := "android-23"

run <<= run in Android

addCommandAlias("apk", ";clean;run")

val supportVersion="23.1.1"

libraryDependencies ++= Seq(
  aar("com.android.support" %  "design" % supportVersion),
  aar("com.android.support" %  "cardview-v7" % supportVersion),
  aar("com.android.support" % "support-v4" % supportVersion),
  aar("com.android.support" % "appcompat-v7" % supportVersion),
  aar("com.android.support" % "recyclerview-v7" % supportVersion)
)


//val akkaVersion = "2.4.7"
val akkaVersion = "2.3.15"
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"
//libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3"
//libraryDependencies += "com.hierynomus" % "sshj" % "0.15.0"
//libraryDependencies += "org.scodec" %% "scodec-protocols" % "1.0.0-M2a"
libraryDependencies += "org.scodec" %% "scodec-core" % "1.10.1"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
//libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.4.7"
//libraryDependencies += "org.scodec" % "scodec-bits_2.11" % "1.1.0"


//useProguard := true
//proguardScala in Android := true
dexMaxHeap in Android := "2g"

useProguardInDebug in Android := false
proguardScala in Android := false
dexMulti in Android := true

//dexMulti in Android := true
javacOptions ++= Seq("-source", "1.7", "-target", "1.7")
scalacOptions ++= Seq("-feature", "-deprecation", "-target:jvm-1.7")

// Generic ProGuard rules
proguardOptions in Android ++= Seq(
  "-ignorewarnings",
  "-keep class scala.Option",
  "-keep class scala.Function1",
  "-keep class scala.PartialFunction",
  "-keep class scala.Dynamic"
)

// ProGuard rules for Akka
proguardOptions in Android ++= Seq(
  "-keep class akka.** { *; }",
  "-keep class akka.actor.Actor$class { *; }",
  "-keep class akka.actor.LightArrayRevolverScheduler { *; }",
  "-keep class akka.actor.LocalActorRefProvider { *; }",
  "-keep class akka.actor.CreatorFunctionConsumer { *; }",
  "-keep class akka.actor.TypedCreatorFunctionConsumer { *; }",
  "-keep class akka.dispatch.BoundedDequeBasedMessageQueueSemantics { *; }",
  "-keep class akka.dispatch.UnboundedMessageQueueSemantics { *; }",
  "-keep class akka.dispatch.UnboundedDequeBasedMessageQueueSemantics { *; }",
  "-keep class akka.dispatch.DequeBasedMessageQueueSemantics { *; }",
  "-keep class akka.dispatch.MultipleConsumerSemantics { *; }",
  "-keep class akka.actor.LocalActorRefProvider$Guardian { *; }",
  "-keep class akka.actor.LocalActorRefProvider$SystemGuardian { *; }",
  "-keep class akka.dispatch.UnboundedMailbox { *; }",
  "-keep class akka.actor.DefaultSupervisorStrategy { *; }",
  "-keep class akka.event.Logging$LogExt { *; }"
)

proguardOptions in Android ++= Seq(
  "-keep class im.xun.shadowriver.** { *; }",
  "-keep interface im.xun.shadowriver.** { *; }"
)

val cleanTask = TaskKey[Unit]("clean", "remove target folders")
cleanTask := {
  println("remove target/ folders")
  "find . -name target" #| "xargs rm -fr" !
}
protifySettings
