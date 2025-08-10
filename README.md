Ensure the java environment and maven environment before running the project.

git clone https://github.com/Sunshuo000/java_unit_test.git

cd java_unit_test

mvn clean package

"C:\Users\wuxiao\Desktop\javau\test\test11" is the project file path that needs to be detected

"C:\Users\wuxiao\Desktop\javau" is the output desktop of the last json

Before running the code, you need to compile the target project. The compilation method is to cd to the test project directory to execute the mvn clean package, such as:

cd C:\Users\lenovo\Desktop\test11\test11

mvn clean package

java -jar target/coverage-analyzer-1.0-SNAP-jar-with-dependencies.jar "C:\Users\wuxiao\Desktop\j Avau\test\test11" "C:\Users\wuxiao\Desktop\javau"

If the project cannot be compiled, please directly use the compiled jar package provided(coverage-analyzer-1.0-SNAPSHOT-jar-with-dependencies.jar).