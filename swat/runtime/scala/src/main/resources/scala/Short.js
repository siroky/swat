swat.provide('scala.Short');
swat.require('scala.Any', true);
swat.require('scala.AnyVal', true);

scala.Short = swat.type('scala.Short', [scala.Short, scala.AnyVal, scala.Any]);
