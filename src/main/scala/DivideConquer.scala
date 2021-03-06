import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.io.Source
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import scala.language.postfixOps

import task.WordCountTask
import worker.WordCountWorker
import worker.Worker.{Assignment, Execute, TaskReport}
import result.WordCountResult

object DivideConquer extends App {

  // Details on how to read file in "resources" folder here:
  // https://stackoverflow.com/questions/31453511/how-to-read-a-text-file-using-relative-path-in-scala
  val source = Source.fromResource("FDR_inaugural_addresses.txt")

  // Details on how to efficiently read entire file here:
  // https://stackoverflow.com/questions/1284423/read-entire-file-in-scala/27518379
  val text = try source.getLines mkString "\n" finally source.close

  val system = ActorSystem("DivideConquerSystem")
  val worker = system.actorOf(Props[WordCountWorker], "rootWorker")
  val task = WordCountTask(text)

  implicit val timeout: Timeout = Timeout(5 second)

  worker ! Assignment(task)
  val future = worker ? Execute

  // Expected word count: 5552
  future.onComplete {
    case Success(TaskReport(WordCountResult(count))) => println(s"Final count: $count")
    case Failure(exception) => println(s"Failure: ${exception.getMessage}")
  }
}