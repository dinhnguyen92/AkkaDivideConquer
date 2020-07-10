import akka.actor.{Actor, ActorLogging, ActorRef, FSM}
import akka.routing.{RoundRobinRoutingLogic, Router}

object Worker {
  // Worker events
  sealed trait WorkerEvent
  case class Assignment(work: Work) extends WorkerEvent
  case object StartWork extends WorkerEvent
  case class WorkReport(result: WorkReport) extends WorkerEvent

  // Worker states
  sealed trait WorkerState
  case object AwaitingWork extends WorkerState
  case object AssignedWork extends WorkerState
  case object AggregatingWork extends WorkerState

  // Worker data
  sealed trait WorkerData
  case object NoWork extends WorkerData
  case class WorkLoad(work: Work, workGiver: ActorRef) extends WorkerData
}

abstract class Worker(val branchingFactor: Int)
  extends FSM[Worker.WorkerState, Worker.WorkerData]
  with ActorLogging {

  import Worker._

  startWith(AwaitingWork, NoWork)

  when(AwaitingWork) {
    case Event(Assignment(work), NoWork) => goto(AssignedWork) using WorkLoad(work, sender)
  }

  when(AssignedWork) {
    case Event(StartWork, WorkLoad(work, workGiver)) =>
      if (work.isAtomic) ??? // TODO: perform the work
      else ??? // TODO: divide the work
  }

  when(AggregatingWork) {
    ???
  }
}


