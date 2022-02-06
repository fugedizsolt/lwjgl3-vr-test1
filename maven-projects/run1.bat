set CLASSPATH=.
rem mvn exec:java -Dexec.mainClass="test.TestLog4j2" -Dexec.args=" " -Dlog4j.configurationFile=file://./log4j2-config2.xml
mvn exec:java -Dexec.mainClass="testvr01.HelloOpenVR" -Dexec.args=" "
rem mvn exec:java -Dexec.mainClass="testvr02.HelloOpenVR" -Dexec.args=" "
