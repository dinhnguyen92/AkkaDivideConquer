# AkkaDivideConquer

This project demonstrates how Akka actors can be used to recursively divide and conquer a problem asynchronously. The main Akka actor in the project is the `Worker` class. When a worker is assigned a task, it will either try to divide the task amonng more child workers, or solve it if the task is not divisible. Once the worker has solved the task (either by itself or by aggregating the results from its child workers), it will send the result back to the parent worker that has originally assigned it the task. In this way, a single initial worker will automatically create a tree of child workers to asynchronously solve a given task.

A worker is implemented as a Finite State Machine (FSM) with the following states and state transition:

![Worker FSM Diagram](https://drive.google.com/uc?export=view&id=1BmMIUKyo5MGlHrZYvCr2-NK7koUxx1Pp)

The `Worker` class is an abstract class. This allows the common logic for distributing tasks and aggregating results among the workers to be abstracted away. This leaves the particular logic for how to divide specific types of tasks and how to combine their results to be implemented in specialized classes that inherit from the `Worker` class. In this demo project, the specialized worker class is the `WordCountWorker` class, which counts the number of words in a string by recursively decomposing the string into smaller substrings.

No Akka actor in the project uses mutable variables to store state information. Instead, all state information is stored as immutable state parameters that are passed between state functions. Additionally, except for function calls to read files, all methods in the project are implemented as pure functions with no side effects. In other words, the project demonstrates how object-oriented programming (class hierarchy and abstraction) and functional programming can be combined in the context of asynchronous programming with Akka.
