package idetector.dal.caching.bean.edge;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import soot.Unit;
import soot.Value;
import idetector.dal.caching.bean.ref.MethodReference;
import idetector.dal.caching.converter.ListInteger2JsonStringConverter;
import idetector.dal.caching.converter.MethodRef2StringConverter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name = "Call")
public class Call {

    @Id
    private String id;

    @Convert(converter = MethodRef2StringConverter.class)
    private MethodReference source;


    @Convert(converter = MethodRef2StringConverter.class)
    private MethodReference target;


    private int lineNum = 0;
    private String invokerType;

    private String realCallType;

    private transient Value base;
    private transient List<Value> params = new ArrayList<>();
    private transient Unit unit;


    @Column(length = 1000)
    @Convert(converter = ListInteger2JsonStringConverter.class)
    private List<Integer> pollutedPosition = new ArrayList<>();

    public static Call newInstance(MethodReference source, MethodReference target){
        Call call = new Call();
        call.setId(UUID.randomUUID().toString());
        call.setSource(source);
        call.setTarget(target);
        return call;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Call call = (Call) o;

        return new EqualsBuilder().append(source.getId(), call.source.getId()).append(target.getId(), call.target.getId()).append(invokerType, call.invokerType).append(realCallType, call.realCallType).append(pollutedPosition, call.pollutedPosition).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(source.getId()).append(target.getId()).append(invokerType).append(realCallType).append(pollutedPosition).toHashCode();
    }
}
