import akka.actor.{Actor, ActorLogging, ActorRef, FSM}
import akka.routing.{RoundRobinRoutingLogic, Router}

object Worker {
  trait WorkerState
  trait WorkerData

  // All Worker states
  case object AwaitingWork extends WorkerState
  case object DividingWork extends WorkerState
  case object ConqueringWork extends WorkerState
  case object AggregatingWork extends WorkerState

  // All Worker data
  case object NoWork extends WorkerData
  case class NewWork(work: Work) extends WorkerData
  case class WorkToDivide(work: Work) extends WorkerData
  case class WorkToConquer(work: Work, workGiver: ActorRef) extends WorkerData

}

abstract class Worker(val branchingFactor: Int)
  extends FSM[Worker.WorkerState, Worker.WorkerData]
  with ActorLogging {

  import Worker._

  startWith(AwaitingWork, NoWork)

  when(AwaitingWork) {
    case Event(NewWork(work), NoWork) =>
      if (work.isAtomic) goto(ConqueringWork) using WorkToConquer(work, sender)
      else goto(DividingWork) using WorkToDivide(work)
  }

  when(DividingWork) {
    ???
  }

  when(ConqueringWork) {
    ???
  }

  when(AggregatingWork) {
    ???
  }
}


