swat.provide('scala.Boolean');
swat.require('scala.Any', true);
swat.require('scala.AnyVal', true);

scala.Boolean = swat.type('scala.Boolean', [scala.Boolean, scala.AnyVal, scala.Any]);
