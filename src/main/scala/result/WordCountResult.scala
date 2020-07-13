package result

case class WordCountResult(wordCount: Int) extends Result {
  override val summary: String = s"Count: $wordCount"
}
