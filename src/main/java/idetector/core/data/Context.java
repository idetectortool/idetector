package idetector.core.data;

import lombok.Data;
import soot.*;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import idetector.core.switcher.Switcher;
import idetector.dal.caching.bean.ref.MethodReference;

import java.util.*;


@Data
public class Context {

    private String methodSignature;
    private MethodReference methodReference;
    private Map<Local, idetectorVariable> initialMap;

    private Local thisVar;

    private Map<Integer, Local> args = new HashMap<>();

    private Context preContext;

    private int depth;

    private Map<Local, idetectorVariable> localMap;
    private Map<Local, Set<idetectorVariable>> maybeLocalMap = new HashMap<>();
    private Map<Value, idetectorVariable> globalMap = new HashMap<>();

    private idetectorVariable returnVar;

    private List<Integer> pollutedArgs = new ArrayList<>();

    private List<HashSet<idetectorVariable>> quoteVars = new ArrayList<>();

    private boolean quote = false;

    private boolean alreadySan = false;

    public Context() {
        this.localMap = new HashMap<>();
    }

    public Context(MethodReference methodReference, Context preContext, int depth) {
        this.methodSignature = methodReference.getSignature();
        this.methodReference = methodReference;
        this.depth = depth;
        this.preContext = preContext;
        this.localMap = new HashMap<>();
    }

    public static Context newInstance(MethodReference methodReference) {
        return new Context(methodReference, null, 0);
    }


    public Context createSubContext(MethodReference methodReference) {
        Context subContext = new Context(methodReference, this, depth + 1);
        subContext.setGlobalMap(globalMap); // 同步所有globalMap
        return subContext;
    }


    public idetectorVariable getOrAdd(Value sootValue) {
        idetectorVariable var = null;
        if (sootValue instanceof Local) {
            var = localMap.get(sootValue);
            if (var == null) {
                idetectorVariable tempVar = initialMap.get(sootValue);
                if (tempVar != null) {
                    var = tempVar.deepClone(new ArrayList<>());
                    localMap.put((Local) sootValue, var);
                }
            }
            if (var == null) {
                var = idetectorVariable.makeLocalInstance((Local) sootValue);
                localMap.put((Local) sootValue, var);
            }
        } else if (sootValue instanceof StaticFieldRef) {
            var = globalMap.get(sootValue);
            if (var == null) {
                var = idetectorVariable.makeStaticFieldInstance((StaticFieldRef) sootValue);
                globalMap.put(sootValue, var);
            }
        } else if (sootValue instanceof InstanceFieldRef) {
            InstanceFieldRef ifr = (InstanceFieldRef) sootValue;
            SootField sootField = ifr.getField();
            SootFieldRef fieldRef = ifr.getFieldRef();

            String signature = null;
            if (sootField != null) {
                signature = sootField.getSignature();
            } else if (fieldRef != null) {
                signature = fieldRef.getSignature();
            }

            Value base = ifr.getBase();
            if (base instanceof Local) {
                idetectorVariable baseVar = getOrAdd(base);
                var = baseVar.getField(signature);
                if (var == null) {
                    if (sootField != null) {
                        var = baseVar.getOrAddField(baseVar, sootField);
                    } else if (fieldRef != null) {
                        var = baseVar.getOrAddField(baseVar, fieldRef);
                    }
                }
                if (var != null) {
                    var.setOrigin(ifr);
                }
            }
        }
        return var;
    }

    public void bindThis(Value value) {
        if (value instanceof Local) {
            thisVar = (Local) value;
            idetectorVariable var = getOrAdd(thisVar);
            var.setThis(true);

            if (pollutedArgs.contains(-1)) {
                var.getValue().setPolluted(true);
            }

            var.getValue().setRelatedType("this");
            var.getFieldMap().forEach((fieldName, fieldVar) -> {
                if (fieldVar != null) {
                    if (var.getValue().isPolluted()) {
                        fieldVar.getValue().setPolluted(true);
                    }
                    fieldVar.getValue().setRelatedType("this|" + fieldName);
                }
            });
        }
    }


    public void bindArg(Local local, int paramIndex) {
        idetectorVariable paramVar = getOrAdd(local);
        paramVar.setParam(true);
        paramVar.setParamIndex(paramIndex);

        if (preContext == null || pollutedArgs.contains(paramIndex)) {
            if (!(Switcher.checkPrimType(local.getType()))) {
                paramVar.getValue().setPolluted(true);
            }
        }
        paramVar.getValue().setRelatedType("param-" + paramIndex);

        paramVar.getFieldMap().forEach((fieldName, fieldVar) -> {
            if (fieldVar != null) {
                if (paramVar.getValue().isPolluted()) {
                    fieldVar.getValue().setPolluted(true);
                }
                fieldVar.getValue().setRelatedType("param-" + paramIndex + "|" + fieldName);
            }
        });
        args.put(paramIndex, local);
    }

    public void unbind(Value value) {
        if (localMap.containsKey(value)) {
            localMap.remove(value);
        } else globalMap.remove(value);
    }


    public boolean isInRecursion() {
        Context context = getPreContext();
        while (context != null) {
            if (context.getMethodSignature().equals(getMethodSignature())) {
                return true;
            }
            context = context.getPreContext();
        }
        return false;
    }

    public void clear() {
        globalMap.clear();
        maybeLocalMap.clear();
    }

}
