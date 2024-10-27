package idetector.core.scanner;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import idetector.core.collector.ClassInfoCollector;
import idetector.core.container.DaoContainer;
import idetector.core.container.DataContainer;
import idetector.core.switcher.Switcher;
import idetector.dal.caching.bean.edge.Alias;
import idetector.dal.caching.bean.edge.Extend;
import idetector.dal.caching.bean.edge.Has;
import idetector.dal.caching.bean.edge.Interfaces;
import idetector.dal.caching.bean.ref.ClassReference;
import idetector.dal.caching.bean.ref.MethodReference;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static idetector.core.container.DataContainer.clean;
import static idetector.core.container.DataContainer.cleanPackageName;


@Data
@Slf4j
@Component
public class ClassInfoScanner {

    @Autowired
    private DataContainer dataContainer;

    @Autowired
    private DaoContainer daoContainer;

    @Autowired
    private ClassInfoCollector collector;

    public void run(List<String> paths){

        Map<String, CompletableFuture<ClassReference>> classes = loadAndExtract(paths);

        transform(classes.values());
        List<String> runtimeClasses = new ArrayList<>(classes.keySet());
        classes.clear();

        buildClassEdges(runtimeClasses);

//        save();

        searchCallChain();
    }

    public Map<String, CompletableFuture<ClassReference>> loadAndExtract(List<String> targets){
        Map<String, CompletableFuture<ClassReference>> results = new HashMap<>();
        Scene.v().loadBasicClasses();
        Scene.v().loadDynamicClasses();
        int counter = 0;
        log.info("Start to collect {} targets' class information.", targets.size());

        for (final String path : targets) {

            for (String cl : SourceLocator.v().getClassesUnder(path)) {

                try {
                    if (cl.startsWith("BOOT-INF.classes.")) {
                        cl = cl.replaceFirst("BOOT-INF\\.classes\\.", "");
                    }

                    if (results.containsKey(cl)) {
                        continue;
                    }
                    SootClass theClass = Scene.v().loadClassAndSupport(cl);

                    if (!path.contains(File.separator+"lib")) {
                        String packageName = cleanPackageName(theClass.getPackageName());
                        dataContainer.getProgramPackages().add(packageName);
                    }
                    if (!theClass.isPhantom()) {

                        results.put(cl, collector.collect(theClass));
                        theClass.setApplicationClass();
                        if (counter % 10000 == 0) {
                            log.info("Collected {} classes.", counter);
                        }
                        counter++;
                    }
                }
                catch (Exception e){
                    log.error("jar：" + path + ";" + cl);
                }
            }
        }
        log.info("Collected {} classes.", counter);
        return results;
    }

    public void transform(Collection<CompletableFuture<ClassReference>> futures){
        for(CompletableFuture<ClassReference> future:futures){
            try {
                ClassReference classRef = future.get();

                dataContainer.store(classRef);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();

            }
        }
    }

    public void buildClassEdges(List<String> classes){
        int counter = 0;
        int total = classes.size();
        log.info("Build {} classes' edges.", total);
        for(String cls:classes){
            if(counter%10000 == 0){
                log.info("Build {}/{} classes.", counter, total);
            }
            counter++;
            ClassReference clsRef = dataContainer.getClassRefByName(cls);
            if(clsRef == null) continue;
            extractRelationships(clsRef, dataContainer, daoContainer,0);
        }
        log.info("Build {}/{} classes.", counter, total);
    }

    /**
     *
     * @param clsRef
     * @param dataContainer
     * @param depth
     */
    public static void extractRelationships(ClassReference clsRef, DataContainer dataContainer, DaoContainer daoContainer, int depth){

        if(clsRef.isHasSuperClass()){
            ClassReference superClsRef = dataContainer.getClassRefByName(clsRef.getSuperClass());
            if(superClsRef == null && depth < 10){
                superClsRef = collect0(clsRef.getSuperClass(), null, dataContainer, daoContainer, depth+1);
            }
            if(superClsRef != null){
                Extend extend =  Extend.newInstance(clsRef, superClsRef);
                clsRef.setExtendEdge(extend);
                dataContainer.store(extend);
            }
        }

        if(clsRef.isHasInterfaces()){
            List<String> infaces = clsRef.getInterfaces();
            for(String inface:infaces){
                ClassReference infaceClsRef = dataContainer.getClassRefByName(inface);
                if(infaceClsRef == null && depth < 10){
                    infaceClsRef = collect0(inface, null, dataContainer, daoContainer, depth+1);
                }
                if(infaceClsRef != null){
                    Interfaces interfaces = Interfaces.newInstance(clsRef, infaceClsRef);
                    clsRef.getInterfaceEdge().add(interfaces);
                    dataContainer.store(interfaces);
                }
            }
        }

        makeAliasRelations(clsRef, dataContainer);
    }

    /**
     * @param classname
     * @return
     */
    public static ClassReference collect0(String classname, SootClass cls,
                                          DataContainer dataContainer, DaoContainer daoContainer, int depth){
        ClassReference classRef = null;
        try{
            if(cls == null){
                cls = Scene.v().getSootClass(classname);
            }
        }catch (Exception e){
            // class not found
            log.error("classes {} not found.", classname);
        }

        if(cls != null) {

            if(cls.isPhantom()){
                classRef = ClassReference.newInstance(cls);
                classRef.setPhantom(true);
            }else{
                classRef = ClassInfoCollector.collect0(cls, dataContainer, daoContainer);
                extractRelationships(classRef, dataContainer, daoContainer, depth);
            }
        }else if(!classname.isEmpty()){
            classRef = ClassReference.newInstance(classname);
            classRef.setPhantom(true);
        }

        dataContainer.store(classRef);
        return classRef;
    }


    public static void makeAliasRelations(ClassReference ref, DataContainer dataContainer){
        if(ref == null)return;
        // build alias relationship
        if(ref.getHasEdge() == null) return;

        List<Has> hasEdges = ref.getHasEdge();
        for(Has has:hasEdges){
            makeAliasRelation(has, dataContainer);
        }

        ref.setInitialed(true);
    }



    public static void makeAliasRelation(Has has, DataContainer dataContainer){
        MethodReference currentMethodRef = has.getMethodRef();

        SootMethod currentSootMethod = currentMethodRef.getMethod();
        if(currentSootMethod == null) return;

        SootClass cls = currentSootMethod.getDeclaringClass();

        Set<MethodReference> refs = dataContainer.getAliasMethodRefs(cls, currentSootMethod.getSubSignature());

        if(refs != null && !refs.isEmpty()){
            for(MethodReference ref:refs){
                if("<init>".equals(currentMethodRef.getName()) || "<clinit>".equals(currentMethodRef.getName())){
                    dataContainer.addSavedAliasByFather(currentMethodRef, ref);
                    Alias alias = Alias.newInstance(ref, currentMethodRef);
                    dataContainer.store(alias);
                    dataContainer.addSavedCallee2Caller(alias.getTarget().getSignature(), alias.getSource().getSignature());
                } else {
                    dataContainer.addSavedAliasByFather(ref,currentMethodRef);
                    //son.a -> father.a
                    Alias alias = Alias.newInstance(currentMethodRef, ref);
                    dataContainer.store(alias);

                    dataContainer.addSavedCallee2Caller(alias.getSource().getSignature(), alias.getTarget().getSignature());
                }

            }
        }
    }



    public void save(){
        log.info("Start to save remained data to graphdb.");
        dataContainer.save("class");
        dataContainer.save("has");
        dataContainer.save("alias");
        dataContainer.save("extend");
        dataContainer.save("interfaces");
        log.info("Graphdb saved.");
    }


    public void searchCallChain() {


        Map<String, String> reviseSign = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry: dataContainer.getSavedCallee2Callers().entrySet()) {
            String oldCalleeSign = entry.getKey();
            if (oldCalleeSign.contains("bootstrap$")) {
                String className = oldCalleeSign.split(": ")[0].substring(1);
                String paramType = oldCalleeSign.substring(oldCalleeSign.indexOf('(')+1, oldCalleeSign.indexOf(')'));
                String newCalleeSign = "";
                if (paramType.contains(",")) {
                    paramType = "";
                }
                String regSignature = Switcher.transformBootstrap(className, paramType);
                MethodReference callee = dataContainer.getMethodRefByRegSignature(regSignature);
                if (callee == null) {
                    continue;
                }
                newCalleeSign = clean(callee.getSignature());
                reviseSign.put(oldCalleeSign, newCalleeSign);
            }
        }
        for (Map.Entry<String, String> entry: reviseSign.entrySet()) {
            Set<String> value = dataContainer.getSavedCallerByCallee(entry.getKey());
            value.addAll(dataContainer.getSavedCallerByCallee(entry.getValue()));
            dataContainer.getSavedCallee2Callers().put(entry.getValue(), value);
            dataContainer.getSavedCallee2Callers().remove(entry.getKey());
        }




        Queue<MethodReference> queue = new LinkedList<>();
        HashSet<String> visitedMethods = new HashSet<>();
        for (MethodReference methodReference: dataContainer.getSavedMethodRefs().values()) {
            if (methodReference.isSink()) {
                queue.offer(methodReference);
                while (!queue.isEmpty()) {
                    MethodReference analyzeMethod = queue.poll();
                    visitedMethods.add(analyzeMethod.getSignature());
                    analyzeMethod.setAnalyzeLevel(0);
                    if (analyzeMethod.isSource()) {
                         dataContainer.addToWorkList(analyzeMethod);
                    }
                    Set<String> aliasSet = new HashSet<>();
                    Set<String> callers = dataContainer.getSavedCallerByCallee(analyzeMethod.getSignature());
                    if (callers != null)
                        aliasSet.addAll(callers);
                    for (String caller: aliasSet) {
                        MethodReference callerMethod = dataContainer.getMethodRefBySignature(caller);
                        if (!visitedMethods.contains(callerMethod.getSignature()))
                            queue.offer(callerMethod);
                    }
                }
            }
        }

        log.debug("WorkList is ready!");


    }

}