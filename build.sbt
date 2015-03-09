lazy val rssreader = project.in(file(".")).
  aggregate(simple, reactive)

lazy val simple = project.in(file("READER/simple"))

lazy val reactive = project.in(file("READER/reactive"))
