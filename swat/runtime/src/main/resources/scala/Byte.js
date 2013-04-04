swat.provide('scala.Byte');
swat.require('scala.Any', true);
swat.require('scala.AnyVal', true);

scala.Byte = swat.type('scala.Byte', [scala.Byte, scala.AnyVal, scala.Any]);
