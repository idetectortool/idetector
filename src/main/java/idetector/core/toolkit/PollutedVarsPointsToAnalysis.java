package idetector.core.toolkit;

import lombok.Getter;
import lombok.Setter;
import soot.*;
import soot.jimple.ArrayRef;
import soot.jimple.InstanceFieldRef;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import idetector.core.container.ChainContainer;
import idetector.core.switcher.Switcher;
import idetector.dal.caching.bean.ref.MethodReference;
import idetector.core.data.Context;
import idetector.core.container.DataContainer;
import idetector.core.data.idetectorVariable;
import idetector.core.switcher.stmt.SimpleStmtSwitcher;
import idetector.core.switcher.stmt.StmtSwitcher;
import idetector.core.switcher.value.SimpleLeftValueSwitcher;
import idetector.core.switcher.value.SimpleRightValueSwitcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@Setter
@Getter
public class PollutedVarsPointsToAnalysis extends ForwardFlowAnalysis<Unit, Map<Local, idetectorVariable>> {


    private Context context;
    private DataContainer dataContainer;
    private ChainContainer chainContainer;
    private Map<Local, idetectorVariable> emptyMap;

    private Map<Local, idetectorVariable> initialMap;
    private StmtSwitcher stmtSwitcher;
    private MethodReference methodRef;
    private Body body;
    /**
     * Construct the analysis from a DirectedGraph representation of a Body.
     *
     * @param graph
     */
    public PollutedVarsPointsToAnalysis(DirectedGraph<Unit> graph) {
        super(graph);
        emptyMap = new HashMap<>();
        initialMap = new HashMap<>();

    }

    public void doAnalysis(){
        for(ValueBox box:body.getUseAndDefBoxes()){
            Value value = box.getValue();
            Type type = value.getType();

            if (Switcher.checkPrimType(type)) {
                continue;
            }

            if(value instanceof Local && !initialMap.containsKey(value)){
                initialMap.put((Local) value, idetectorVariable.makeLocalInstance((Local) value));
            }else if(value instanceof InstanceFieldRef){
                InstanceFieldRef ifr = (InstanceFieldRef) value;
                SootField sootField = ifr.getField();
                SootFieldRef sfr = ifr.getFieldRef();

                String signature = null;
                if(sootField != null){
                    signature = sootField.getSignature();
                }else if(sfr != null){
                    signature = sfr.getSignature();
                }

                Value base = ifr.getBase();
                if(base instanceof Local){
                    idetectorVariable baseVar = initialMap.get(base);
                    if(baseVar == null){
                        baseVar = idetectorVariable.makeLocalInstance((Local) base);
                        initialMap.put((Local) base, baseVar);
                    }
                    idetectorVariable fieldVar = baseVar.getField(signature);
                    if(fieldVar == null){
                        if(sootField != null){
                            fieldVar = idetectorVariable.makeFieldInstance(baseVar, sootField);
                        }else if(sfr != null){
                            fieldVar = idetectorVariable.makeFieldInstance(baseVar, sfr);
                        }
                        if(fieldVar != null && signature != null){
                            fieldVar.setOrigin(value);
                            baseVar.addField(signature, fieldVar);
                        }
                    }
                }
            }else if(value instanceof ArrayRef){
                ArrayRef v = (ArrayRef) value;
                Value base = v.getBase();
                if(base instanceof Local){
                    idetectorVariable baseVar = initialMap.get(base);
                    if(baseVar == null){
                        baseVar = idetectorVariable.makeLocalInstance((Local) base);
                        initialMap.put((Local) base, baseVar);
                    }
                }
            }
        }
        super.doAnalysis();
    }

    @Override
    protected void flowThrough(Map<Local, idetectorVariable> in, Unit d, Map<Local, idetectorVariable> out) {
        Map<Local, idetectorVariable> newIn = new HashMap<>();
        copy(in, newIn);
        context.setLocalMap(newIn);
        context.setInitialMap(initialMap);
        stmtSwitcher.setContext(context);
        stmtSwitcher.setDataContainer(dataContainer);
        stmtSwitcher.setChainContainer(chainContainer);
        d.apply(stmtSwitcher);
        out.putAll(context.getLocalMap());

    }


    @Override
    protected Map<Local, idetectorVariable> newInitialFlow() {
        return new HashMap<>(emptyMap);
    }

    @Override
    protected Map<Local, idetectorVariable> entryInitialFlow() { return new HashMap<>(emptyMap); }

    @Override
    protected void merge(Map<Local, idetectorVariable> in1, Map<Local, idetectorVariable> in2, Map<Local, idetectorVariable> out) {

        copy(in1, out);

        in2.forEach((local, in2Var) -> {
            idetectorVariable outVar = out.get(local);
            if(outVar != null){
                outVar.union(in2Var);
            }else{
                out.put(local, in2Var);
            }
        });
    }

    @Override
    protected void copy(Map<Local, idetectorVariable> source, Map<Local, idetectorVariable> dest) {
        dest.clear();
        for (Map.Entry<Local, idetectorVariable> entry : source.entrySet()) {
            Local value = entry.getKey();
            idetectorVariable variable = entry.getValue();
            dest.put(value, variable.deepClone(new ArrayList<>()));
        }
    }

    public Map<Local, idetectorVariable> filterPollute(Map<Local, idetectorVariable> localMap){

        Map<Local, idetectorVariable> tmp = new HashMap<>();
        localMap.forEach((local, var) -> {
            if (var.isPolluted(-1)){
                tmp.put(local, var);
            }
            if (var.getValue().isFunction()) {
                tmp.put(local, var);
            }
        });
        return tmp;
    }

    public static PollutedVarsPointsToAnalysis makeDefault(MethodReference methodRef,
                                                           Body body,
                                                           DirectedGraph<Unit> graph,
                                                           DataContainer dataContainer,
                                                           ChainContainer chainContainer,
                                                           Context context,
                                                           boolean reset){
        PollutedVarsPointsToAnalysis analysis = new PollutedVarsPointsToAnalysis(graph);

        StmtSwitcher switcher = new SimpleStmtSwitcher();
        SimpleLeftValueSwitcher leftSwitcher = new SimpleLeftValueSwitcher();
        leftSwitcher.setReset(reset);
        switcher.setReset(reset);
        switcher.setMethodRef(methodRef);
        switcher.setLeftValueSwitcher(leftSwitcher);
        switcher.setRightValueSwitcher(new SimpleRightValueSwitcher());

        analysis.setBody(body);
        analysis.setDataContainer(dataContainer);
        analysis.setChainContainer(chainContainer);
        analysis.setStmtSwitcher(switcher);
        analysis.setContext(context);
        analysis.setMethodRef(methodRef);

        analysis.doAnalysis();
        return analysis;
    }
}
