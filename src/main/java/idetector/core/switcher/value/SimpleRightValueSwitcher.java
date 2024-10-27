package idetector.core.switcher.value;

import lombok.extern.slf4j.Slf4j;
import soot.Local;
import soot.Value;
import soot.jimple.*;
import idetector.config.GlobalConfiguration;
import idetector.core.data.idetectorVariable;
import idetector.core.switcher.Switcher;


@Slf4j
public class SimpleRightValueSwitcher extends ValueSwitcher {
    @Override
    public void caseInterfaceInvokeExpr(InterfaceInvokeExpr v) {
        caseInvokeExpr(v, "InterfaceInvoke");
    }

    @Override
    public void caseSpecialInvokeExpr(SpecialInvokeExpr v) {
        caseInvokeExpr(v, "SpecialInvoke");
    }

    @Override
    public void caseStaticInvokeExpr(StaticInvokeExpr v) {
        caseInvokeExpr(v, "StaticInvoke");
    }

    @Override
    public void caseVirtualInvokeExpr(VirtualInvokeExpr v) {
        caseInvokeExpr(v, "VirtualInvoke");
    }

    @Override
    public void caseDynamicInvokeExpr(DynamicInvokeExpr v) {
        defaultCase(v);
    }

    @Override
    public void caseCastExpr(CastExpr v) {
        Value value = v.getOp();
        value.apply(this);
    }

//    @Override
//    public void caseNewArrayExpr(NewArrayExpr v) {
//        idetectorVariable var = idetectorVariable.makeRandomInstance();
//        var.getValue().setArray(true);
//        setResult(var);
//    }
//
//    @Override
//    public void caseNewMultiArrayExpr(NewMultiArrayExpr v) {
//        defaultCase(v);
//    }
//
    @Override
    public void caseNewExpr(NewExpr v) {
        setResult(idetectorVariable.makeRandomInstance());
    }

    @Override
    public void caseArrayRef(ArrayRef v) {
        idetectorVariable var = null;
        Value baseValue = v.getBase();
        Value indexValue = v.getIndex();
        idetectorVariable baseVar = context.getOrAdd(baseValue);
        if (indexValue instanceof IntConstant) {
            int index = ((IntConstant) indexValue).value;
            var = baseVar.getElement(index);
        }else if(indexValue instanceof Local){

        }
        if(var == null){
            setResult(baseVar);
        }else{
            setResult(var);
        }
    }

    @Override
    public void caseLocal(Local v) {
        setResult(context.getOrAdd(v));
    }

    @Override
    public void caseStaticFieldRef(StaticFieldRef v) {
        idetectorVariable var = context.getGlobalMap().get(v);
        setResult(var);
    }

    @Override
    public void caseInstanceFieldRef(InstanceFieldRef v) {
        idetectorVariable var = context.getOrAdd(v);
        setResult(var);
    }

    public void caseInvokeExpr(InvokeExpr invokeExpr, String invokeType){

        setResult(Switcher.doInvokeExprAnalysis(unit, invokeExpr, dataContainer, chainContainer, context));
    }
}
