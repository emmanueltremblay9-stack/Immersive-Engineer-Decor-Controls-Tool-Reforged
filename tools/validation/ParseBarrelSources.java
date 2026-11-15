import java.nio.file.*;
import javax.tools.*;
import com.sun.source.util.JavacTask;
public class ParseBarrelSources {
 public static void main(String[] args) throws Exception {
  var compiler = ToolProvider.getSystemJavaCompiler();
  var diagnostics = new DiagnosticCollector<JavaFileObject>();
  try(var manager = compiler.getStandardFileManager(diagnostics,null,null);
      var paths = Files.walk(Path.of("src"))) {
   var files = paths.filter(p -> p.toString().endsWith(".java")).map(Path::toFile).toList();
   var task = (JavacTask)compiler.getTask(null,manager,diagnostics,java.util.List.of("-proc:none"),null,manager.getJavaFileObjectsFromFiles(files));
   for(var ignored : task.parse()) {}
   for(var d : diagnostics.getDiagnostics()) if(d.getKind()==Diagnostic.Kind.ERROR) throw new AssertionError(d);
   System.out.println("PASS: syntax parsing for " + files.size() + " Java sources (not dependency/type checking)");
  }
 }
}
