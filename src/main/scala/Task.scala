import akka.actor.ActorRef

trait Task {
  val isAtomic: Boolean
}
