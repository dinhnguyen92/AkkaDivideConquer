package task

trait Task {
  val isAtomic: Boolean
  val summary: String
}

object Task {

  // Task exceptions
  val ILLEGAL_PARENT_ERROR = "Worker can only have one direct manager"
  case class TaskTypeMisMatch(msg: String) extends RuntimeException(msg)
}