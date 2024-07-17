package idetector.dal.caching.bean.edge;

import lombok.Getter;
import lombok.Setter;
import idetector.dal.caching.bean.ref.MethodReference;
import idetector.dal.caching.converter.MethodRef2StringConverter;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name = "Alias")
public class Alias {

    @Id
    private String id;

    @Convert(converter = MethodRef2StringConverter.class)
    private MethodReference source;

    @Convert(converter = MethodRef2StringConverter.class)
    private MethodReference target;

    public static Alias newInstance(MethodReference source, MethodReference target){
        Alias alias = new Alias();
        alias.setId(UUID.randomUUID().toString());
        alias.setSource(source);
        alias.setTarget(target);
        return alias;
    }



}
