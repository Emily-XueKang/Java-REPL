package cs652.repl;

import java.io.*;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.tools.*;
import com.sun.source.util.JavacTask;
import javafx.beans.binding.ObjectExpression;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;


public class JavaREPL {
	private static Path DEFAULT_PATH;
	private static ClassLoader loader;

	public static void main(String[] args) throws IOException {
		DEFAULT_PATH = Files.createTempDirectory("temp");
		loader = new URLClassLoader(new URL[]{DEFAULT_PATH.toUri().toURL()});
		exec(new InputStreamReader(System.in));
	}

	public static void exec(Reader r) throws IOException {
		BufferedReader stdin = new BufferedReader(r);
		NestedReader reader = new NestedReader(stdin);
		int classNumber = 0;


		while (true) {  //while not end of file, if isdeclaration, save java file decl; else save java file statments, then exec
			System.out.print("> ");
			String java = reader.getNestedString();

            if(java.length() == 0) continue;

            String className;
            String extendSuper;

            if(classNumber != 0){
                extendSuper = "Interp_" + (classNumber-1);
                className = "Interp_" + classNumber;
            }
            else{
                className = "Interp_" + classNumber;
                extendSuper = "";
            }

			String statement = "";
            String declaration = "";
			String newcode;

            if (isDeclaration(java)){
                declaration = java;
                newcode = getCode(className, extendSuper,declaration,statement);
            }
			else{
			    statement = java;
			    newcode = getCode(className,extendSuper,declaration,statement);
			}

			writeFile(DEFAULT_PATH.toString(), className, newcode);
            boolean success = compile(className);
			if (success) exec(loader, className, "exec");
            classNumber++;

        }

	}


	public static boolean isDeclaration(String line) throws IOException{
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		Iterable<? extends JavaFileObject> compilationUnits = fileManager
				.getJavaFileObjectsFromStrings(Arrays.asList(line));
		JavacTask task = (JavacTask)
				compiler.getTask(null, fileManager, diagnostics,
						null, null, compilationUnits);
		task.parse();
		return diagnostics.getDiagnostics().size() == 0;

//		getTask(Writer out, JavaFileManager fileManager, DiagnosticListener<? super JavaFileObject> diagnosticListener, Iterable<String> options, Iterable<String> classes, Iterable<? extends JavaFileObject> compilationUnits)
//		Creates a future for a compilation task with the given components and arguments.

	}

    /**
     * Return a class code based on the input line, with classname, declaration or statement.
     * @param className
     * @param extendSuper
     * @param declaration
     * @param statement
     * @return
     */
	public static String getCode(String className, String extendSuper, String declaration, String statement){

	    String javacode = "";
		String importing = "import java.io.*;\n" + "import java.util.*;\n";

		String executable = "public static void exec() {\n" + statement + "\n}\n";
		String classbody = "public static " + declaration + "\n" + executable;

		if(!statement.equals("")){
		    classbody = executable;
        }
        if(!declaration.equals("")){
		    classbody = "public static " + declaration + "\n" + "public static void exec() {\n}\n";
        }
        if(className.equals("Interp_0")){
            javacode = importing + "public class " + className + " {\n" + classbody + "\n}";
        }
        else{
            javacode = importing + "public class " + className + " extends " + extendSuper + " {\n" + classbody + "\n}";
        }

		return javacode;
	}


	/**
	 * Reference source: http://www.java2s.com/Code/Java/JDK-6/CompileaJavafilewithJavaCompiler.htm
	 * and http://www.informit.com/articles/article.aspx?p=2027052&seqNum=2
	 * Modified by XueKang
	 */
	public static Boolean compile(String className) throws IOException {
		String filename = DEFAULT_PATH.toString() + "/" + className + ".java";
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		Iterable<? extends JavaFileObject> compilationUnits = fileManager
				.getJavaFileObjectsFromStrings(Arrays.asList(filename)); //Construct an in-memory java source file from dynamic code
		String[] compileOptions = new String[]{"-g", "-d", "-cp"} ;//need to be verified
		Iterable<String> compilationOptions = Arrays.asList(compileOptions);
		JavacTask task = (JavacTask)
				compiler.getTask(null, fileManager, diagnostics,
						compilationOptions, null, compilationUnits);
		boolean success = task.call();
		fileManager.close();
		return success;
	}
//	public static CompilerControl getCompilerControlObject(String fileName){
//
//	}
	public static void exec(ClassLoader loader, String className, String methodName){
        try{
        	Class cl = loader.loadClass(className);
			Method f1 = cl.getDeclaredMethod(methodName);

			Object o = cl.newInstance();
			f1.invoke(null,null);
		}catch (Exception e){
        	System.out.println(e.getStackTrace());
		}

	}

	/**
	 * Reference sourse: https://www.mkyong.com/java/how-to-write-to-file-in-java-bufferedwriter-example/
	 * @param dir
	 * @param fileName
	 * @param content
	 */
	public static void writeFile(String dir, String fileName, String content){
		//SimpleJavaFileObject file = new SimpleJavaFileObject();
		BufferedWriter bw = null;
		FileWriter fw = null;
		String FILENAME = "\\" + fileName + ".java";
		try {

			fw = new FileWriter(FILENAME);
			bw = new BufferedWriter(fw);
			bw.write(content);

			//System.out.println("Done");

		} catch (IOException e) {

			e.printStackTrace();

		} finally {

			try {

				if (bw != null)
					bw.close();

				if (fw != null)
					fw.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}

		}
	}

    /**
     * Copy from http://stackoverflow.com/questions/617414/how-to-create-a-temporary-directory-folder-in-java
	 * Reference :http://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html#createTempDirectory%28java.nio.file.Path
	 * ,%20java.lang.String,%20java.nio.file.attribute.FileAttribute...%29
     * @return a
     * @throws IOException
     */
    public static Path createTempDirectory()
            throws IOException
    {
        final Path temp;

        temp = Files.createTempFile("temp", Long.toString(System.nanoTime()));
		
        return (temp);
    }


}
