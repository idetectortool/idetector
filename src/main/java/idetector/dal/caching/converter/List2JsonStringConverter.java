package idetector.dal.caching.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


@Converter
public class List2JsonStringConverter implements AttributeConverter<List<String>,String> {

    private static Gson gson = new Gson();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if(attribute == null){
            return "";
        }

        return gson.toJson(attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if(dbData == null || "".equals(dbData)){
            return new ArrayList<>();
        }
        Type objectType = new TypeToken<List<String>>(){}.getType();
        return gson.fromJson(dbData, objectType);
    }
}
