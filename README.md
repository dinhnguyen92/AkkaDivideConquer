# AkkaDivideConquer

This project demonstrates how Akka actors can be used to recursively divide and conquer a problem asynchronously. The main Akka actor in the project is the `Worker` class. When a worker is assigned a task, it will either try to divide the task amonng more child workers, or solve it if the task is not divisible. Once the worker has solved the task (either by itself or by aggregating the results from its child workers), it will send the result back to the parent worker that has originally assigned it the task. In this way, a single initial worker will automatically create a tree of child workers to asynchronously solve a given task.

A worker is implemented as a Finite State Machine (FSM) with the following states and state transition:
