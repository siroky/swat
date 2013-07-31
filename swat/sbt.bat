set SCRIPT_DIR=%~dp0
java -Xmx512M -XX:MaxPermSize=512M -Xss2M -jar "%SCRIPT_DIR%sbt-launch.jar" %*