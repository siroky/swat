swat.provide('scala.Long');
swat.require('scala.Any', true);
swat.require('scala.AnyVal', true);

scala.Long = swat.type('scala.Long', [scala.Long, scala.AnyVal, scala.Any]);
