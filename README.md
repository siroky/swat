# Swat - Scala web application toolkit

Aim of this project is to create a set of tools that will help to create rich internet applications using the Scala language. That is compilation of programs written in Scala to JavaScript and runtime environment which should enable execution of the compiled code in modern web browsers. On top of that, advanced libraries or features can be built in order to simplify tasks, that aren't very progrmmer-friendly to implement directly in JavaScript. Unlike ScalaGWT, it should be completely separated from any web application framework, so existing frameworks would be easily integratable into the Swat. Or new frameworks implemented directly in Scala could be created from scratch.

## Core Components

<table>
    <tr>
        <th>Component</th>
        <th>Status</th>
    </tr>
    <tr>
        <td>Scala to JavaScript compiler.</td>
        <td><strong>95%</strong></td>
    </tr>
    <tr>
        <td>Type safe adapters of existing & native JavaScript objects (and means of simple integration).</td>
        <td><strong>100%</strong></td>
    </tr>
    <tr>
        <td>Runtime that would support execution of the compiled code.</td>
        <td><strong>90%</strong></td>
    </tr>
    <tr>
        <td>Port of the most important Scala Library classes to JavaScript (hopefully with big help of Swat).</td>
        <td><strong>5%</strong></td>
    </tr>
    <tr>
        <td>Object graph serializer/deserializer to/from JSON.</td>
        <td><strong>0%</strong></td>
    </tr>
    <tr>
        <td>Remote procedure call mechanism between the client-side and the server-side.</td>
        <td><strong>0%</strong></td>
    </tr>
    <tr>
        <td>Classloader that can dynamically fetch compiled class definitions from the server on the fly.</td>
        <td><strong>50%</strong></td>
    </tr>
</table>

## Optional Components

Other possibilities stemming from new web browser features should be at least investigated from the feasibility point of view. And optionally implemented as a proof of concept.

<table>
    <tr>
        <th>Component</th>
        <th>Status</th>
    </tr>
    <tr>
        <td>Actors-like abstraction based on web workers.</td>
        <td><strong>0%</strong></td>
    </tr>
    <tr>
        <td>Web sockets.</td>
        <td><strong>0%</strong></td>
    </tr>
    <tr>
        <td>Template engine based on Scala XML support, or rather string interpolation.</td>
        <td><strong>0%</strong></td>
    </tr>
</table>
