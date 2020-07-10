import akka.actor.{Actor, ActorLogging, ActorRef, FSM, Props}
import akka.routing.{ActorRefRoutee, Broadcast, RoundRobinGroup, RoundRobinRoutingLogic, Router}

object Worker {
  // Worker events
  sealed trait WorkerEvent
  case class Assignment(task: Task) extends WorkerEvent
  case object Execute extends WorkerEvent
  case class TaskReport(result: TaskResult) extends WorkerEvent

  // Worker states
  sealed trait WorkerState
  case object Idle extends WorkerState
  case object OnStandby extends WorkerState
  case object AggregatingResults extends WorkerState

  // Worker data
  sealed trait WorkerData
  case object NoWork extends WorkerData
  case class Workload(tasks: List[Task], directManager: ActorRef) extends WorkerData
  case class AggregatedResult(workerManager: ActorRef, results: List[TaskResult]) extends WorkerData
}

abstract class Worker(val branchingFactor: Int)
  extends FSM[Worker.WorkerState, Worker.WorkerData]
  with ActorLogging {

  import Worker._

  protected def divide(task: Task): List[Task]
  protected def perform(task: Task): TaskResult
  protected def aggregate(resultA: TaskResult, resultB: TaskResult): TaskResult

  protected def spawnChildWorkers(workerCount: Int): IndexedSeq[ActorRef] =
    for (i <- 1 to workerCount) yield context.actorOf(Props[Worker], s"child_$i")


  startWith(Idle, NoWork)

  when(Idle) {
    case Event(Assignment(task), NoWork) => goto(OnStandby) using Workload(List(task), sender)
  }

  when(OnStandby) {

    case Event(Assignment(task), Workload(tasks, directManager)) =>
      if (sender != directManager) throw new RuntimeException("Worker can only have one direct manager")
      stay using Workload(tasks :+ task, directManager)

    case Event(Execute, Workload(tasks, directManager)) =>
      if (sender != directManager) throw new RuntimeException("Worker can only have one direct manager")

      // If there's only one atomic task
      // Perform the task and report the result
      if (tasks.length == 1 && tasks.head.isAtomic) {
        val result = perform(tasks.head)
        directManager ! TaskReport(result)
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
        val childWorkers = spawnChildWorkers(workerCount)
        childWorkers.foreach(context.watch)

        // Use a round robin strategy to route tasks to child workers
        val childPaths = childWorkers.map(_.path.toString)
        val workerManager = context.actorOf(RoundRobinGroup(childPaths).props())
        tasksToTriage.foreach(workerManager ! Assignment(_))

        // Once all of the tasks have been routed,
        // Tell all child workers to execute them
        workerManager ! Broadcast(Execute)

        goto(AggregatingResults) using AggregatedResult(workerManager, List())
      }
  }

  when(AggregatingResults) {

    case Event(TaskReport(result), AggregatedResult(workerManager, results)) => ???
  }
}


