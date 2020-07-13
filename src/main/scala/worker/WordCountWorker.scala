package worker
import akka.actor.{ActorRef, Props}
import result.{Result, WordCountResult}
import task.{Task, WordCountTask}
import task.Task.TaskTypeMisMatch

// Branching factor of a word count worker is 2 by default
// This because during the divide step, we only divide a text into 2 substrings
class WordCountWorker() extends Worker[WordCountTask, WordCountResult](2) {

  import WordCountTask._

  // We'll find an appropriate whitespace char
  // where we can split the string into 2 substrings
  // The whitespace char is the first whitespace after the first n chars from the left
  // We'll start out with n = ATOMIC_LENGTH
  // If we can't find any whitespace, divide n by 2 and try again
  // Keep trying until n = 0, i.e. we'll use the first whitespace encountered
  protected override def divide(task: Task): List[WordCountTask] = task match {
    case WordCountTask(text) =>

      @scala.annotation.tailrec
      def findN(str: String, currN: Int): Int =
        if (currN == 0) currN
        else if (str.substring(currN).exists(_.isWhitespace)) currN
        else findN(str, currN / 2)

      val trimmed = text.trim
      val n = findN(trimmed, ATOMIC_LENGTH)
      val index = trimmed.substring(n).indexWhere(_.isWhitespace) + n
      val taskA = WordCountTask(trimmed.substring(0, index).trim)
      val taskB = WordCountTask(trimmed.substring(index).trim)
      List(taskA, taskB)

    case _ => throw TaskTypeMisMatch(s"Expected word count task but found '${task.getClass}' task.")
  }

  protected override def perform(task: Task): WordCountResult = task match {
    case WordCountTask(text) =>
      // Split the text by whitespace regex
      val count = text.split("\\s+").count(!_.isEmpty)
      WordCountResult(count)

    case _ => throw TaskTypeMisMatch(s"Expected word count task but found '${task.getClass}' task.")
  }

  protected override def combine(resultA: Result, resultB: Result): WordCountResult =
    (resultA, resultB) match {
      case (WordCountResult(countA), WordCountResult(countB)) => WordCountResult(countA + countB)

      case _ =>
        val msg = s"Expected word count results but found '${resultA.getClass}' and '${resultB.getClass}'."
        throw TaskTypeMisMatch(msg)
    }

  protected override def createWorker(name: String): ActorRef = context.actorOf(Props[WordCountWorker], name)
}