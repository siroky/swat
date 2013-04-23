set SCRIPT_DIR=%~dp0
java -Xmx1024M -XX:MaxPermSize=512M -Xss2M -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -jar "%SCRIPT_DIR%sbt-launch.jar" %*