swat.provide('scala.Nothing');
swat.require('scala.Any', true);

scala.Nothing = swat.type('scala.Nothing', [scala.Nothing, scala.Any]);
