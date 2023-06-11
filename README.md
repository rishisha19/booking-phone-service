# booking-phone-service
An akka http java typed actor based rest service


This service is developed with akka-http and akka-typed-actor.

## Why Akka-http?

Akka HTTP is a suite of libraries, rather than a framework like Play and Lagom.

Akka HTTP is a Reactive Streams compliant toolkit based on Akka Streams that implements a fully asynchronous and non-blocking server- and client-side HTTP stack.

Akka HTTP is geared towards flexibility with integration layers, rather than application cores. It includes built-in support for streaming APIs with Akka Streams and Reactive Streams, with a client-side API providing the same asynchronous, non-blocking and streaming support.

Akka HTTP is recommended for the following use cases:

- Adding small, lightweight REST/HTTP endpoints to applications.
- Building rich HTTP servers where flexibility is more important than simplicity.

## Alternatives choices to akka-http for this project

- play
- Spring
- lagom


## Pre-Requisite

- Java - 16
- scala binary - 2.13
- akka - 2.8
- akka-http - 10.5.2
- akka-typed-actor
- maven 3.8

## Tests & Run


### build with tests

```maven
 mvn clean install
```

### run tests

```maven
 mvn tests
```

### run server

```maven
 mvn exec: java
```

http://localhost:8080/phones
