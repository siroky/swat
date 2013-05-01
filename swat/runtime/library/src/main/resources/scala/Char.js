swat.provide('scala.Char');
swat.require('scala.Any', true);
swat.require('scala.AnyVal', true);

scala.Char = swat.type('scala.Char', [scala.Char, scala.AnyVal, scala.Any]);
