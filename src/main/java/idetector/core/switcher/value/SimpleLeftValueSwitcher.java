package idetector.core.switcher.value;

import lombok.Getter;
import lombok.Setter;
import soot.Local;
import soot.PrimType;
import soot.SootFieldRef;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.IntConstant;
import soot.jimple.StaticFieldRef;
import idetector.core.data.idetectorVariable;
import idetector.core.switcher.Switcher;


@Getter
@Setter
public class SimpleLeftValueSwitcher extends ValueSwitcher {

    /**
     * case a = rvar
     * @param v
     */
    public void caseLocal(Local v) {

        if(Switcher.checkPrimType(v.getType())) return;

        idetectorVariable var = context.getOrAdd(v);

        generateAction(var, rvar, -1, unbind);
        if(unbind){
            var.clearVariableStatus();
        }else{
            var.assign(rvar, false);
        }
    }

    /**
     * case Class.field = rvar
     * @param v
     */
    public void caseStaticFieldRef(StaticFieldRef v) {
        // 剔除无用的类属性传递
        if(Switcher.checkPrimType(v.getType())) return;
        idetectorVariable var = context.getOrAdd(v);
        if(unbind){
            context.unbind(v);
        } else {
            var.assign(rvar, false);
        }
    }

    /**
     * case a[index] = rvar
     * @param v
     */
    public void caseArrayRef(ArrayRef v) {
        Value baseValue = v.getBase();
        Value indexValue = v.getIndex();
        idetectorVariable baseVar = context.getOrAdd(baseValue);

        if (indexValue instanceof IntConstant) {
            int index = ((IntConstant) indexValue).value;
            generateAction(baseVar, rvar, index, unbind);
            if(unbind){
                baseVar.clearElementStatus(index);
            }else{
                baseVar.assign(index, rvar);
            }
        }else if(indexValue instanceof Local){

            int size = baseVar.getElements().size();
            generateAction(baseVar, rvar, size, unbind);
            if(!unbind){
                baseVar.assign(size, rvar);
            }
        }
    }

    /**
     * case a.f = rvar
     * @param v
     */
    public void caseInstanceFieldRef(InstanceFieldRef v) {
        SootFieldRef sfr = v.getFieldRef();

        if(Switcher.checkPrimType(v.getType())) return;
        idetectorVariable fieldVar = context.getOrAdd(v);

        if(unbind){
            fieldVar.clearVariableStatus();
        }else{
            fieldVar.assign(rvar, false);
        }
    }

    public void generateAction(idetectorVariable lvar, idetectorVariable rvar, int index, boolean unbind){
        if(!reset) return;
        if(unbind && lvar.isPolluted(-1)){
            if(index != -1){
                methodRef.addAction(lvar.getValue().getRelatedType() + "|"+index, "clear");
            }else{
                methodRef.addAction(lvar.getValue().getRelatedType(), "clear");
            }
        }else if(lvar.isPolluted(-1)){
            if(rvar != null && rvar.isPolluted(-1)){
                if(index != -1){
                    methodRef.addAction(lvar.getValue().getRelatedType() + "|"+index, rvar.getValue().getRelatedType());
                }else if(lvar.getValue().getRelatedType() != null && !lvar.getValue().getRelatedType().equals(rvar.getValue().getRelatedType())){
                    methodRef.addAction(lvar.getValue().getRelatedType(), rvar.getValue().getRelatedType());
                }

            }else{
                if(index != -1){
                    methodRef.addAction(lvar.getValue().getRelatedType() + "|"+index, "clear");
                }else{
                    methodRef.addAction(lvar.getValue().getRelatedType(), "clear");
                }
            }
        }
    }

}
