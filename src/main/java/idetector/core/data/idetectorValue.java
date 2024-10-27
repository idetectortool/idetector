package idetector.core.data;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import soot.ArrayType;
import soot.Local;
import soot.Type;
import soot.Value;

import java.io.Serializable;
import java.util.UUID;


@Getter
@Setter
public class idetectorValue implements Serializable {

    private UUID uuid;
    private Type type;
    private String typeName;
    private Value origin;
    private boolean isArray = false;
    private boolean isField = false;
    private boolean isFunction = false;
    private String functionName = "";
    private boolean isStatic = false;

    private idetectorStatus status = new idetectorStatus();

    public idetectorValue(){
        uuid = UUID.randomUUID();
    }

    public idetectorValue(Local value){
        uuid = UUID.randomUUID();
        type = value.getType();
        typeName = type.toString();
        origin = value;

        isArray = isArrayType(value.getType());
    }

    public idetectorValue(Type type, String relatedType){
        uuid = UUID.randomUUID();
        this.type = type;
        typeName = type.toString();

        isArray = isArrayType(type);
        status.setPolluted(true);
        status.setType(relatedType);
    }

    public static idetectorValue newInstance(Local value){
        return new idetectorValue(value);
    }

    public idetectorValue deepClone(){
        // try to clone value
        idetectorValue newValue = new idetectorValue();
        newValue.setUuid(uuid);
        newValue.setField(isField);
        newValue.setArray(isArray);
        newValue.setStatic(isStatic);
        newValue.setFunction(isFunction);
        newValue.setFunctionName(functionName);
        newValue.setType(type);
        newValue.setTypeName(typeName);
        newValue.setOrigin(origin);
        newValue.setStatus(status.clone());

        return newValue;
    }

    public static boolean isArrayType(Type type){
        if(type instanceof ArrayType){
            return true;
        }else if("java.util.List".equals(type.toString())
                && "java.util.Collection".equals(type.toString())
        ){
            return true;
        }
        return false;
    }

    public String getRelatedType(){
        return status.getFirstPollutedType();
    }

    public void setRelatedType(String type){
        status.setType(type);
    }

    public boolean isPolluted(){
        return status.isPolluted();
    }

    public void setPolluted(boolean polluted){
        status.setPolluted(polluted);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        idetectorValue that = (idetectorValue) o;

        return new EqualsBuilder()
                .append(isArray, that.isArray)
                .append(isField, that.isField)
                .append(isStatic, that.isStatic)
                .append(isFunction, that.isFunction)
                .append(functionName, that.functionName)
                .append(type, that.type)
                .append(typeName, that.typeName)
//                .append(status.isPolluted, that.status.isPolluted).isEquals();
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(type)
                .append(typeName)
                .append(isArray)
                .append(isField).append(isStatic)
                .append(isFunction)
                .append(functionName)
                .toHashCode();

    }
}
