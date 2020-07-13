import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.io.Source
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.language.postfixOps
import task.WordCountTask
import worker.WordCountWorker
import worker.Worker.{Assignment, Execute, TaskReport}
import result.WordCountResult

import scala.concurrent.Await

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

  // Assign the task to the worker
  worker ! Assignment(task)

  // Normally we use onComplete to asynchronously handle the worker's response
  // However, here we make this synchronous so that we can time the worker
  val workerStart = System.nanoTime

  // Tell the worker to execute the task and handle the result report
  implicit val timeout: Timeout = Timeout(5 second)
  val future = worker ? Execute
  val report = Await.result(future, atMost = 5 second)

  val workerStop = System.nanoTime

  report match {
    // Expected word count: 5552
    case TaskReport(WordCountResult(count)) =>
      println(s"Worker's final count: $count")
      println(s"Worker's time: ${(workerStop - workerStart) / 1e9d} seconds")
    case report => println(s"Unknown report of type '${report.getClass}'")
  }

  // Perform the word count synchronously to compare the performance
  val start = System.nanoTime
  val count = text.split("\\s+").count(!_.isEmpty)
  val stop = System.nanoTime

  println(s"Synchronous final count: $count")
  println(s"Synchronous time: ${(stop - start) / 1e9d} seconds")
}
