set CLASSPATH=.
rem mvn exec:java -Dexec.mainClass="test.HelloOpenVR" -Dexec.args=" "
mvn exec:java -Dexec.mainClass="test.TestLog4j2" -Dexec.args=" " -Dlog4j.configurationFile=file://./log4j2-config2.xml
