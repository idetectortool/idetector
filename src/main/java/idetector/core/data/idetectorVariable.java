package idetector.core.data;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import soot.*;
import soot.jimple.FieldRef;
import soot.jimple.StaticFieldRef;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Getter
@Setter
@Slf4j
public class idetectorVariable {

    private String uuid;
    private String name;
    private Value origin;
    private boolean isThis = false;
    private boolean isParam = false;
    private int paramIndex;

    private idetectorValue value = null;
    private idetectorVariable owner = null;
    private String firstPollutedVarRelatedType = null;

    // fields
    private Map<String, idetectorVariable> fieldMap = new HashMap<>();
    // arrays
    private Map<Integer, idetectorVariable> elements = new HashMap<>();

    private idetectorVariable() {
        uuid = UUID.randomUUID().toString();
    }

    private idetectorVariable(String uuid) {
        this.uuid = uuid;
    }

    private idetectorVariable(Value sootValue, idetectorValue idetectorValue) {
        if (sootValue instanceof Local) {
            name = ((Local) sootValue).getName();
        } else if (sootValue instanceof FieldRef) {
            FieldRef fr = (FieldRef) sootValue;
            name = idetectorValue.getTypeName() + "|" + fr.getFieldRef().name();
        } else {
            name = sootValue.toString();
        }
        uuid = UUID.randomUUID().toString();
        value = idetectorValue;
        origin = sootValue;
    }

    public static idetectorVariable makeLocalInstance(Local local) {
        idetectorValue ideValue = idetectorValue.newInstance(local);
        return new idetectorVariable(local, ideValue);
    }

    public static idetectorVariable makeStaticFieldInstance(StaticFieldRef staticFieldRef) {
        SootField sootField = staticFieldRef.getField();
        idetectorVariable field = null;
        if (sootField != null) {
            field = makeFieldInstance(null, sootField);
        } else {
            SootFieldRef sootFieldRef = staticFieldRef.getFieldRef();
            field = makeFieldInstance(null, sootFieldRef);
        }
        field.setOrigin(staticFieldRef);
        return field;
    }

    public static idetectorVariable makeFieldInstance(idetectorVariable baseVar, SootField sootField) {
        idetectorValue idetectorValue = new idetectorValue();
        idetectorVariable fieldVar = new idetectorVariable();
        idetectorValue.setType(sootField.getType());
        idetectorValue.setTypeName(sootField.getType().toString());
        idetectorValue.setField(true);
        idetectorValue.setStatic(sootField.isStatic());
        idetectorValue.setArray(idetectorValue.isArrayType(sootField.getType()));
        fieldVar.setName(sootField.getName());
        fieldVar.setOwner(baseVar);
        fieldVar.setValue(idetectorValue);

        if (baseVar != null && baseVar.isPolluted(-1)) {
            idetectorValue.setPolluted(true);
            String type = baseVar.getValue().getRelatedType();

            if (type != null && type.contains(sootField.getSignature())) {
                idetectorValue.setRelatedType(type);
            } else {
                idetectorValue.setRelatedType(type + "|" + sootField.getSignature());
            }

        }

        return fieldVar;
    }

    public static idetectorVariable makeFieldInstance(idetectorVariable baseVar, SootFieldRef sfr) {
        idetectorValue idetectorValue = new idetectorValue();
        idetectorVariable fieldVar = new idetectorVariable();
        idetectorValue.setType(sfr.type());
        idetectorValue.setTypeName(sfr.type().toString());
        idetectorValue.setField(true);
        idetectorValue.setStatic(sfr.isStatic());
        idetectorValue.setArray(idetectorValue.isArrayType(sfr.type()));
        fieldVar.setName(sfr.name());
        fieldVar.setOwner(baseVar);
        fieldVar.setValue(idetectorValue);

        if (baseVar != null && baseVar.isPolluted(-1)) {
            idetectorValue.setPolluted(true);
            String type = baseVar.getValue().getRelatedType();

            if (type != null && type.contains(sfr.getSignature())) {
                idetectorValue.setRelatedType(type);
            } else {
                idetectorValue.setRelatedType(type + "|" + sfr.getSignature());
            }

        }

        return fieldVar;
    }


    public static idetectorVariable makeRandomInstance() {
        idetectorValue idetectorValue = new idetectorValue();
        idetectorVariable idetectorVariable = new idetectorVariable();
        idetectorVariable.setName("Temp Variable");
        idetectorVariable.setValue(idetectorValue);
        return idetectorVariable;
    }

    public void union(idetectorVariable that) {
        idetectorStatus thatStatus = that.getValue().getStatus();

        value.getStatus().getTypes().addAll(thatStatus.getTypes());
        value.getStatus().getTypes().remove(null);

        if (!value.isPolluted() && thatStatus.isPolluted()) {
            value.setPolluted(true);
        }
    }


    public void assign(idetectorVariable var, boolean remain) {
        // copy value
        if (var != null && var.getValue() != null) {
            if (isPolluted(-1) && remain) {
                return;
            }
            value = var.getValue();
            elements = var.getElements();
            fieldMap = var.getFieldMap();
        }
    }

    /**
     * case a[index] = b
     *
     * @param index
     * @param var
     */
    public void assign(int index, idetectorVariable var) {
        idetectorVariable element = elements.get(index);
        var.owner = this;
        if (element != null) {
            element.assign(var, false);
        } else {
            boolean flag = true;
            for (idetectorVariable temp : elements.values()) {
                if (temp.getOrigin() != null && temp.getOrigin().equals(var.getOrigin())) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                addElement(index, var);
            }
        }
    }

    /**
     * case a.f = b
     *
     * @param sfr
     * @param var
     */
    public void assign(String sfr, idetectorVariable var) {
        idetectorVariable fieldVar = fieldMap.get(sfr);
        var.owner = this;
        if (fieldVar != null) {
            fieldVar.assign(var, false);
        } else {
            fieldMap.put(sfr, var);
        }
    }

    public boolean containsPollutedVar(List<idetectorVariable> queue) {
        if (queue.contains(this)) {
            return value.isPolluted();
        } else {
            queue.add(this);
        }
        if (value.isPolluted()) {
            firstPollutedVarRelatedType = value.getRelatedType();
            return true;
        } else if (elements != null && elements.size() > 0) {
            for (idetectorVariable element : elements.values()) {
                if (element.containsPollutedVar(queue)) {
                    firstPollutedVarRelatedType = element.getFirstPollutedVarRelatedType();
                    return true;
                }
            }
        } else if (fieldMap != null && fieldMap.size() > 0) {
            for (idetectorVariable field : fieldMap.values()) {
                if (field.containsPollutedVar(queue)) {
                    firstPollutedVarRelatedType = field.getFirstPollutedVarRelatedType();
                    return true;
                }
            }
        }
        return false;
    }

    public String getFirstPollutedVarRelatedType() {
        if (firstPollutedVarRelatedType == null) {
            containsPollutedVar(new ArrayList<>());
        }
        return firstPollutedVarRelatedType;
    }

    public boolean isPolluted(int index) {
        if (value.isPolluted() && index == -1) {
            return true;
        } else if (index != -1) {
            if (elements.size() > index) {
                idetectorVariable var = elements.get(index);
                return var.isPolluted(-1);
            }
        }
        return false;
    }

    public void clearVariableStatus() {
        value.setStatus(new idetectorStatus());
        fieldMap.clear();
        elements.clear();
    }

    public void clearElementStatus(int index) {
        idetectorVariable element = elements.get(index);
        if (element != null) {
            element.clearVariableStatus();
        }
    }

    public void clearFieldStatus(SootFieldRef sfr) {
        idetectorVariable field = fieldMap.get(sfr.name());
        if (field != null) {
            field.clearVariableStatus();
        }
    }

    public idetectorVariable getElement(int index) {
        return elements.getOrDefault(index, null);
    }

    public void removeElement(int index) {
        elements.remove(index);
    }

    public void addElement(int index, idetectorVariable var) {
        if (!elements.containsValue(var)) {
            elements.put(index, var);
        }
    }

    public idetectorVariable getField(String sfr) {
        return fieldMap.getOrDefault(sfr, null);
    }

    public SootField getSootField(String sfr) {

        Pattern pattern = Pattern.compile("<(.*):\\s(.*)\\s(.*)>");
        Matcher m = pattern.matcher(sfr);
        if (m.find()) {
            String classname = m.group(1);
            String fieldname = m.group(3);
            classname = classname.replace("'", "");
            fieldname = fieldname.replace("'", "");
            SootClass cls = Scene.v().getSootClass(classname);
            try {
                return cls.getFieldByName(fieldname);
            } catch (Exception e) {
                // e.printStackTrace();
                // log.warn(sfr + " field not found!");
            }
        }

        return null;
    }

    public void removeField(String sfr) {
        fieldMap.remove(sfr);
    }

    public void addField(String sfr, idetectorVariable var) {
        fieldMap.put(sfr, var);
    }

    public idetectorVariable getOrAddField(idetectorVariable baseVar, SootField sf) {
        idetectorVariable fieldVar = baseVar.getField(sf.getSignature());
        if (fieldVar == null) {
            fieldVar = makeFieldInstance(baseVar, sf);
            baseVar.assign(sf.getSignature(), fieldVar);
        }
        return fieldVar;
    }

    public idetectorVariable getOrAddField(idetectorVariable baseVar, SootFieldRef sfr) {
        idetectorVariable fieldVar = baseVar.getField(sfr.getSignature());
        if (fieldVar == null) {
            fieldVar = makeFieldInstance(baseVar, sfr);
            baseVar.assign(sfr.getSignature(), fieldVar);
        }
        return fieldVar;
    }

    public idetectorVariable deepClone(List<idetectorVariable> clonedVars) {
        idetectorVariable clonedVar = null;
        // try to find from cache
        if (clonedVars.contains(this)) {
            return this;
        } else {
            clonedVars.add(this);
        }
        // try to clone value
        clonedVar = new idetectorVariable(uuid);
        clonedVar.setName(name);
        clonedVar.setOrigin(origin);
        clonedVar.setParam(isParam);
        clonedVar.setParamIndex(paramIndex);
        clonedVar.setThis(isThis);
        clonedVar.setOwner(owner);
        clonedVar.setValue(value == null ? null : value.deepClone());

        Map<Integer, idetectorVariable> newElements = new HashMap<>();
        Map<String, idetectorVariable> newFields = new HashMap<>();


        for (Map.Entry<Integer, idetectorVariable> entry : elements.entrySet()) {
            idetectorVariable var = entry.getValue();
            newElements.put(entry.getKey(), var != null ? var.deepClone(clonedVars) : null);
        }

        for (Map.Entry<String, idetectorVariable> entry : fieldMap.entrySet()) {
            String sfr = entry.getKey();
            idetectorVariable field = entry.getValue();
            newFields.put(sfr, field != null ? field.deepClone(clonedVars) : null);
        }

        clonedVar.setElements(newElements);
        clonedVar.setFieldMap(newFields);

        return clonedVar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        idetectorVariable that = (idetectorVariable) o;

        if (elements.size() != that.elements.size()) return false;
        if (fieldMap.size() != that.fieldMap.size()) return false;

        EqualsBuilder builder = new EqualsBuilder()
                .append(isThis, that.isThis)
                .append(isParam, that.isParam)
                .append(paramIndex, that.paramIndex)
                .append(name, that.name)
                .append(origin, that.origin)
                .append(value, that.value);

        if (!builder.isEquals()) return false;

        for (Map.Entry<Integer, idetectorVariable> entry : elements.entrySet()) {
            if (!equals(entry.getValue(), that.elements.get(entry.getKey()))) {
                return false;
            }
        }

        for (Map.Entry<String, idetectorVariable> entry : fieldMap.entrySet()) {
            if (!equals(entry.getValue(), that.fieldMap.get(entry.getKey()))) {
                return false;
            }
        }

        return true;
    }

    public boolean equals(idetectorVariable var1, idetectorVariable var2) {
        if (var1 == var2) return true;

        if (var1 == null || var2 == null) return false;

        EqualsBuilder builder = new EqualsBuilder()
                .append(var1.isThis, var2.isThis)
                .append(var1.isParam, var2.isParam)
                .append(var1.paramIndex, var2.paramIndex)
                .append(var1.name, var2.name)
                .append(var1.origin, var2.origin)
                .append(var1.value, var2.value);

        return builder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder(17, 37)
                .append(name)
                .append(origin)
                .append(isThis)
                .append(isParam)
                .append(paramIndex)
                .append(value);

        for (idetectorVariable element : elements.values()) {
            builder.append(element.name)
                    .append(element.origin)
                    .append(element.isThis)
                    .append(element.isParam)
                    .append(element.paramIndex)
                    .append(element.value);
        }

        for (idetectorVariable field : fieldMap.values()) {
            builder.append(field.name)
                    .append(field.origin)
                    .append(field.isThis)
                    .append(field.isParam)
                    .append(field.paramIndex)
                    .append(field.value);
        }

        return builder.toHashCode();
    }


}
