swat.provide('scala.Float');
swat.require('scala.Any', true);
swat.require('scala.AnyVal', true);

scala.Float = swat.type('scala.Float', [scala.Float, scala.AnyVal, scala.Any]);
