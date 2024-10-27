package idetector.dal.caching.converter;

import idetector.dal.caching.bean.ref.ClassReference;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;


@Converter
public class ClassRef2StringConverter implements AttributeConverter<ClassReference,String> {
    @Override
    public String convertToDatabaseColumn(ClassReference attribute) {
        if(attribute == null){
            return "";
        }

        return attribute.getName();
    }

    @Override
    public ClassReference convertToEntityAttribute(String dbData) {
        if(dbData == null || "".equals(dbData)){
            return null;
        }
        ClassReference classRef = new ClassReference();
        classRef.setName(dbData);
        return classRef;
    }
}
