Ensure the java environment and maven environment before running the project.

git clone https://github.com/Sunshuo000/java_unit_test.git

cd java_unit_test

mvn clean package

"/home/sunshuo/commons-lang-master" is the project file path that needs to be detected

"/home/sunshuo" is the output path of the last json

Before running the code, you need to compile the target project. The compilation method is to cd to the test project directory to execute the mvn clean package, such as:

cd /home/sunshuo/commons-lang-master

mvn clean package   (maybe there will be some error about Javadoc, then you can change the "mvn clean package" to 

mvn clean package "-Drat.skip=true" "-DskipTests" "-Dmaven.javadoc.skip=true" )

java -jar target/coverage-analyzer-1.0-SNAPSHOT-jar-with-dependencies.jar "/home/sunshuo/commons-lang-master" "/home/sunshuo"

If the project cannot be compiled, please directly use the compiled jar package provided(coverage-analyzer-1.0-SNAPSHOT-jar-with-dependencies.jar).
