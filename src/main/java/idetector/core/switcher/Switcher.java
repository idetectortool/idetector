package idetector.core.switcher;

import fj.Hash;
import fj.P;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JimpleLocalBox;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import idetector.config.GlobalConfiguration;
import idetector.core.container.ChainContainer;
import idetector.core.container.DataContainer;
import idetector.core.data.Context;
import idetector.core.data.idetectorValue;
import idetector.core.data.idetectorVariable;
import idetector.core.toolkit.PollutedVarsPointsToAnalysis;
import idetector.dal.caching.bean.edge.Call;
import idetector.dal.caching.bean.edge.Interfaces;
import idetector.dal.caching.bean.ref.MethodReference;

import java.util.*;
import java.util.stream.Collectors;

import static idetector.core.container.DataContainer.clean;
import static idetector.core.container.DataContainer.cleanPackageName;


@Slf4j
public class Switcher {

    /**
     *
     * @param context
     * @param dataContainer
     * @param methodRef
     */
    public static void doMethodAnalysis(Context context, DataContainer dataContainer, ChainContainer chainContainer, MethodReference methodRef) {
        try {
            SootMethod method = methodRef.getMethod();
            if (method == null) {
                return;
            }

            if (method.isAbstract() || Modifier.isNative(method.getModifiers()) || method.isPhantom()) {

                if (method.isPhantom()) {
                    if (method.getName().startsWith("get") || method.getName().startsWith("set")) {
                        if (method.getParameterCount() > 0) {
                            methodRef.addAction("this", "param-0&remain");
                        }
                        methodRef.addAction("return", "this");
                    }
                }
                methodRef.setInitialed(true);
                methodRef.setActionInitialed(true);
                return;
            }

            if (context.isInRecursion())
                return;

            // debug
            log.debug("Now analyze method: {}DEPTH[{}] {}", StringUtils.repeat(" ", methodRef.getAnalyzeLevel()),methodRef.getAnalyzeLevel(), methodRef.getSignature());

            JimpleBody body = (JimpleBody) method.retrieveActiveBody();
            Switcher.addAnalyzeStmts(dataContainer, methodRef, body);
            UnitGraph graph = new BriefUnitGraph(body);
            if (methodRef.getSubSignature().contains("directoryRename")){
                System.out.println("xxx");
            }
            if (methodRef.isSanitization()){
                context.setAlreadySan(true);
            }
            PollutedVarsPointsToAnalysis.makeDefault(methodRef, body, graph, dataContainer, chainContainer, context, !methodRef.isActionInitialed());
            methodRef.setInitialed(true);
            methodRef.setActionInitialed(true);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }



    public static idetectorVariable doInvokeExprAnalysis(Unit unit, InvokeExpr invokeExpr, DataContainer dataContainer, ChainContainer chainContainer, Context context) {

        idetectorVariable retVar = null;



        String invokeType = "";
        if (invokeExpr instanceof StaticInvokeExpr) {
            invokeType = "StaticInvoke";
        } else if (invokeExpr instanceof VirtualInvokeExpr) {
            invokeType = "VirtualInvoke";
        } else if (invokeExpr instanceof SpecialInvokeExpr) {
            invokeType = "SpecialInvoke";
        } else if (invokeExpr instanceof InterfaceInvokeExpr) {
            invokeType = "InterfaceInvoke";
        }


        SootClass cls = invokeExpr.getMethod().getDeclaringClass();
        SootMethod invokedMethod = invokeExpr.getMethod();

        if (invokedMethod.getName().contains("udpateFunction")&&cls.getName().contains("SysFunctionController")){
            System.out.println("xx");
        }

        if (invokedMethod.getName().contains("findApplyedTasksPage")){
            System.out.println("xx");
        }

        if (invokedMethod.getName().contains("findApplyedTasksPage")){
            System.out.println("xx");
        }


        if (invokedMethod.getSignature().contains("java.lang.String: java.lang.String replaceAll")
                || invokedMethod.getSignature().contains("java.lang.String: java.lang.String replace")
                || invokedMethod.getSignature().contains("java.lang.String: boolean matches")
                || invokedMethod.getSignature().contains("java.lang.String: boolean startsWith")
                || invokedMethod.getSignature().contains("java.lang.String: boolean equalsIgnoreCase")
                || invokedMethod.getSignature().contains("boolean contains")
                || invokedMethod.getSignature().contains("java.lang.String: boolean endsWith")
                || invokedMethod.getSignature().contains("java.lang.String: java.lang.Integer indexOf")
                || invokedMethod.getSignature().contains("java.lang.String: java.lang.Integer lastIndexOf")
                || invokedMethod.getSignature().contains("java.lang.String: boolean equals")
                || invokedMethod.getSignature().contains("java.lang.String: boolean equalsIgnoreCase")
                || invokedMethod.getSignature().contains("java.util.regex.Pattern: java.util.regex.Pattern compile")
                || invokedMethod.getSignature().contains("java.util.regex.Pattern: java.util.regex.Matcher matcher")
                || invokedMethod.getSignature().contains("java.util.regex.Matcher: boolean find")
                || invokedMethod.getSignature().contains("java.util.regex.Matcher: boolean matches")
                || invokedMethod.getSignature().contains("java.util.regex.Matcher: java.lang.String group")
        ){
            System.out.println("filter");
            chainContainer.addSan(context.getMethodSignature());
        }



        MethodReference methodRef = dataContainer.getOrAddMethodRef(invokeExpr.getMethodRef(), invokedMethod);
        String invClassName = invokedMethod.getDeclaringClass().getName();
        String invRetType = invokedMethod.getReturnType().toString();
        String invMethodName = invokedMethod.getName();


        idetectorVariable baseVar = Switcher.extractBaseVarFromInvokeExpr(invokeExpr, context);
        Map<Integer, idetectorVariable> args = Switcher.extractArgsFromInvokeExpr(invokeExpr, context);


        List<Integer> pollutedPosition = pollutedPositionAnalysis(baseVar, args);

        if (methodRef.getSubSignature().contains("upload")){
            System.out.println("xxxxx");
        }
        Boolean Sanflag = false;
        if (methodRef.isSanitization()){
            Sanflag =true;
        }



        if (methodRef.isQuote()) {
            context.setQuote(true);
            Integer setIndex = -1;
            for (int i = 0; i < context.getQuoteVars().size(); i++) {
                Set<idetectorVariable> quoteVar = context.getQuoteVars().get(i);
                idetectorVariable finalBaseVar = baseVar;
                if (finalBaseVar != null) {
                    if (quoteVar.stream().anyMatch(o -> o.getName().equals(finalBaseVar.getName()))) {
                        setIndex = i;
                        break;
                    }
                }
                for (idetectorVariable var : args.values()) {
                    if (quoteVar.stream().anyMatch(o -> o.getName().equals(var.getName()))) {
                        setIndex = i;
                        break;
                    }
                }
                if (setIndex >= 0) break;
            }
            HashSet<idetectorVariable> includeSet = new HashSet<>();
            if (setIndex >= 0) {
                includeSet = context.getQuoteVars().get(setIndex);
            }
            includeSet.add(baseVar);
            includeSet.addAll(args.values());
            if (setIndex < 0) {
                context.getQuoteVars().add(includeSet);
            }
        }




        if (methodRef.isSource()) {
            pollutedPosition.add(-1);
            if (baseVar == null) {
                baseVar = idetectorVariable.makeRandomInstance();
            }
            baseVar.getValue().setPolluted(true);
            baseVar.getValue().setRelatedType(baseVar.getValue().getRelatedType());
            retVar = baseVar;
        }
        if (retVar != null) {
            processRetVar(methodRef, retVar, pollutedPosition);
            return retVar;
        }


        if (invokeType == "StaticInvoke") {

            if (invRetType.matches("java.util.stream.[a-zA-Z]*Stream") && invMethodName.equals("stream") && args.size() == 1) {
                retVar = args.get(0);
            }

            if (invMethodName.contains("bootstrap$")) {
                retVar = idetectorVariable.makeRandomInstance();
                retVar.getValue().setFunction(true);
                String paramType = null;
                if (invokedMethod.getParameterCount() == 1)
                    paramType = invokedMethod.getParameterType(0).toString();
                retVar.getValue().setFunctionName(transformBootstrap(invClassName, paramType));
            }
        } else if (invokeType == "InterfaceInvoke") {

            if (invRetType.matches("java.util.stream.[a-zA-Z]*Stream") && invMethodName.equals("stream")) {
                retVar = baseVar;
            }
            if (invClassName.matches("java.util.stream.[a-zA-Z]*Stream")) {
                switch (invMethodName) {
                    case "of":
                        if (args.size() == 1) return args.get(0);
                        break;
                    case "sorted":
                    case "distinct":
                    case "limit":
                    case "skip":
                    case "toArray":
                    case "findFirst":
                    case "findAny":
                    case "get":
                        return baseVar;
                    case "filter":
                    case "map":
                    case "flatMap":
                    case "peek":
                        idetectorVariable arg0 = args.get(0);
                        if (!arg0.getValue().isFunction()) break;
                        String functionName = arg0.getValue().getFunctionName();

                        methodRef = dataContainer.getMethodRefByRegSignature(functionName);

                        args = new HashMap<>();
                        args.put(0, baseVar);
                        baseVar = idetectorVariable.makeRandomInstance();
                        break;
                    default:
                        retVar = idetectorVariable.makeRandomInstance();
                }
            }
        }

        if (retVar != null) {
            processRetVar(methodRef, retVar, pollutedPosition);
            return retVar;
        }


        if (pollutedPosition.isEmpty())
            return null;


        if (methodRef.isIgnore()) {
            for (Map.Entry<String, String> entry : methodRef.getActions().entrySet()) {
                String position = entry.getKey();
                String newRelated = entry.getValue();
                if ("return".equals(position)) continue;
                idetectorVariable oldVar = parsePosition(position, baseVar, args, true);
                idetectorVariable newVar;
                if (oldVar != null) {
                    if ("clear".equals(newRelated)) {
                        oldVar.clearVariableStatus();
                    } else {
                        boolean remain = false;
                        if(newRelated != null && newRelated.contains("&remain")){
                            remain = true;
                        }
                        newVar = parsePosition(newRelated, baseVar, args, false);
                        oldVar.assign(newVar, remain);
                    }
                }
            }
            if (methodRef.getActions().containsKey("return")) {
                retVar = parsePosition(methodRef.getActions().get("return"), baseVar, args, true);
            } else {
                return null;
            }
        }
        if (retVar != null) {
            processRetVar(methodRef, retVar, pollutedPosition);
            return retVar;
        }


        if (methodRef.isSink()) {
            Boolean isPolluted = true;

            for (int i : methodRef.getPollutedPosition()) {

                if (i == -1 && pollutedPosition.size() > 0) {
                    isPolluted = true;
                    break;
                }


                if (pollutedPosition.contains(i)) {
                    isPolluted = true;
                    break;
                }
            }
            if (isPolluted) {
                buildCallRelationship(cls.getName(), context, methodRef, dataContainer, unit, invokeType, pollutedPosition);

                chainContainer.addChain(context, methodRef.getSignature(), pollutedPosition);
            }
            return null;
        }


        Set<String> aliasSet = new HashSet<>();
        if (!methodRef.isStatic() && !(baseVar.isThis())) {
            aliasSet = dataContainer.getSavedAliasByFather(methodRef.getId());
        }


        if ("<init>".equals(methodRef.getName())
                && (methodRef.getAnalyzeLevel() != 0)
                && baseVar != null
                && !baseVar.isPolluted(-1)
                && aliasSet == null) {

            for (idetectorVariable arg : args.values()) {
                if (arg != null && arg.isPolluted(-1)) {
                    baseVar.getValue().setPolluted(true);
                    baseVar.getValue().setRelatedType(arg.getValue().getRelatedType());
                    retVar = baseVar;
                    break;
                }
            }
        } else if (methodRef.getName().equals("format")) {
            if (pollutedPosition.contains(1)) {
                if (baseVar == null) {
                    baseVar = idetectorVariable.makeRandomInstance();
                }
                baseVar.getValue().setPolluted(true);
                idetectorVariable var = args.get(1);
                for (idetectorVariable val : var.getFieldMap().values()) {
                    if (val != null && val.isPolluted(-1)) {
                        baseVar.getValue().setRelatedType(val.getValue().getRelatedType());
                        break;
                    }
                }
                if (baseVar.getValue().getRelatedType() == null) {
                    for (idetectorVariable val : var.getElements().values()) {
                        if (val != null && val.isPolluted(-1)) {
                            baseVar.getValue().setRelatedType(val.getValue().getRelatedType());
                            break;
                        }
                    }
                }
                retVar = baseVar;
            }
        }
        if (retVar != null) {
            processRetVar(methodRef, retVar, pollutedPosition);
            return retVar;
        }


        if (methodRef.getTaintDigests().containsKey(pollutedPosition)) {
            String retMessage = methodRef.getTaintDigests().get(pollutedPosition);
            retVar = parsePosition(retMessage, baseVar, args, true);
            return retVar;
        }


        if (methodRef.completeAnalyze()) {

            if (methodRef.getActions().get("return") != null) {

                methodRef.setCompletedAnalyze(true);
                retVar = parsePosition(methodRef.getActions().get("return"), baseVar, args, true);

                String retMessage = methodRef.getActions().get("return");
                if (retMessage != null)
                    methodRef.getTaintDigests().put(pollutedPosition, retMessage);
                return retVar;
            }
        }


        if (dataContainer.getJdkPackMap().containsKey(cleanPackageName(methodRef.getClassname()))) {
            if(pollutedPosition.size() != 0) {
                if (baseVar == null) {
                    baseVar = idetectorVariable.makeRandomInstance();
                }
                baseVar.getValue().setType(invokedMethod.getReturnType());
                baseVar.getValue().setPolluted(true);
                if (pollutedPosition.contains(-1)) {
                    baseVar.getValue().setRelatedType(baseVar.getValue().getRelatedType());
                } else {
                    baseVar.getValue().setRelatedType(args.get(pollutedPosition.get(0)).getValue().getRelatedType());
                }
                retVar = baseVar;
                processRetVar(methodRef, retVar, pollutedPosition);
                return retVar;
            }
            return retVar;
        }


        if (aliasSet == null) {
            aliasSet = new HashSet<>();
        }
        aliasSet.add(methodRef.getSignature());

        if (methodRef.isSanitization()){
            Sanflag =true;
        }
        synchronized (aliasSet) {
            for (String signature : aliasSet) {

                MethodReference mef = dataContainer.getMethodRefBySignature(signature);

                if (mef.getAnalyzeLevel() == -1) {
                    mef.setAnalyzeLevel(context.getMethodReference().getAnalyzeLevel() + 1);
                }

                if (mef.getAnalyzeLevel() == mef.getMaxAnalyzeLevel()) {
                    if(pollutedPosition.size() != 0) {
                        if (baseVar == null) {
                            baseVar = idetectorVariable.makeRandomInstance();
                        }
                        baseVar.getValue().setType(invokedMethod.getReturnType());
                        baseVar.getValue().setPolluted(true);
                        if (pollutedPosition.contains(-1)) {
                            baseVar.getValue().setRelatedType(baseVar.getValue().getRelatedType());
                        } else {
                            baseVar.getValue().setRelatedType(args.get(pollutedPosition.get(0)).getValue().getRelatedType());
                        }
                        retVar = baseVar;
                        processRetVar(methodRef, retVar, pollutedPosition);
                        return retVar;
                    }
                    return retVar;
                }
                buildCallRelationship(cls.getName(), context, mef, dataContainer, unit, invokeType, pollutedPosition);

                Context subContext = context.createSubContext(mef);
                subContext.setPollutedArgs(pollutedPosition);

//                }
                if (Sanflag || context.isAlreadySan() || mef.isSanitization()){
                    subContext.setAlreadySan(true);
                }
                Switcher.doMethodAnalysis(subContext, dataContainer, chainContainer, mef);
                for (Map.Entry<String,String> entry: mef.getActions().entrySet()) {
                    if (entry.getKey().equals("polluteThis")) {
                        polluteThis(entry.getValue(), baseVar, true);
                    } else if (!entry.getKey().equals("return") && !entry.getValue().equals("clear")) {
                        parsePosition(entry.getValue(), baseVar, args, true);
                    }
                }
                if (mef.getActions().containsKey("return")) {
                    retVar = parsePosition(mef.getActions().get("return"), baseVar, args, true);
                    if (retVar != null)
                        break;
                }
            }
        }
        if (retVar != null) {
            processRetVar(methodRef, retVar, pollutedPosition);
            return retVar;
        }



        return null;
    }

    public static idetectorVariable parsePosition(String position, idetectorVariable baseVar, Map<Integer, idetectorVariable> args, boolean created) {
        if (position == null) return null;
        idetectorVariable retVar = null;
        String[] positions = position.split("\\|");
        for (String pos : positions) {
            if (pos.contains("&remain")) {
                pos = pos.split("&")[0];
            }
            if ("this".equals(pos)) { // this
                retVar = baseVar;
            } else if (pos.startsWith("param-")) { // param-0
                int index = Integer.valueOf(pos.split("-")[1]);
                retVar = args.get(index);

            } else if (retVar != null && StringUtils.isNumeric(pos)) {
                int index = Integer.valueOf(pos);
                idetectorVariable tempVar = retVar.getElement(index);
                if (created && tempVar == null) {
                    tempVar = idetectorVariable.makeRandomInstance();
                    boolean isPolluted = retVar.isPolluted(-1);
                    tempVar.getValue().setPolluted(isPolluted);
                    if (isPolluted) {
                        tempVar.getValue().setRelatedType(retVar.getValue().getRelatedType() + "|" + index);
                    }
                    retVar.addElement(index, tempVar);
                }
                retVar = tempVar;
            } else if (retVar != null) {
                idetectorVariable tempVar = retVar.getField(pos);
                if (created && tempVar == null) {
                    SootField field = retVar.getSootField(pos);
                    if (field != null) {
                        tempVar = retVar.getOrAddField(retVar, field);
                    }
                }
                retVar = tempVar;
            }
        }
        return retVar;
    }

    public static void polluteThis(String position, idetectorVariable baseVar, boolean created) {
        if (position == null) return ;
        String[] positions = position.split("\\|");
        for (String pos : positions) {
            if ("this".equals(pos)) { // this
                baseVar.getValue().setPolluted(true);
            } else if (StringUtils.isNumeric(pos)) {
                int index = Integer.valueOf(pos);
                idetectorVariable tempVar = baseVar.getElement(index);
                if (created && tempVar == null) {
                    tempVar = idetectorVariable.makeRandomInstance();
                }
                tempVar.getValue().setPolluted(true);
                tempVar.getValue().setRelatedType(baseVar.getValue().getRelatedType() + "|" + index);
                baseVar.addElement(index, tempVar);
            } else if (pos.length() > 0) {
                idetectorVariable tempVar = baseVar.getField(pos);
                if (tempVar != null)
                    tempVar.getValue().setPolluted(true);
                if (created && tempVar == null) {
                    SootField field = baseVar.getSootField(pos);
                    if (field != null) {
                        tempVar = baseVar.getOrAddField(baseVar, field);
                        tempVar.getValue().setPolluted(true);
                    }
                }
            }
        }
    }

    public static List<Integer> pollutedPositionAnalysis(idetectorVariable baseVar, Map<Integer, idetectorVariable> args) {
        HashSet<Integer> positionsSet = new HashSet<>();
        // baseVar
        if (baseVar != null && baseVar.isPolluted(-1)) {
            positionsSet.add(-1);
        }
        // args
        for (Map.Entry<Integer, idetectorVariable> entry : args.entrySet()) {
            Integer loc = entry.getKey();
            idetectorVariable var = entry.getValue();
            if (var != null && var.isPolluted(-1)) {
                positionsSet.add(loc);
            }
            for (idetectorVariable val : var.getFieldMap().values()) {
                if (val != null && val.isPolluted(-1)) {
                    positionsSet.add(loc);
                }
            }
            for (idetectorVariable val : var.getElements().values()) {
                if (val != null && val.isPolluted(-1)) {
                    positionsSet.add(loc);
                }
            }
        }

        return new ArrayList<>(positionsSet);
    }


    public static List<Integer> propagatePositionAnalysis(idetectorVariable baseVar, Map<Integer, idetectorVariable> args) {
        List<Integer> positions = new ArrayList<>();
        // baseVar
        positions.add(getPropagatePosition(baseVar));

        // args
        for (idetectorVariable var : args.values()) {
            positions.add(getPropagatePosition(var));
        }

        return positions;
    }

    public static int getPropagatePosition(idetectorVariable var) {
        if (var != null) {
            String related = null;
            if (var.isPolluted(-1)) {
                related = var.getValue().getRelatedType();
            } else if (var.containsPollutedVar(new ArrayList<>())) {
                related = var.getFirstPollutedVarRelatedType();
            }
            if (related != null) {
                if (related.startsWith("this")) {
                    return -1;
                } else if (related.startsWith("param-")) {
                    String[] pos = related.split("\\|");
                    return Integer.parseInt(pos[0].split("-")[1]);
                }
            }
        }
        return -2;
    }

    public static void buildCallRelationship(String classname, Context context, MethodReference targetMethodRef, DataContainer dataContainer, Unit unit, String invokeType, List<Integer> pollutedPosition) {
        MethodReference sourceMethodRef = context.getMethodReference();
        if (sourceMethodRef == null || targetMethodRef == null) {

            return;
        }

        if (!targetMethodRef.isIgnore()) {
            if ("java.lang.String".equals(classname)
                    && ("equals".equals(targetMethodRef.getName()) || "hashCode".equals(targetMethodRef.getName()) || "length".equals(targetMethodRef.getName())))
                return;

            if ("java.lang.StringBuilder".equals(classname)
                    && ("toString".equals(targetMethodRef.getName()) || "hashCode".equals(targetMethodRef.getName())))
                return;

            Call call = Call.newInstance(sourceMethodRef, targetMethodRef);
            call.setRealCallType(classname);
            call.setInvokerType(invokeType);
            call.setPollutedPosition(pollutedPosition);
            call.setUnit(unit);

            call.setLineNum(unit.getJavaSourceStartLineNumber());
            if (!targetMethodRef.getCallEdge().contains(call)) {
                targetMethodRef.getCallEdge().add(call);
                dataContainer.store(call);
            }
        }
    }

    public static idetectorVariable extractBaseVarFromInvokeExpr(InvokeExpr invokeExpr, Context context) {
        idetectorVariable baseVar = null;
        List<ValueBox> valueBoxes = invokeExpr.getUseBoxes();
        for (ValueBox box : valueBoxes) {
            Value value = box.getValue();
            if (box instanceof JimpleLocalBox) {
                baseVar = context.getOrAdd(value);
                break;
            }
        }
        if (baseVar == null && invokeExpr instanceof SpecialInvokeExpr) {
            baseVar = context.getOrAdd(context.getThisVar());
        }
        return baseVar;
    }

    public static Map<Integer, idetectorVariable> extractArgsFromInvokeExpr(InvokeExpr invokeExpr, Context context) {
        Map<Integer, idetectorVariable> args = new HashMap<>();
        for (int i = 0; i < invokeExpr.getArgCount(); i++) {
            idetectorVariable var = context.getOrAdd(invokeExpr.getArg(i));
            if (var != null) { // 不提取常量

                args.put(i, var);
            }
        }
        return args;
    }

    /**
     *
     * @param methodRef
     * @param retVar
     * @param pollutedPosition
     */
    public static void processRetVar(MethodReference methodRef, idetectorVariable retVar, List<Integer> pollutedPosition) {
        if (retVar == null)
            return;

        methodRef.setCompletedAnalyze(true);
        if (retVar.getValue().getRelatedType() != null) {

            methodRef.addAction("return", retVar.getValue().getRelatedType());

            methodRef.getTaintDigests().put(pollutedPosition, methodRef.getActions().get("return"));
        }
    }

    /**
     *
     * @param methodRef
     * @param body
     */
    public static void addAnalyzeStmts(DataContainer dataContainer, MethodReference methodRef, JimpleBody body) {

        for (Unit unit : body.getUnits()) {
            if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;
                methodRef.addAnalyzeStmt(stmt);
            }
        }

    }


    /**
     *
     * @param className
     * @param paramType
     * @return
     */
    public static String transformBootstrap(String className, String paramType) {
        className = clean(className);
        String regSignature = "";
        if (paramType == null || paramType.equals("")) {
            regSignature += "<";
            regSignature += className.replace(".", "\\.").replace("[", "\\[").replace("]", "\\]")
                    .replaceFirst("\\$", ": ([a-zA-Z]+\\\\.)*[a-zA-Z]+ ")
                    .replace("$", "\\$");
            regSignature += "\\([\\w ,\\.]+\\)>";
        } else {
            paramType = paramType.replace(".", "\\.").replace("$", "\\$")
                    .replace("[", "\\[").replace("]", "\\]");
            String methodName = className.substring(className.indexOf("$") + 1);
            methodName = methodName.replace(".", "\\.").replace("$", "\\$");
            regSignature = String.format("<%s: ([a-zA-Z]+\\.)*[a-zA-Z]+ %s\\([\\w ,\\.]+\\)>", paramType, methodName);
        }
        return regSignature;
    }


    public static List<MethodReference> getMethodRefBySignature(DataContainer dataContainer, String signature) {
        List<MethodReference> results = new ArrayList<>();
        MethodReference calleeMethodRef = dataContainer.getMethodRefBySignature(signature);

        if (signature.contains("bootstrap$")) {

            String className = signature.split(": ")[0].substring(1);
            String paramType = signature.substring(signature.indexOf('(') + 1, signature.indexOf(')'));
            if (paramType.contains(",")) {
                paramType = "";
            }
            String regSignature = Switcher.transformBootstrap(className, paramType);
            MethodReference realMethodRef = dataContainer.getMethodRefByRegSignature(regSignature);
            if (realMethodRef != null)
                results.add(realMethodRef);
        } else if (calleeMethodRef != null) {
            if (calleeMethodRef.isStatic())
                results.add(calleeMethodRef);
            else {

                Set<String> aliasSet = dataContainer.getSavedAliasByFather(calleeMethodRef.getId());
                if (aliasSet == null) {
                    aliasSet = new HashSet<>();
                    aliasSet.add(calleeMethodRef.getSignature());
                }
                for (String alias : aliasSet) {
                    MethodReference realMethodRef = dataContainer.getMethodRefBySignature(alias);
                    if (realMethodRef != null)
                        results.add(realMethodRef);
                }
            }
        }
        return results;
    }

    public static Boolean checkPrimType(Type type) {
        if (type instanceof PrimType) {
            return true;
        } else if (type instanceof RefType) {
            List<String> prims = new ArrayList<>();
            prims.add("java.lang.Integer");
            prims.add("java.lang.Boolean");
            prims.add("java.lang.Byte");
            prims.add("java.lang.Short");
            prims.add("java.lang.Long");
            prims.add("java.lang.Float");
            prims.add("java.lang.Double");
            prims.add("java.lang.Character");
            if (prims.contains(((RefType) type).getClassName())) {
                return true;
            }
        }
        return false;
    }
}
