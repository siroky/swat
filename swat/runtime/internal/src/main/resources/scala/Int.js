swat.provide('scala.Int');
swat.require('scala.Any', true);
swat.require('scala.AnyVal', true);

scala.Int = swat.type('scala.Int', [scala.Int, scala.AnyVal, scala.Any]);
