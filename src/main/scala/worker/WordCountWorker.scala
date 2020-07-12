package worker
import akka.actor.{ActorRef, Props}
import result.{Result, WordCountResult}
import task.{Task, WordCountTask}
import task.Task.TaskTypeMisMatch

// Branching factor of a word count worker is 2 by default
// This because during the divide step, we only divide a text
// into 2 substrings: 1 atomic word, and the remaining substring
class WordCountWorker() extends Worker[WordCountTask, WordCountResult](2) {

  // Simply find the index of the first whitespace char and use it
  // to split the string into an atomic word and a substring
  protected override def divide(task: Task): List[WordCountTask] = task match {
    case WordCountTask(text) =>
      val index = text.strip.indexWhere(_.isWhitespace)
      val taskA = WordCountTask(text.substring(0, index).trim)
      val taskB = WordCountTask(text.substring(index).trim)
      List(taskA, taskB)

    case _ => throw TaskTypeMisMatch(s"Expected word count task but found '${task.getClass}' task.")
  }

  protected override def perform(task: Task): WordCountResult = task match {
    // A word count task is only performed when it is atomic
    // i.e when the text in the task is either an atomic word or empty
    case WordCountTask(text) =>
      if (text.isEmpty) WordCountResult(0)
      else WordCountResult(1)

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
