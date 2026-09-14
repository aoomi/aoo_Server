import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** Source-launcher build gate for duplicate HTTP routes and concrete WSS message registrations. */
public final class ApiSurfaceCollisionCheck {
    private static final Pattern HTTP = Pattern.compile("(?:createContext\\s*\\(|(?:Get|Post|Put|Delete|Patch)Mapping\\s*\\()\\s*\"([^\"]+)\"");
    private static final Pattern WSS = Pattern.compile("\\.register\\s*\\(\\s*\"([a-z][a-z0-9_-]*(?:\\.[a-z][a-z0-9_-]*)+_(?:req|resp|push))\"");
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args[0]).toAbsolutePath().normalize();Map<String,Path> bindings=new LinkedHashMap<>();List<String> collisions=new ArrayList<>();
        try(var paths=Files.walk(root.resolve("server"))){for(Path file:paths.filter(Files::isRegularFile).filter(value->value.toString().endsWith(".java"))
                .filter(value->value.toString().contains("/src/main/")||value.toString().contains("/src/core/")).toList()){
            String source=Files.readString(file);scan("HTTP",HTTP,source,file,bindings,collisions);scan("WSS",WSS,source,file,bindings,collisions);
        }}
        if(!collisions.isEmpty()){System.err.println("Shadow API entry points are forbidden:");collisions.forEach(value->System.err.println("  "+value));System.exit(1);}
        System.out.println("API surface collision check passed: "+bindings.size()+" registered entries.");
    }
    private static void scan(String transport,Pattern pattern,String source,Path file,Map<String,Path> bindings,List<String> collisions){
        Matcher matcher=pattern.matcher(source);while(matcher.find()){String key=transport+":"+matcher.group(1);Path previous=bindings.putIfAbsent(key,file);if(previous!=null&&!previous.equals(file))collisions.add(key+" -> "+previous+" and "+file);}
    }
}
