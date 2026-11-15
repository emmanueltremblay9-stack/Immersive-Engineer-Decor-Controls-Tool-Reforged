import com.sun.source.util.JavacTask;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/** Syntax only. This deliberately does NOT claim mapped Minecraft compilation. */
public final class ParseJavaSources {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("Supply Java source paths");
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK compiler module required");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            var files = manager.getJavaFileObjectsFromPaths(Arrays.stream(args).map(Path::of).toList());
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                List.of("-proc:none", "--source", "21"), null, files);
            int count = 0;
            for (var ignored : task.parse()) count++;
            boolean errors = diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
            diagnostics.getDiagnostics().forEach(System.out::println);
            if (errors) throw new AssertionError("Java parser reported errors");
            System.out.println("PASS: parsed " + count + " Java files. Syntax only; mapped compilation NOT RUN.");
        }
    }
}
