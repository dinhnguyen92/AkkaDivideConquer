package worker

import akka.actor.{ActorRef, LoggingFSM, PoisonPill, Props}
import akka.routing.{Broadcast, RoundRobinGroup}
import result.Result
import task.Task

object Worker {
  // Worker events
  sealed trait WorkerEvent
  case class Assignment(task: Task) extends WorkerEvent
  case object Execute extends WorkerEvent
  case class TaskReport(result: Result) extends WorkerEvent

  // Worker states
  sealed trait WorkerState
  case object Idle extends WorkerState
  case object OnStandby extends WorkerState
  case object AggregatingResults extends WorkerState

  // Worker data
  sealed trait WorkerData
  case object NoWork extends WorkerData
  case class Workload(tasks: List[Task]) extends WorkerData
  case class DelegatedWorkload(inProgressWorkers: Set[ActorRef],
                               originalWorkGiver: ActorRef,
                               results: List[Result]) extends WorkerData
}

abstract class Worker[T <: Task, R <: Result](val branchingFactor: Int)
  extends LoggingFSM[Worker.WorkerState, Worker.WorkerData] {

  import Worker._

  protected def divide(task: Task): List[T]
  protected def perform(task: Task): R
  protected def combine(resultA: Result, resultB: Result): R
  protected def createWorker(name: String): ActorRef

  protected def createChildWorkers(workerCount: Int): IndexedSeq[ActorRef] =
    for (i <- 1 to workerCount) yield createWorker(s"child_$i")

  startWith(Idle, NoWork)

  when(Idle) {
    case Event(Assignment(task), NoWork) =>
      log.debug("Going to OnStandBy")
      goto(OnStandby) using Workload(List(task))
  }

  when(OnStandby) {

    case Event(Assignment(task), Workload(tasks)) =>
      log.debug("Receiving more task in OnStandBy")
      stay using Workload(tasks :+ task)

    case Event(Execute, Workload(tasks)) =>
      // If there's only one atomic task
      // Perform the task and report the result
      if (tasks.length == 1 && tasks.head.isAtomic) {
        log.debug("Performing atomic task")
        val result = perform(tasks.head)
        sender ! TaskReport(result)
        goto(Idle) using NoWork
      }
      else {
        // If there's only 1 divisible task, split it up
        // Otherwise, simply triage all of the assigned tasks
        val tasksToTriage =
          if (tasks.length == 1) divide(tasks.head)
          else tasks

        // Spawn and monitor the child workers
        // The number of child workers is either the branching factor
        // or the number of tasks, whichever is smaller
        val workerCount = math.min(branchingFactor, tasksToTriage.length)
        val childWorkers = createChildWorkers(workerCount)
        childWorkers.foreach(context.watch)

        // Use a round robin strategy to route tasks to child workers
        val childPaths = childWorkers.map(_.path.toString)
        val workerGroup = context.actorOf(RoundRobinGroup(childPaths).props())
        tasksToTriage.foreach(workerGroup ! Assignment(_))

        // Once all of the tasks have been routed,
        // Tell all child workers to execute them
        workerGroup ! Broadcast(Execute)

        goto(AggregatingResults) using DelegatedWorkload(childWorkers.toSet, sender, List())
      }
  }

  when(AggregatingResults) {

    case Event(TaskReport(result), DelegatedWorkload(inProgressWorkers, originalWorkGiver, results)) =>
      // Remove the child worker from the in-progress group and kill it
      val remainingWorkers = inProgressWorkers - sender
      sender ! PoisonPill

      val resultsSoFar = results :+ result

      if (remainingWorkers.isEmpty) {
        // If results from all workers have been collected
        // Compute the aggregate result and report back to original work giver
        val aggregateResult = resultsSoFar.reduceLeft(combine)
        originalWorkGiver ! TaskReport(aggregateResult)

        goto(Idle) using NoWork
      }
      else {
        // If there are still in-progress workers
        // Keep waiting for their reports
        stay using DelegatedWorkload(inProgressWorkers, originalWorkGiver, resultsSoFar)
      }
  }
}


