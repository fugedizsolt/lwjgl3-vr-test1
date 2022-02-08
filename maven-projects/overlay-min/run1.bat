SET M2REPOS=C:/Users/Fuge/.m2/repository

set CLASSPATH=.
set CLASSPATH=%CLASSPATH%;target\l3ovr1-1.0-SNAPSHOT.jar

set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\lwjgl\lwjgl\3.3.0\lwjgl-3.3.0.jar
set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\lwjgl\lwjgl-assimp\3.3.0\lwjgl-assimp-3.3.0.jar
set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\lwjgl\lwjgl-glfw\3.3.0\lwjgl-glfw-3.3.0.jar
set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\lwjgl\lwjgl-openal\3.3.0\lwjgl-openal-3.3.0.jar
set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\lwjgl\lwjgl-opengl\3.3.0\lwjgl-opengl-3.3.0.jar
set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\lwjgl\lwjgl-openvr\3.3.0\lwjgl-openvr-3.3.0.jar
set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\lwjgl\lwjgl-stb\3.3.0\lwjgl-stb-3.3.0.jar
set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\lwjgl\lwjgl\3.3.0\lwjgl-3.3.0-natives-windows.jar
set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\lwjgl\lwjgl-assimp\3.3.0\lwjgl-assimp-3.3.0-natives-windows.jar
set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\lwjgl\lwjgl-glfw\3.3.0\lwjgl-glfw-3.3.0-natives-windows.jar
set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\lwjgl\lwjgl-openal\3.3.0\lwjgl-openal-3.3.0-natives-windows.jar
set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\lwjgl\lwjgl-opengl\3.3.0\lwjgl-opengl-3.3.0-natives-windows.jar
set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\lwjgl\lwjgl-openvr\3.3.0\lwjgl-openvr-3.3.0-natives-windows.jar
set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\lwjgl\lwjgl-stb\3.3.0\lwjgl-stb-3.3.0-natives-windows.jar

set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\apache\logging\log4j\log4j-api\2.17.1\log4j-api-2.17.1.jar
set CLASSPATH=%CLASSPATH%;%M2REPOS%\org\apache\logging\log4j\log4j-core\2.17.1\log4j-core-2.17.1.jar

set CLASSPATH=%CLASSPATH%;

rem mvn exec:java -Dexec.mainClass="test.TestLog4j2" -Dexec.args=" " -Dlog4j.configurationFile=file://./log4j2-config2.xml
rem mvn exec:java -Dexec.mainClass="testvr01.HelloOpenVR" -Dexec.args=" "
rem mvn exec:java -Dexec.mainClass="testvr02.HelloOpenVR" -Dexec.args=" "
rem mvn exec:java -Dexec.mainClass="HelloWorld"
rem mvn -X exec:java -Dorg.lwjgl.util.Debug=true -Dexec.mainClass="tests.overlay1.lwjgl3ovr1.Main1" -Dexec.args=-javaagent:lwjglx-debug-1.0.0.jar > aa1

D:\Java64bit\jdk1.8.0_191\bin\java -javaagent:D:/tmp/lwjgl3-debug/lwjglx-debug-1.0.0.jar -Dorg.lwjgl.util.Debug=true tests.overlay1.lwjgl3ovr1.Main1 > aa1
