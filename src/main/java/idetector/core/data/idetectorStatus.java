package idetector.core.data;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;


@Data
public class idetectorStatus {
    // polluted
    boolean isPolluted = false;
    // polluted positions like param-0,param-1,field-name1,this
    Set<String> types = new HashSet<>();

    public void setType(String type){
        types.clear();
        if(type != null){
            types.add(type);
        }
    }

    public void concatType(String type){
        Set<String> newTypes = new HashSet<>();
        for(String old:types){
            newTypes.add(String.format("%s|%s", old, type));
        }
    }


    public String getFirstPollutedType(){
        if(!isPolluted) return null;
        for(String type:types){
            if(type != null && (type.startsWith("this") || type.startsWith("param-"))){
                return type;
            }
        }
        return null;
    }

    public idetectorStatus clone(){
        idetectorStatus status = new idetectorStatus();
        status.setPolluted(isPolluted);
        status.setTypes(types);
        return status;
    }
}
