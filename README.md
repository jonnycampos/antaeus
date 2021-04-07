## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!


#Main Challenges
After a first analysis I discovered these are the main challenges to solve:
* Create a scheduler in Kotlin. Use a Cron service instead
* Deploy the scheduler in a different docker instance to decouple from the rest service

But also I need to find out how
* Learn Kotlin basics
* Learn Docker basics
* How to debug kotlin in IntelliJ
* How to create and launch JUnit in Kotlin
* How to access and Write sqllite database


#Application Versions
The application will be released in different versions
##Antaeus 1.0 
MVP of the application. It will cover main requirements with the main goal
of processing invoices to show the capabilities of the app
Stories and Tasks to cover:
- BillingService implementation to handle pending invoices (change status of invoices in database)
- Rest call to execute billing service 
- A simple scheduler in a different project with no dependency with others
- Scheduler calls Rest API
- JUnit Billing Service
- Simple retry mechanism to schedule daily invoices where the payment fails
- Everything will run in a single docker container

##Antaeus 1.1
- The scheduler will run in a different docker instance (decouple from the main logic) (!)
- Retry mechanism based on the number of retries (Example, it will retry 3 times)
- Payment Provider mock will return a payment state instead of a Boolean. Real Payment Providers work with states that
  are different from one payment method to another.
- New scheduler to update the status of the payment executed every day
- Integration Test to check database changes

##Antaeus 1.2 
- Security end to end for API rest 
- Payment Provider mock will accept a payment token (recurring payment stored) and a payment method 
- Implement Quartz for schedulers (Easier way to change frequency)
- Add concurrency to manage calls to the external provider(!!)
dock
#Component Diagram
TBD 

#Time spent and challenges during implementation

