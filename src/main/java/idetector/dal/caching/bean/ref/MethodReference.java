package idetector.dal.caching.bean.ref;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import idetector.dal.caching.bean.edge.Alias;
import idetector.dal.caching.bean.edge.Call;
import idetector.dal.caching.converter.ListInteger2JsonStringConverter;
import idetector.dal.caching.converter.Map2JsonStringConverter;
import idetector.dal.caching.converter.Set2JsonStringConverter;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import java.nio.charset.StandardCharsets;
import java.util.*;


@Data
@Entity
@Slf4j
@Table(name = "methods")
public class MethodReference {

    @Id
    private String id;
    private String name;
    // @Column(unique = true)
    @Column(columnDefinition = "TEXT")
    private String signature;
    @Column(columnDefinition = "TEXT")
    private String subSignature;
    private String returnType;
    private int modifiers;
    private String classname;
    private int parameterSize;
    private String vul;
    @Column(columnDefinition = "TEXT")
    @Convert(converter = Set2JsonStringConverter.class)
    private HashSet<String> parameters = new HashSet<>();
    @Column(columnDefinition = "TEXT")
    @Convert(converter = Set2JsonStringConverter.class)
    private HashSet<String> callees = new HashSet<>();

    private boolean isSink = false;
    private boolean isSanitization = false;
    private boolean isSource = false;
    private boolean isStatic = false;
    private boolean isPolluted = false;
    private boolean hasParameters = false;
    private boolean isInitialed = false;
    private boolean actionInitialed = false;
    private boolean isIgnore = false;
    private boolean isSerializable = false;
    private boolean fromXml = false;
    private boolean fromAnnotation = false;

    private boolean isFake = false;

    private boolean isQuote = false;

    private boolean completedAnalyze = false;

    @Transient
    private Integer analyzeLevel = -1;

    @Transient
    private Integer maxAnalyzeLevel = 5;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = Map2JsonStringConverter.class)
    private Map<String, String> actions = new HashMap<>();

    @Convert(converter = ListInteger2JsonStringConverter.class)
    private List<Integer> pollutedPosition = new ArrayList<>();

    @org.springframework.data.annotation.Transient
    private transient Set<Call> callEdge = new HashSet<>();


    @org.springframework.data.annotation.Transient
    private transient Alias aliasEdge;


    @Transient
    private Map<List<Integer>, String> taintDigests = new HashMap<>();


    @Transient
    private Map<Integer, Boolean> analyzeStmts = new HashMap<>();

    @Transient
    private Set<String> relatedChains = new HashSet<>();

    public static MethodReference newInstance(String name, String signature){
        MethodReference methodRef = new MethodReference();
        String id = null;
        if(signature == null || signature.isEmpty()){
            id = Hashing.sha256()
                    .hashString(UUID.randomUUID().toString(), StandardCharsets.UTF_8)
                    .toString();
        }else{
            id = Hashing.sha256()
                    .hashString(signature, StandardCharsets.UTF_8)
                    .toString();
        }
        methodRef.setName(name);
        methodRef.setId(id);
        methodRef.setSignature(signature);
        return methodRef;
    }

    public static MethodReference newInstance(String classname, SootMethod method){
        MethodReference methodRef = newInstance(method.getName(), method.getSignature());
        methodRef.setClassname(classname);
        methodRef.setModifiers(method.getModifiers());
        methodRef.setSubSignature(method.getSubSignature());
        methodRef.setStatic(method.isStatic());
        methodRef.setReturnType(method.getReturnType().toString());
        Gson gson = new Gson();
        if(method.getParameterCount() > 0){
            methodRef.setHasParameters(true);
            methodRef.setParameterSize(method.getParameterCount());
            for(int i=0; i<method.getParameterCount();i++){
                List<Object> param = new ArrayList<>();
                param.add(i); // param position
                param.add(method.getParameterType(i).toString()); // param type
                methodRef.getParameters().add(gson.toJson(param));
            }
        }
        return methodRef;
    }

    public SootMethod getMethod(){
        SootMethod method = null;
        try{
            SootClass sc = Scene.v().getSootClass(classname);
            if(!sc.isPhantom()){
                method = sc.getMethod(subSignature);
                return method;
            }
        }catch (Exception ignored){

        }
        return null;
    }

    public void addAction(String key, String value){
        actions.putIfAbsent(key, value);
    }

    public void addAnalyzeStmt(Stmt stmt) {
        analyzeStmts.put(stmt.hashCode(), false);
    }

    public Boolean containAnalyzeStmt(Stmt stmt) {
        return analyzeStmts.containsKey(stmt);
    }

    public boolean filterStmt(Stmt stmt) {
        if (analyzeStmts.keySet().contains(stmt.hashCode())) {
            return false;
        }
        return true;
    }

    public void completeStmt(Stmt stmt) {
        analyzeStmts.put(stmt.hashCode(), true);
    }

    public boolean completeAnalyze() {
        if (completedAnalyze) {
            return true;
        }

        if (analyzeStmts.isEmpty()) {
            return false;
        } else  {

            if (analyzeStmts.values().contains(false))
                return false;
        }
        return true;
    }

}
