# Task
Create a restaurant application which accepts menu items from various serving staff in the restaurant.  This
application must then store the item along with a countdown for the item to be ready to serve.  The application
must be able to give a quick snapshot of any or all items on its list at any time.  It must also be able to remove
specific orders from the list of orders on demand.

## System Actors

### The application
Running on a “server” and accepting calls from devices carried by restaurant staff to process guest’s
menu orders.  This is where the bulk of time should be spent.

### The client
Multiple "tablets" carried by restaurant staff to take orders.  These will send requests to the “server”
to add, remove, and query menu items for each table.  Please make this as simple as possible.

## Requirements

* The client (the restaurant staff “devices” making the requests) MUST be able to: add one or more items with a
  table number, remove an item for a table, and query the items still remaining for a table.
* The application MUST, upon creation request, store the item, the table number, and how long the item will take to cook.
* The application MUST, upon deletion request, remove a specified item for a specified table number.
* The application MUST, upon query request, show all items for a specified table number.
* The application MUST, upon query request, show a specified item for a specified table number.
* The application MUST accept at least 10 simultaneous incoming add/remove/query requests.
* The client MAY limit the number of specific tables in its requests to a finite set (at least 100).
* The application MAY assign a length of time for the item to prepare as a random time between 5-15 minutes.
* The application MAY keep the length of time for the item to prepare static (in other words, the time does not have
  to be counted down in real time, only upon item creation and then removed with the item upon item deletion).

# Implementation
The system is implemented with the use of Akka actors, specifically, each table will be its own actor, and will keep track of the items in the order.

Another "Table manager" actor will forward the requests to the correct table.

The response from the "table actor" is asynchronous, and the Table service will only wait up to a configured timeout.

If the timeout is reached, an error response will be sent to the client, that will need to try again.

The client needs to specify a unique String as ID when requesting to add an item. 

That ID is used to make the request idempotent, thus preventing the creation of duplicate order items.
If the same ID is used for an order with different content, it will be rejected.

The use of a UUID is recommended as item identifier to reduce the potential conflict between separate clients.

The service is written in Kotlin 1.5.2, using Micronaut 3.0.0 and Akka 2.6

## Running the server...
### ...from command line
Execute `./gradlew run` from the root of the project to run the application with basic configuration.

### ...with Intellij
In the `.idea/runConfiguration` folder you can find a run configuration for IntelliJ that executes the gradle task.

### ...with docker
Execute `docker-compose up` to build the image and start the docker container.  
Follow the logs to verify when the server is ready to accept requests.

Make sure to rebuild the image with `docker-compose build --no-cache` after you make changes to the code.

## Running the tests
* Execute `./gradlew test` to execute the full test suite (with `--rerun-task` option to force the re-execution of all unchanged tests)

## Monitoring
The service exposes an endpoint for metrics in Prometheus format here: [Prometheus Metrics](http://localhost:8080/prometheus)

## Api Docs
The api definition is available, when the server is running locally, via [Swagger UI](http://localhost:8080/swagger/views/swagger-ui/index.html)

## Starting the mock clients
The server provides endpoints to start and stop a set of mock clients:
* GET http://localhost:8080/mock/client/start/{N} 
  * will start N background coroutines that will act as clients by continuously:
    * choosing a random table (0 ~ 200)
    * adding a few items
    * getting all the items
    * getting one item
    * removing one item
    * clearing the table
  * each coroutine will sleep between 0.1 and 1 seconds at the end of each "loop"
  * any exceptions at this stage will be ignored to keep the coroutine running
* GET http://localhost:8080/mock/client/stop
  * will stop all background coroutines

The progress of the clients can be seen in the output logs of the application