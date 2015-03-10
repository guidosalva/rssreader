lazy val rssreader = project.in(file(".")).
  aggregate(simple, reactive, events)

lazy val simple = project.in(file("READER/simple"))

lazy val reactive = project.in(file("READER/reactive"))

lazy val events = project.in(file("READER/events"))
