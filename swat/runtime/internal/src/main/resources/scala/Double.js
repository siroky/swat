swat.provide('scala.Double');
swat.require('scala.Any', true);
swat.require('scala.AnyVal', true);

scala.Double = swat.type('scala.Double', [scala.Double, scala.AnyVal, scala.Any]);
