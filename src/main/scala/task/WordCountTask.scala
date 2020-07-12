package task

case class WordCountTask(text: String) extends Task {

  import WordCountTask._

  // A text is atomic if it does not have any whitespace
  override val isAtomic: Boolean = !text.trim.exists(_.isWhitespace)
}

case object WordCountTask {
  val WHITESPACE_REGEX = "\\S+"
}
