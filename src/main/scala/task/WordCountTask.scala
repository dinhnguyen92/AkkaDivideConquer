package task

case class WordCountTask(text: String) extends Task {
  // A text is atomic if it does not have any whitespace
  override val isAtomic: Boolean = !text.trim.exists(_.isWhitespace)

  // If the text is not empty,
  // display up to first 10 characters
  override val summary: String = {
    val trimmed = text.trim
    val maxLength = 10
    if (trimmed.isEmpty) "Empty string"
    else if (trimmed.length <= maxLength) trimmed
    else trimmed.substring(0, maxLength) + "..."
  }
}
