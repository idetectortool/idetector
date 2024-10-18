package idetector.dal.caching.converter;

import idetector.dal.caching.bean.ref.MethodReference;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;


@Converter
public class MethodRef2StringConverter implements AttributeConverter<MethodReference,String> {
    @Override
    public String convertToDatabaseColumn(MethodReference attribute) {
        if(attribute == null){
            return "";
        }

        return attribute.getId();
    }

    @Override
    public MethodReference convertToEntityAttribute(String dbData) {
        if(dbData == null || "".equals(dbData)){
            return null;
        }
        MethodReference methodRef = new MethodReference();
        methodRef.setId(dbData);
        return methodRef;
    }
}
