
# Main Challenges
After a first analysis I discovered these are the main challenges to solve:
* Create a scheduler in Kotlin. Use a Cron service instead
* Deploy the scheduler in a different docker instance to decouple from the rest service
* Payment Provider to be called asynchronously (differen threads)

Also I need to find out how
* Learn Kotlin basics
* Learn Docker basics
* How to debug kotlin in IntelliJ
* How to create and launch JUnit in Kotlin
* How to access and Write sqllite database


# Application Versions
The application will be released in different versions

## Antaeus 1.0 
![Alt text](doc/antaeus_10.png?raw=true "Antaeus 1.0")

MVP of the application. It will cover main requirements with the main goal
of processing invoices to show the capabilities of the app

- BillingService implementation to handle pending invoices (change status of invoices in database)
- PaymentService to handle payments through Payment Provider 
- Rest call to execute billing services
- A simple scheduler in a different project with no dependency with others
- Scheduler calls Rest API
- JUnit Billing Service and Payment Service
- Simple retry mechanism to schedule daily invoices where the payment fails
- Everything will run in a single docker container

## Antaeus 1.1
![Alt text](doc/antaeus_11.png?raw=true "Antaeus 1.1")

- The scheduler will run in a different docker instance (decouple from the main logic)
- Concurrency to manage calls to the external provider
- Retry mechanism based on the number of retries (Example, it will retry 3 times)
- Payment Provider mock will return a payment state instead of Boolean. Real Payment Providers work with states that
  are different from one payment method to another.
- Integration Test to check database changes

## Antaeus 1.2 
![Alt text](doc/antaeus_12.png?raw=true "Antaeus 1.2")

- Security end to end for API rest 
- Payment Provider mock will accept a payment token (recurring payment stored) and a payment method 
- Implement Quartz for schedulers (Easier way to change frequency) in a new docker image
- Database in a separate docker image
- New scheduler to update the status of the payment executed every day



# Time spent and challenges during implementation

## Version 1.0
Time Spent - 10h 
Version 1.0 is delivered completely.

### Billing Logic
The billing logic is implemented mostly using BillingService and PaymentService
A simple retry mechanism was added to retry failed invoices a second time.
Added two new rest calls:
```
/rest/v1/billing/pending
/rest/v1/billing/retry
```


### Scheduler
The scheduler is working in a different project, but running under the same Docker instance
It has no dependency with other projects since it is invoking the logic using rest call
It took me a long to find out how to set up the schedulers in Kotlin. It seems there are not a
quite flexible way to set up the frequency as we will be doing in cron.
Changing the frequency would need a change in the code. Added the integration with Quartz
in version 1.2 as it seems the smart way to set up frequencies and there is a pre-built module in
Kotlin to manage the integration easily.


## Version 1.1
Time Spent - 7h 
Version 1.1 was not delivered completely. I added some features though as I wanted to learn more
about some concepts, and I wanted to deep dive more

### Billing Logic
I added coroutines to call the external provider asynchronously (Since there are no dependencies)
It was not an easy task to have them running even if I read a lot of docs regarding coroutines.

### Scheduler
The scheduler is totally decoupled now, and it is running on its own docker container
I had several issues with Docker in general. First I found several issues since I had Docker
installed on my laptop from a very old project and systems environments were not deleted. Also
it took me longer than expected fo find out why the port 7000 was not open. 
Finally, I managed to create two different docker files to create both images:
* app.Dockerfile
* scheduler.Dockerfile  (building and running only the scheduler)
I joined both in a docker-compose yaml file so to run both just execute:
  
```
docker-compose up
```
