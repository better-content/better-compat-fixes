import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** java --source 17 scripts/TreasureUrnPureCheck.java <repo> <raw.jar> <mapped.jar>
 * Compiles only the actual two JDK-only policy classes in memory; no caches/build outputs. */
class TreasureUrnPureCheck {
    private static void require(boolean condition) { if (!condition) throw new AssertionError("Pure urn policy contract failed"); }
    private static void rejects(java.lang.reflect.Method method, Object... args) throws Exception {
        try { method.invoke(null, args); throw new AssertionError("Unreviewed input accepted"); }
        catch (java.lang.reflect.InvocationTargetException failure) { require(failure.getCause() instanceof IllegalStateException); }
    }
    public static void main(String[] args) throws Exception {
        Map<String,ByteArrayOutputStream> compiled = new HashMap<>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager standard = compiler.getStandardFileManager(null, null, null)) {
            var manager = new ForwardingJavaFileManager<StandardJavaFileManager>(standard) {
                @Override public JavaFileObject getJavaFileForOutput(Location location, String name, JavaFileObject.Kind kind, FileObject sibling) {
                    return new SimpleJavaFileObject(URI.create("memory:///" + name.replace('.', '/') + kind.extension), kind) {
                        @Override public OutputStream openOutputStream() {
                            var bytes = new ByteArrayOutputStream(); compiled.put(name, bytes); return bytes;
                        }
                    };
                }
            };
            List<JavaFileObject> sources = new ArrayList<>();
            for (String type : List.of("TreasureUrnPredicate", "TreasureUrnCompatibility")) {
                Path source = Path.of(args[0], "src/main/java/com/bettercontent/bettercompatfixes/compat", type + ".java");
                String text = Files.readString(source);
                sources.add(new SimpleJavaFileObject(source.toUri(), JavaFileObject.Kind.SOURCE) {
                    @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return text; }
                });
            }
            require(compiler.getTask(null, manager, null, List.of("-proc:none", "--release", "17"), null, sources).call());
        }
        ClassLoader loader = new ClassLoader() {
            @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
                var stream = compiled.get(name); if (stream == null) throw new ClassNotFoundException(name);
                byte[] bytes = stream.toByteArray(); return defineClass(name, bytes, 0, bytes.length);
            }
        };
        var predicate = loader.loadClass("com.bettercontent.bettercompatfixes.compat.TreasureUrnPredicate").getMethod("skipLookup", boolean.class, boolean.class, boolean.class);
        require((boolean) predicate.invoke(null, true, false, true));
        for (boolean[] declined : new boolean[][]{{true,false,false}, {true,true,true}, {true,true,false},
                {false,false,true}, {false,false,false}, {false,true,true}, {false,true,false}})
            require(!(boolean) predicate.invoke(null, declined[0], declined[1], declined[2]));
        var policy = loader.loadClass("com.bettercontent.bettercompatfixes.compat.TreasureUrnCompatibility");
        var version = policy.getMethod("checkVersion", boolean.class, String.class);
        require(!(boolean) version.invoke(null, false, null)); require((boolean) version.invoke(null, true, "1.20-3.1.43"));
        for (String unsupported : new String[]{null, "", "1.20-3.1.42", "1.20-3.1.44"}) rejects(version, true, unsupported);
        var classes = policy.getMethod("checkClasses", String.class, String.class);
        List<List<String>> pairs = new ArrayList<>();
        for (int index = 1; index <= 2; index++) try (ZipFile zip = new ZipFile(args[index])) {
            List<String> pair = new ArrayList<>();
            for (String suffix : List.of("blocks/UrnBlock.class", "tiles/UrnBlockTile.class"))
                pair.add(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(zip.getInputStream(zip.getEntry("net/mehvahdjukaar/supplementaries/common/block/" + suffix)).readAllBytes())));
            classes.invoke(null, pair.get(0), pair.get(1)); rejects(classes, "0".repeat(64), pair.get(1)); pairs.add(pair);
        }
        rejects(classes, pairs.get(0).get(0), pairs.get(1).get(1));
        System.out.println("Actual in-memory JDK-only urn predicate/optional-admission positive+negative contracts PASS; no Native qualification");
    }
}
