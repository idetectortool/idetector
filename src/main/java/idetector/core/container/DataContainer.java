package idetector.core.container;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import soot.*;
import idetector.core.collector.ClassInfoCollector;
import idetector.core.data.idetectorRule;
import idetector.core.data.WorklistItem;
import idetector.core.scanner.ClassInfoScanner;
import idetector.dal.caching.bean.edge.*;
import idetector.dal.caching.bean.ref.ClassReference;
import idetector.dal.caching.bean.ref.MethodReference;
import idetector.dal.caching.service.ClassRefService;
import idetector.dal.caching.service.MethodRefService;
import idetector.dal.caching.service.RelationshipsService;
import idetector.dal.neo4j.service.ClassService;
import idetector.dal.neo4j.service.MethodService;

import java.io.File;
import java.util.*;


@Getter
@Setter
@Slf4j
@Component

public class DataContainer {

    @Autowired
    private RulesContainer rulesContainer;
    @Autowired
    private ClassService classService;
    @Autowired
    private MethodService methodService;
    @Autowired
    private ClassRefService classRefService;
    @Autowired
    private MethodRefService methodRefService;
    @Autowired
    private RelationshipsService relationshipsService;
    @Autowired
    private  DaoContainer daoContainer;
    private HashSet<String> programPackages = new HashSet<>();
    private List<String> includeLib;

    private Map<String, ClassReference> savedClassRefs = Collections.synchronizedMap(new HashMap<>());
    private Map<String, MethodReference> savedMethodRefs = Collections.synchronizedMap(new HashMap<>());
    private Map<String, Set<String>> savedAliasByFather = Collections.synchronizedMap(new HashMap<>());
    private Set<WorklistItem> workList = Collections.synchronizedSet(new HashSet<>());
    private Set<String> savedDateMethod = Collections.synchronizedSet(new HashSet<>());
    private Set<Has> savedHasNodes = Collections.synchronizedSet(new HashSet<>());
    private Set<Call> savedCallNodes = Collections.synchronizedSet(new HashSet<>());
    private Set<Alias> savedAliasNodes = Collections.synchronizedSet(new HashSet<>());
    private Set<Extend> savedExtendNodes = Collections.synchronizedSet(new HashSet<>());
    private Set<Interfaces> savedInterfacesNodes = Collections.synchronizedSet(new HashSet<>());
    private Map<String, Set<String>> savedCallee2Callers = Collections.synchronizedMap(new HashMap<>());
    private Set<String> savedSourceMethods = Collections.synchronizedSet(new HashSet<>());
    private Set<String> savedSinkMethods = Collections.synchronizedSet(new HashSet<>());
    private Map<String, String> libPackMap = Collections.synchronizedMap(new HashMap<>());
    private Map<String, String> jdkPackMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * check size and save nodes
     */
    public void save(String type) {
        switch (type) {
            case "class":
                if (!savedClassRefs.isEmpty()) {
                    List<ClassReference> list = new ArrayList<>(savedClassRefs.values());
                    savedClassRefs.clear();
                    classRefService.save(list);
                }
                break;
            case "method":
                if (!savedMethodRefs.isEmpty()) {
                    List<MethodReference> list = new ArrayList<>(savedMethodRefs.values());
                    savedMethodRefs.clear();
                    methodRefService.save(list);
                }
                break;
            case "has":
                if (!savedHasNodes.isEmpty()) {
                    relationshipsService.saveAllHasEdges(savedHasNodes);
                    savedHasNodes.clear();
                }
                break;
            case "call":
                if (!savedCallNodes.isEmpty()) {
                    relationshipsService.saveAllCallEdges(savedCallNodes);
                    savedCallNodes.clear();
                }
                break;
            case "extend":
                if (!savedExtendNodes.isEmpty()) {
                    relationshipsService.saveAllExtendEdges(savedExtendNodes);
                    savedExtendNodes.clear();
                }
                break;
            case "interfaces":
                if (!savedInterfacesNodes.isEmpty()) {
                    relationshipsService.saveAllInterfacesEdges(savedInterfacesNodes);
                    savedInterfacesNodes.clear();
                }
                break;
            case "alias":
                if (!savedAliasNodes.isEmpty()) {
                    relationshipsService.saveAllAliasEdges(savedAliasNodes);
                    savedAliasNodes.clear();
                }
                break;
        }
    }

    /**
     *
     * store nodes
     * insert if node not exist
     * replace if node exist
     *
     * @param ref node
     * @param <T> node type
     */
    public <T> void store(T ref) {
        if (ref == null)
            return;

        if (ref instanceof ClassReference) {
            ClassReference classRef = (ClassReference) ref;
            savedClassRefs.put(classRef.getName(), classRef);
        } else if (ref instanceof MethodReference) {
            synchronized (savedMethodRefs) {
                MethodReference methodRef = (MethodReference) ref;
                savedMethodRefs.put(clean(methodRef.getSignature()), methodRef);
            }
        } else if (ref instanceof Has) {
            savedHasNodes.add((Has) ref);
        } else if (ref instanceof Call) {
            savedCallNodes.add((Call) ref);
        } else if (ref instanceof Interfaces) {
            savedInterfacesNodes.add((Interfaces) ref);
        } else if (ref instanceof Extend) {
            savedExtendNodes.add((Extend) ref);
        } else if (ref instanceof Alias) {
            savedAliasNodes.add((Alias) ref);
        }
    }


    public void addToWorkList(MethodReference methodReference) {
        String methodSignature = clean(methodReference.getSignature());
        synchronized (workList) {
            if (!savedDateMethod.contains(methodSignature)) {
                log.debug("Add {} to wordlist", methodSignature);
                WorklistItem worklistItem = new WorklistItem(methodReference);
                savedDateMethod.add(methodSignature);
                workList.add(worklistItem);
            }
        }
    }

    public boolean worklistIsEmpty() {
        return workList.isEmpty();
    }

    public WorklistItem getOneFormWorkList() {
        synchronized (workList) {
            WorklistItem tmp = workList.iterator().next();
            log.debug("Get {} from workList", tmp.getMethodReference().getSignature());
            workList.remove(tmp);
            return tmp;
        }
    }

    public ClassReference getClassRefByName(String name) {
        ClassReference ref = savedClassRefs.getOrDefault(name, null);
        if (ref != null)
            return ref;
        // find from h2
        // ref = classRefService.getClassRefByName(name);
        return ref;
    }

    /**
     * @param classname
     * @param subSignature
     * @return
     */
    public MethodReference getMethodRefBySubSignature(String classname, String subSignature) {
        String signature = String.format("<%s: %s>", clean(classname), clean(subSignature));
        MethodReference ref = savedMethodRefs.getOrDefault(signature, null);
        if (ref != null)
            return ref;
        // // find from h2
        // ref = methodRefService.getMethodRefBySignature(signature);
        return ref;
    }


    /**
     *
     * @param data
     * @return
     */
    public static String clean(String data) {
        data =  data.replace("'", "");
        data = data.replaceAll("__\\d+", "");

        if (data.contains("$lambda")) {
            if (data.contains(": ")) {

                String className = data.substring(1, data.indexOf(": "));
                String newClassName = className.replace("_","$");
                data = data.replace(className, newClassName);
            } else {
                data = data.replace("_","$");
            }
        }
        return data;
    }


    public static String cleanPackageName(String packageName) {
        String[] items = packageName.split("\\.");
        if (items.length > 3) {
            return String.format("%s.%s.%s", items[0], items[1], items[2]);
        }
        return packageName;
    }


    public MethodReference getMethodRefBySignature(String signature) {
        return savedMethodRefs.getOrDefault(clean(signature), null);
    }


    public MethodReference getMethodRefByRegSignature(String regSignature) {
        try {

            String className = regSignature.split(":")[0].replace("\\","");
            for(String sign: savedMethodRefs.keySet()) {
                if (sign.startsWith(className)) {
                    if (sign.matches(className)) {
                        return getMethodRefBySignature(sign);
                    }
                }
            }
        }  catch (java.util.NoSuchElementException e) {
            log.error(e.getMessage());
        }
        return null;
    }



    public MethodReference getMethodRefBySignature(SootMethodRef sootMethodRef) {
        SootClass cls = sootMethodRef.getDeclaringClass();
        String subSignature = sootMethodRef.getSubSignature().toString();
        MethodReference target = getMethodRefBySubSignature(cls.getName(), subSignature);
        if (target != null) {// 当前对象就能找到
            return target;
        }

        return getFirstMethodRefFromFatherNodes(cls, subSignature, false);
    }


    public MethodReference getMethodRefBySignature(String classname, String subSignature) {
        try {
            SootClass cls = Scene.v().getSootClass(classname);
            try {
                SootMethod method = cls.getMethod(subSignature);
                if (method != null) {
                    return getMethodRefBySignature(method.makeRef());
                }
            } catch (Exception e) {

                return getFirstMethodRefFromFatherNodes(cls, subSignature, false);
            }
        } catch (Exception e) {

        }
        return null;
    }


    public MethodReference getOrAddMethodRef(SootMethodRef sootMethodRef, SootMethod method) {

        MethodReference methodRef = getMethodRefBySignature(sootMethodRef);

        if (methodRef == null) {

            SootClass cls = method.getDeclaringClass();
            String className = cls.getName();
            ClassReference classRef = getClassRefByName(className);
            if (classRef == null) {
                classRef = ClassInfoScanner.collect0(className, cls, this, daoContainer, 0);
                methodRef = getMethodRefBySignature(sootMethodRef);
            }

            if (methodRef == null) {
                if (this.getLibPackMap().containsKey(cleanPackageName(cls.getPackageName()))) {
                    String path = this.getLibPackMap().get(cleanPackageName(cls.getPackageName()));
                    Scene.v().setSootClassPath(Scene.v().getSootClassPath() + File.pathSeparator + path);
                    try {
                        SootClass theClass = Scene.v().loadClassAndSupport(className);
                        theClass.setApplicationClass();
                        if (!theClass.isPhantom()) {
                            ClassInfoCollector.collect0(cls, this, daoContainer);
                        }
                    }
                    catch (Exception e){
                        log.error("异常jar：" + path + "; 异常类： " + className);
                    }
                }
                methodRef = getMethodRefBySignature(sootMethodRef);
            }


            if (methodRef == null) {
                methodRef = MethodReference.newInstance(className, method);
                methodRef.setFake(true);

                idetectorRule.Rule rule = rulesContainer.getRule(className, methodRef.getName());

                if (rule == null) {
                    rule = rulesContainer.getRule("ignoreMethod", methodRef.getName());
                }

                if (rule == null) {
                    rule = rulesContainer.getRule(className, "*");
                }
                boolean isSource = rule != null && rule.isSource();
                boolean isSink = rule != null && rule.isSink();
                boolean isIgnore = rule != null && rule.isIgnore();
                methodRef.setSink(isSource);
                methodRef.setSink(isSink);
                methodRef.setPolluted(isSink);
                methodRef.setIgnore(isIgnore);

                if (rule != null) {
                    Map<String, String> actions = rule.getActions();
                    List<Integer> polluted = rule.getPolluted();
                    if (isSink) {
                        methodRef.setVul(rule.getVul());
                    }
                    methodRef.setActions(actions != null ? actions : new HashMap<>());
                    methodRef.setPollutedPosition(polluted != null ? polluted : new ArrayList<>());
                    methodRef.setInitialed(true);
                    methodRef.setActionInitialed(true);
                }

                Has has = Has.newInstance(classRef, methodRef);
                if (!classRef.getHasEdge().contains(has)) {
                    classRef.getHasEdge().add(has);
                    store(has);
                    ClassInfoScanner.makeAliasRelation(has, this);
                }
                store(methodRef);
            }
        }
        return methodRef;
    }


    public MethodReference getFirstMethodRefFromFatherNodes(SootClass cls, String subSignature, boolean deepFirst) {

        MethodReference target = null;

        if (cls.hasSuperclass()) {
            SootClass superCls = cls.getSuperclass();
            target = getTargetMethodRef(superCls, subSignature, deepFirst);

            if (target != null) {
                return target;
            }
        }

        if (cls.getInterfaceCount() > 0) {
            for (SootClass intface : cls.getInterfaces()) {
                target = getTargetMethodRef(intface, subSignature, deepFirst);

                if (target != null) {
                    return target;
                }
            }
        }
        return null;
    }

    private MethodReference getTargetMethodRef(SootClass cls, String subSignature, boolean deepFirst) {
        MethodReference target = null;
        if (deepFirst) {
            target = getFirstMethodRefFromFatherNodes(cls, subSignature, deepFirst);
            if (target == null) {
                target = getMethodRefBySubSignature(cls.getName(), subSignature);
            }
        } else {
            target = getMethodRefBySubSignature(cls.getName(), subSignature);
            if (target == null) {
                target = getFirstMethodRefFromFatherNodes(cls, subSignature, deepFirst);
            }
        }

        return target;
    }

    public Set<MethodReference> getAliasMethodRefs(SootClass cls, String subSignature) {
        Set<MethodReference> refs = new HashSet<>();
        Set<SootClass> classes = new HashSet<>();

        if (cls.hasSuperclass()) {
            classes.add(cls.getSuperclass());
        }

        if (cls.getInterfaceCount() > 0) {
            classes.addAll(cls.getInterfaces());
        }

        MethodReference ref = null;

        for (SootClass clazz : classes) {
            ref = getMethodRefBySubSignature(clazz.getName(), subSignature);
            if (ref != null) {
                refs.add(ref);
            } else {
                refs.addAll(getAliasMethodRefs(clazz, subSignature));
            }
        }
        return refs;
    }


    public void addSavedAliasByFather(MethodReference ref, MethodReference currentMethodRef) {
        if (savedAliasByFather.containsKey(ref.getId())) {
            savedAliasByFather.get(ref.getId()).add(currentMethodRef.getSignature());
        } else {
            Set<String> tmp = Collections.synchronizedSet(new HashSet<>());
            tmp.add(currentMethodRef.getSignature());
            savedAliasByFather.put(ref.getId(), tmp);
        }
    }


    public Set<String> getSavedAliasByFather(String id) {
        return savedAliasByFather.get(id);
    }


    public void addSavedCallee2Caller(String calleeSign, String callerSign) {
        calleeSign = clean(calleeSign);
        callerSign = clean(callerSign);
        savedCallee2Callers.computeIfAbsent(calleeSign, f -> new HashSet<>()).add(callerSign);
    }

    public Set<String> getSavedCallerByCallee(String calleeSign) {
        calleeSign = clean(calleeSign);
        if (savedCallee2Callers.containsKey(calleeSign)) {
            return savedCallee2Callers.get(calleeSign);
        } else {
            return new HashSet<>();
        }
    }

    public void addSavedSourceMethods(String sign) {
        savedSourceMethods.add(sign);
    }

    public void addSavedSinkMethods(String sign) {
        savedSinkMethods.add(sign);
    }


    public Set<MethodReference> getSinkMethodRefByCall() {
        Set<MethodReference> result = new HashSet<>();
        for (MethodReference methodRef : savedMethodRefs.values()) {
            if (methodRef.isSink() && methodRef.getCallEdge().size() > 0) {
                result.add(methodRef);
            }
        }
        return result;
    }


    public List<Call> getCallsByPollutedPosition(MethodReference methodRef, List<Integer> pollutedPosition) {
        Set<Integer> pp = new HashSet<>(pollutedPosition);
        Set<Call> result = new HashSet<>();
        if (pollutedPosition.size() == 0) {
            return new ArrayList<>(result);
        }
        for (Call call : methodRef.getCallEdge()) {
            List<Integer> check_pp = call.getPollutedPosition();
            boolean flag = true;
            for (int i : pp) {
                if (check_pp.size() > i + 1 && check_pp.get(i + 1) == -2) {
                    flag = false;
                    break;
                } else if (check_pp.size() <= i + 1) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                result.add(call);
            }
        }
        return new ArrayList<>(result);
    }

    public List<Integer> transferPollutedPosition(Call call, List<Integer> pollutedPositions) {
        Set<Integer> result = new HashSet<>();
        for (int i : pollutedPositions) {
            List<Integer> pp = call.getPollutedPosition();
            if (pp.size() > i + 1 && pp.get(i + 1) != -2) {
                result.add(pp.get(i + 1));
            }
        }
        return new ArrayList<>(result);
    }

    public void save2Neo4j() {
        int nodes = classRefService.countAll() + methodRefService.countAll();
        int relations = relationshipsService.countAll();
        log.info("Total nodes: {}, relations: {}", nodes, relations);
        log.info("Clean old data in Neo4j.");
        classService.clear();
        log.info("Save methods to Neo4j.");
        methodService.importMethodRef();
        log.info("Save classes to Neo4j.");
        classService.importClassRef();
        log.info("Save relation to Neo4j.");
        classService.buildEdge();
    }

    public void save2CSV() {
        log.info("Save cache to CSV.");
        classRefService.save2Csv();
        methodRefService.save2Csv();
        relationshipsService.save2CSV();
        log.info("Save cache to CSV. DONE!");
    }

}
