package task

case class WordCountTask(text: String) extends Task {

  import WordCountTask._

  // A text is atomic if it does not have any whitespace
  // or if it has fewer than 1000 characters
  // Assumption: counting characters is much less expensive than counting words
  override val isAtomic: Boolean = !text.trim.exists(_.isWhitespace) || text.trim.length <= ATOMIC_LENGTH

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

object WordCountTask {
  val ATOMIC_LENGTH = 1000
}