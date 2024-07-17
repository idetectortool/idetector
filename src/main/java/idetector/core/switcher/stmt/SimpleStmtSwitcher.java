package idetector.core.switcher.stmt;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JimpleLocalBox;
import idetector.config.GlobalConfiguration;
import idetector.core.data.idetectorVariable;
import idetector.core.switcher.Switcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Getter
@Setter
@Slf4j
public class SimpleStmtSwitcher extends StmtSwitcher {

    @Override
    public void caseInvokeStmt(InvokeStmt stmt) {
        if (methodRef.filterStmt(stmt)) {
            return;
        }
        // extract baseVar and args
        InvokeExpr ie = stmt.getInvokeExpr();
        if("<java.lang.Object: void <init>()>".equals(ie.getMethodRef().getSignature())) return;
        Object result = Switcher.doInvokeExprAnalysis(stmt, ie, dataContainer, chainContainer, context);

        idetectorVariable rvar = null;
        if(result instanceof idetectorVariable){
            rvar = (idetectorVariable) result;
        }


        if(rvar != null && rvar.getValue() != null && Switcher.checkPrimType(rvar.getValue().getType())){
            rvar = null;
        }

        if(context.isQuote() && rvar != null && rvar.getValue().isPolluted()) {
            Integer setIndex = -1;
            String uuid = rvar.getUuid();
            for(int i = 0; i < context.getQuoteVars().size(); i++) {

                if (context.getQuoteVars().get(i).stream().filter(s->s.getUuid().equals(uuid)).count() > 0) {
                    setIndex = i;
                    break;
                }
            }
            if (setIndex > -1) {
                for(idetectorVariable var: context.getQuoteVars().get(setIndex)) {
                    var.getValue().setPolluted(true);
                    context.getOrAdd(var.getOrigin()).getValue().setPolluted(true);
                }
            }
        }

        methodRef.completeStmt(stmt);
    }

    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        if (methodRef.filterStmt(stmt)) {
            return;
        }
        Value lop = stmt.getLeftOp();
        Value rop = stmt.getRightOp();
        idetectorVariable rvar = null;
        boolean unbind = false;
        rightValueSwitcher.setUnit(stmt);
        rightValueSwitcher.setContext(context);
        rightValueSwitcher.setDataContainer(dataContainer);
        rightValueSwitcher.setChainContainer(chainContainer);
        rightValueSwitcher.setResult(null);
        rop.apply(rightValueSwitcher);
        Object result = rightValueSwitcher.getResult();


        if(context.isQuote() && stmt.containsInvokeExpr()) {
            Boolean containFlag = false;
            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            idetectorVariable baseVar = Switcher.extractBaseVarFromInvokeExpr(invokeExpr, context);
            Map<Integer, idetectorVariable> args = Switcher.extractArgsFromInvokeExpr(invokeExpr, context);
            for(Set<idetectorVariable> quoteVar: context.getQuoteVars()) {
                if (baseVar != null) {
                    if (quoteVar.stream().anyMatch(o -> o.getName().equals(baseVar.getName()))) {
                        quoteVar.add(context.getOrAdd(lop));
                        break;
                    }
                }
                for(idetectorVariable var: args.values()) {
                    if (quoteVar.stream().anyMatch(o -> o.getName().equals(var.getName()))) {
                        containFlag = true;
                        quoteVar.add(context.getOrAdd(lop));
                        break;
                    }
                }
                if (containFlag) break;
            }
        }

        if(result instanceof idetectorVariable){
            rvar = (idetectorVariable) result;
        }

        if(rop instanceof Constant && !(rop instanceof StringConstant)){
            unbind = true;
        }

        if(rvar != null && rvar.getValue() != null && Switcher.checkPrimType(rvar.getValue().getType())){
            rvar = null;
        }

        if(context.isQuote() && rvar != null && rvar.getValue().isPolluted()) {
            Integer setIndex = -1;
            String uuid = rvar.getUuid();
            for(int i = 0; i < context.getQuoteVars().size(); i++) {

                if (context.getQuoteVars().get(i).stream().filter(s->s.getUuid().equals(uuid)).count() > 0) {
                    setIndex = i;
                    break;
                }
            }
            if (setIndex > -1) {
                for(idetectorVariable var: context.getQuoteVars().get(setIndex)) {
                    var.getValue().setPolluted(true);
                    context.getOrAdd(var.getOrigin()).getValue().setPolluted(true);
                }
            }
        }


        if(rvar != null || unbind){
            leftValueSwitcher.setContext(context);
            leftValueSwitcher.setMethodRef(methodRef);
            leftValueSwitcher.setRvar(rvar);
            leftValueSwitcher.setUnbind(unbind);
            lop.apply(leftValueSwitcher);
        }
        context.setQuote(false);
        methodRef.completeStmt(stmt);
    }


    @Override
    public void caseIdentityStmt(IdentityStmt stmt) {
        if (methodRef.filterStmt(stmt)) {
            return;
        }
        Value lop = stmt.getLeftOp();
        Value rop = stmt.getRightOp();
        if(rop instanceof ThisRef){
            context.bindThis(lop);
        }else if(rop instanceof ParameterRef){
            ParameterRef pr = (ParameterRef)rop;
            context.bindArg((Local)lop, pr.getIndex());
        }
        methodRef.completeStmt(stmt);
    }


//    @Override
//    public void caseIfStmt(IfStmt stmt) {
//        super.caseIfStmt(stmt);
//    }
//
//    @Override
//    public void caseRetStmt(RetStmt stmt) {
//        super.caseRetStmt(stmt);
//    }

    @Override
    public void caseReturnStmt(ReturnStmt stmt) {
        if (methodRef.filterStmt(stmt)) {
            return;
        }
        Value value = stmt.getOp();
        idetectorVariable var = null;

        if(context.getReturnVar() != null && context.getReturnVar().containsPollutedVar(new ArrayList<>())) return;
        rightValueSwitcher.setUnit(stmt);
        rightValueSwitcher.setContext(context);
        rightValueSwitcher.setDataContainer(dataContainer);
        rightValueSwitcher.setChainContainer(chainContainer);
        rightValueSwitcher.setResult(null);
        value.apply(rightValueSwitcher);
        var = (idetectorVariable) rightValueSwitcher.getResult();
        context.setReturnVar(var);

        if (var != null)
            methodRef.addAction("return", var.getValue().getRelatedType());
        methodRef.completeStmt(stmt);
    }

    @Override
    public void caseReturnVoidStmt(ReturnVoidStmt stmt) {
        if (methodRef.filterStmt(stmt)) {
            return;
        }

        String polluteMsg = "";
        idetectorVariable thisVar =  context.getOrAdd(context.getThisVar());
        if (thisVar == null) return;
        if (thisVar.isPolluted(-1)) {
            if (!polluteMsg.equals("")) polluteMsg += "|";
            polluteMsg += "this";
        }
        for (idetectorVariable field: thisVar.getFieldMap().values()) {
            if (field.getValue().isPolluted()) {
                // "return": "this|<javax.swing.plaf.nimbus.NimbusLookAndFeel: javax.swing.UIDefaults uiDefaults>"
                if (!polluteMsg.equals("")) polluteMsg += "|";
                String className = context.getMethodSignature().split(":")[0].substring(1);
                polluteMsg += String.format("<%s: %s %s>", className, field.getValue().getType(), field.getName());
            }
        }
        for (int i = 0; i < thisVar.getElements().size(); i++) {
            idetectorVariable element = thisVar.getElements().get(i);
            if (element.getValue().isPolluted()) {
                if (!polluteMsg.equals("")) polluteMsg += "|";
                polluteMsg += Integer.toString(i);
            }
        }
        if (!polluteMsg.equals("")) methodRef.addAction("polluteThis", polluteMsg);
        methodRef.completeStmt(stmt);
    }

}
