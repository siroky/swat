swat.provide('scala.Unit');
swat.require('scala.Any', true);
swat.require('scala.AnyVal', true);

scala.Unit = swat.type('scala.Unit', [scala.Unit, scala.AnyVal, scala.Any]);
