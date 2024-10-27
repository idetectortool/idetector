package idetector.core.collector;

import com.github.rholder.retry.*;
import com.google.common.base.Predicates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.tagkit.*;
import idetector.config.GlobalConfiguration;
import idetector.core.container.DaoContainer;
import idetector.core.container.DataContainer;
import idetector.core.container.RulesContainer;
import idetector.core.data.idetectorRule;
import idetector.core.switcher.Switcher;
import idetector.dal.caching.bean.edge.Has;
import idetector.dal.caching.bean.ref.ClassReference;
import idetector.dal.caching.bean.ref.MethodReference;
import idetector.util.AnnotationUtil;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static idetector.core.container.DataContainer.cleanPackageName;


@Service
@Slf4j
public class ClassInfoCollector {

    @Autowired
    private DataContainer dataContainer;

    @Autowired
    private DaoContainer daoContainer;


    public CompletableFuture<ClassReference> collect(SootClass cls) {
        return CompletableFuture.completedFuture(collect0(cls, dataContainer, daoContainer));
    }


    public static ClassReference collect0(SootClass cls, DataContainer dataContainer, DaoContainer daoContainer) {
        ClassReference classRef = ClassReference.newInstance(cls);
        Set<String> relatedClassnames = getAllFatherNodes(cls);
        classRef.setSerializable(relatedClassnames.contains("java.io.Serializable"));

        if (cls.getMethodCount() > 0) {
            for (SootMethod method : cls.getMethods()) {
                // 提取类函数信息并存储到DataContainer
                if (dataContainer.getMethodRefBySignature(method.getSignature()) != null) continue;
                extractMethodInfo(method, cls, classRef, relatedClassnames, dataContainer, daoContainer);
            }
        }
        return classRef;
    }


    public static Set<String> getAllFatherNodes(SootClass cls) {
        Set<String> nodes = new HashSet<>();
        if (cls.hasSuperclass() && !cls.getSuperclass().getName().equals("java.lang.Object")) {
            nodes.add(cls.getSuperclass().getName());
            nodes.addAll(getAllFatherNodes(cls.getSuperclass()));
        }
        if (cls.getInterfaceCount() > 0) {
            cls.getInterfaces().forEach(intface -> {
                nodes.add(intface.getName());
                nodes.addAll(getAllFatherNodes(intface));
            });
        }
        return nodes;
    }

    public static boolean isEndpoint(SootMethod method, Set<String> relatedClassnames) {
        // check jsp _jspService
        if ("_jspService".equals(method.getName())) {
            return true;
        }

        // check from annotation
        List<Tag> tags = method.getTags();
        for (Tag tag : tags) {
            if (tag instanceof VisibilityAnnotationTag) {
                VisibilityAnnotationTag visibilityAnnotationTag = (VisibilityAnnotationTag) tag;
                for (AnnotationTag annotationTag : visibilityAnnotationTag.getAnnotations()) {
                    String type = annotationTag.getType();
                    if (type.endsWith("Mapping;")
                            || type.endsWith("javax/ws/rs/Path;")
                            || type.endsWith("javax/ws/rs/GET;")
                            || type.endsWith("javax/ws/rs/PUT;")
                            || type.endsWith("javax/ws/rs/DELETE;")
                            || type.endsWith("javax/ws/rs/POST;")) {
                        return true;
                    }
                }
            }
        }


        List<String> requestTypes = new ArrayList<>(
                Arrays.asList("doGet", "doPost", "doPut", "doDelete", "doHead", "doOptions", "doTrace", "service", "RequestMapping"));
        // check from servlet
        return (relatedClassnames.contains("javax.servlet.Servlet")
                || relatedClassnames.contains("javax.servlet.http.HttpServlet")
                || relatedClassnames.contains("javax.servlet.GenericServlet"))
                && requestTypes.contains(method.getName());
        // not an endpoint
    }

    /**
     * @param method
     * @param cls
     * @param classRef
     * @param relatedClassnames
     * @param dataContainer
     * @param daoContainer
     */
    public static void extractMethodInfo(SootMethod method,
                                         SootClass cls,
                                         ClassReference classRef,
                                         Set<String> relatedClassnames,
                                         DataContainer dataContainer,
                                         DaoContainer daoContainer) {
        boolean isSink = false;
        boolean isIgnore = false;
        boolean isSource = false;
        boolean isQuote = false;
        boolean isSanitization = false;
        idetectorRule.Rule rule = null;
        RulesContainer rulesContainer = dataContainer.getRulesContainer();
        String className = classRef.getName();
        String methodName = method.getName();
        MethodReference methodRef = MethodReference.newInstance(className, method);
        List<AnnotationTag> classAnnotations = AnnotationUtil.getClassAnnoTags(cls);
        List<AnnotationTag> methodAnnotations = AnnotationUtil.getMethodAnnoTags(method);
        List<AnnotationTag> paramAnnotations = AnnotationUtil.getParamAnnoTags(method);
        List<Integer> pollutions = new ArrayList<>();


        if (daoContainer.getMappers().contains(className, methodName)) {
            methodRef.setFromXml(true);
            isSink = true;
            HashSet<String> pollutedArgs = daoContainer.getMappers().get(className, methodName);

            if (method.getParameterCount() == paramAnnotations.size()) {
                for (int i = 0; i < method.getParameterCount(); i++) {
                    if (method.getParameterTypes().get(i).toString().equals("java.lang.String")) {
                        AnnotationStringElem paramAnno = (AnnotationStringElem) paramAnnotations.get(0).getElems().stream().findFirst().get();
                        if (pollutedArgs.contains(paramAnno.getValue())) {
                            pollutions.add(i);
                        }
                    }
                }
            }

            if (pollutions.isEmpty()) {
                pollutions.add(-1);
            }
            methodRef.setPollutedPosition(pollutions);
        }


        if (!methodAnnotations.isEmpty()) {
            for (AnnotationTag annotationTag: methodAnnotations) {

                String type = annotationTag.getType();
                type = type.substring(1, type.length() - 1).replace("/", ".");
                idetectorRule.Rule sourceRule = rulesContainer.getRule(type, "0");

                if (sourceRule != null && !className.contains("org.springframework.boot.autoconfigure.web.servlet")) {
                    isSource = sourceRule.isSource();
                    break;
                }

                if (checkSqlAnnotation(type)) {
                    for (AnnotationElem elem : annotationTag.getElems()) {
                        while (elem instanceof AnnotationArrayElem) {
                            elem = ((AnnotationArrayElem) elem).getValueAt(0);
                        }
                        if (elem instanceof AnnotationStringElem) {
                            String annotationContent = ((AnnotationStringElem) elem).getValue();
                            Matcher matcher = Pattern.compile("\\$\\{(\\w*?)}").matcher(annotationContent);
                            HashSet<String> pollutedArgs = new HashSet<>();
                            while (matcher.find()) {
                                String var = matcher.group(1);
                                pollutedArgs.add(var);
                            }
                            if (!pollutedArgs.isEmpty()) {
                                methodRef.setFromAnnotation(true);
                                isSink = true;

                                if (method.getParameterCount() == paramAnnotations.size()) {
                                    for (int i = 0; i < method.getParameterCount(); i++) {
                                        if (method.getParameterTypes().get(i).toString().equals("java.lang.String")) {
                                            AnnotationStringElem paramAnno = (AnnotationStringElem) paramAnnotations.get(0).getElems().stream().findFirst().get();
                                            if (pollutedArgs.contains(paramAnno.getValue())) {
                                                pollutions.add(i);
                                            }
                                        }
                                    }
                                }

                                if (pollutions.isEmpty()) {
                                    pollutions.add(-1);
                                }
                                methodRef.setPollutedPosition(pollutions);
                            }
                        }
                    }
                }
            }
        }


        if (GlobalConfiguration.isAllEndpoint) {
            if (!isSource) {
                isSource = isEndpoint(method, relatedClassnames);
            }
        }


        rule = rulesContainer.getRule(className, methodRef.getName());

        if (rule == null) {
            for (String relatedClassname : relatedClassnames) {
                idetectorRule.Rule tmpRule = rulesContainer.getRule(relatedClassname, methodRef.getName());
                if (tmpRule == null) {
                    tmpRule = rulesContainer.getRule(relatedClassname, "*");
                }
                if (tmpRule != null && (tmpRule.isIgnore() || tmpRule.isSource())) {
                    rule = tmpRule;
                    break;
                }
            }
        }

        if (rule == null) {
            rule = rulesContainer.getRule("ignoreMethod", methodRef.getName());
        }

        if (rule == null) {
            rule = rulesContainer.getRule(className, "*");
        }
        isSource |= rule != null && rule.isSource();
        isSink |= rule != null && rule.isSink();
        isIgnore |= rule != null && rule.isIgnore();
        isQuote |= rule != null && rule.isQuote();
        isSanitization |= rule != null && rule.isSanitization();




        if (!isSink && (
                dataContainer.getProgramPackages().contains(cleanPackageName(cls.getPackageName()))
                || dataContainer.getIncludeLib().stream().anyMatch(cleanPackageName(cls.getPackageName())::contains))) {
        // if (!isSink) {
            if (!method.isAbstract() && !method.isPhantom()) {
                Callable<Boolean> extractCallers = () -> {
                    Body body = method.retrieveActiveBody();
                    body.getUnits().forEach((unit) -> {
                        if (unit instanceof Stmt) {
                            Stmt stmt = (Stmt) unit;
                            if (stmt.containsInvokeExpr()) {
                                // 此处还没考虑调用时的多态关系
                                String callerSign = method.getSignature();
                                String calleeSign = stmt.getInvokeExpr().getMethod().getSignature();
                                if (!calleeSign.equals(callerSign))
                                    dataContainer.addSavedCallee2Caller(calleeSign, callerSign);


                                String invClassName = stmt.getInvokeExpr().getMethod().getDeclaringClass().getName();
                                String invMethodName = stmt.getInvokeExpr().getMethod().getName();
                                String arg0 = null;
                                if (stmt.getInvokeExpr().getArgs().size() > 0) {
                                    arg0 = stmt.getInvokeExpr().getArg(0).toString();
                                }

                                List<String> sqlMethods = Arrays.asList("selectOne", "selectList", "selectMap", "select", "insert", "update", "delete");
                                if (arg0 != null && arg0.matches("\"[a-zA-Z]+\\.[a-zA-Z]+\"") && sqlMethods.contains(invMethodName)) {
                                    arg0 = arg0.replace("\"","");
                                    String mapperClassName = arg0.split("\\.")[0];
                                    String mapperMethodName = arg0.split("\\.")[1];
                                    if (daoContainer.getMappers().contains(mapperClassName, mapperMethodName)) {
                                        if (dataContainer.getMethodRefBySignature(calleeSign) == null) {

                                            SootClass clazz = Scene.v().getSootClass(mapperClassName);

                                            if (!clazz.declaresMethodByName(mapperMethodName)) {

                                                SootMethodRef invMethodRef = stmt.getInvokeExpr().getMethodRef();
                                                List<Type> paramTypes = invMethodRef.getParameterTypes();
                                                Type returnType = invMethodRef.getReturnType();
                                                List<Value> args = stmt.getInvokeExpr().getArgs();


                                                SootMethodRef newSootMethodRef = Scene.v().makeMethodRef(
                                                        Scene.v().getSootClass(mapperClassName),
                                                        mapperMethodName,
                                                        paramTypes.subList(1, paramTypes.size()),
                                                        returnType,
                                                        true
                                                );
                                                SootMethod newSootMethod = Scene.v().makeSootMethod(
                                                        mapperMethodName,
                                                        paramTypes.subList(1, paramTypes.size()),
                                                        returnType,
                                                        invMethodRef.resolve().getModifiers()
                                                );
                                                clazz.addMethod(newSootMethod);


                                                List<Value> newArgs = new ArrayList<>();
                                                for (int i = 1; i < args.size(); i++) {
                                                    newArgs.add(args.get(i));
                                                }
                                                InvokeExpr newExpr = Jimple.v().newStaticInvokeExpr(newSootMethodRef, newArgs);


                                                if (stmt instanceof JInvokeStmt) {
                                                    ((JInvokeStmt)stmt).setInvokeExpr(newExpr);
                                                } else if (stmt instanceof JAssignStmt) {
                                                    ((JAssignStmt)stmt).setRightOp(newExpr);
                                                }  else {
                                                    return;
                                                }


                                                MethodReference newMethodRef = MethodReference.newInstance(mapperClassName, newSootMethod);
                                                newMethodRef.setFake(true);
                                                newMethodRef.setFromXml(true);
                                                newMethodRef.setSink(true);
                                                newMethodRef.setPollutedPosition(Arrays.asList(-1));
                                                newMethodRef.setInitialed(true);
                                                newMethodRef.setActionInitialed(true);
                                                dataContainer.store(newMethodRef);
                                                log.debug("Set sink method: {}", newMethodRef.getSignature());
                                                dataContainer.addSavedCallee2Caller(newMethodRef.getSignature(), callerSign);
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    });
                    return true;
                };

                Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                        .retryIfException()
                        .retryIfResult(Predicates.equalTo(false))
                        .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                        .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                        .build();

                try {
                    retryer.call(extractCallers);
                } catch (ExecutionException e) {
                    // e.printStackTrace();
                } catch (RetryException e) {
                    log.error("Cannot extract callee from {}", method);
                    // e.printStackTrace();
                }
            }
        }


        methodRef.setSink(isSink);
        methodRef.setPolluted(isSink);
        methodRef.setIgnore(isIgnore);
        methodRef.setSource(isSource);
        methodRef.setQuote(isQuote);
        methodRef.setSanitization(isSanitization);
        methodRef.setSerializable(relatedClassnames.contains("java.io.Serializable"));

        // debug
        if (isSource) {
            log.debug("Set source method: {}", methodRef.getSignature());
        }
        if (isSink) {
            log.debug("Set sink method: {}", methodRef.getSignature());
        }


        if (rule != null && !isSource) {
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
        classRef.getHasEdge().add(has);
        dataContainer.store(has);
        dataContainer.store(methodRef);
    }
    
    public static boolean checkSqlAnnotation(String type) {
        if (type.contains("org.apache.ibatis.annotations"))
            return true;
        return false;
    }
}
