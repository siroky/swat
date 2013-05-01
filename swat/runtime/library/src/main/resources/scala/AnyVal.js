swat.provide('scala.AnyVal');
swat.require('scala.Any', true);

scala.AnyVal = swat.type('scala.AnyVal', [scala.AnyVal, scala.Any]);
