# Swat - Scala web application toolkit

Aim of this project is to create a set of tools that will help to create rich internet applications using the Scala language. That is compilation of programs written in Scala to JavaScript and runtime environment which should enable execution of the compiled code in modern web browsers. On top of that, advanced libraries or features can be built in order to simplify tasks, that aren't very progrmmer-friendly to implement directly in JavaScript. 

## Components

- Scala to JavaScript compiler.
- Runtime that would support execution of the compiled code.
- Port of the most important Scala Library classes to JavaScript.
- Adapters of JavaScript objects and functions so they may be used within Scala code. And means of simple integration of existing libraries like jQuery, Google Closure Library etc.
- Object graph serializer/deserializer to/from JSON.
- Remote procedure call mechanism between the client-side and the server-side.
- Classloader that can dynamically fetch compiled class definitions from the server on the fly.
- Other possibilities stemming from new web browser features should be at least investigated:
  - Actors-like abstraction based on web workers.
  - Web sockets.
  - Template engine based on Scala XML support.
  - ...