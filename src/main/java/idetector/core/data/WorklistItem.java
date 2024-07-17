package idetector.core.data;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import idetector.core.container.DataContainer;
import idetector.dal.caching.bean.ref.MethodReference;

import java.util.ArrayList;
import java.util.List;

@Data
public class WorklistItem {
    private MethodReference methodReference;
    private Context context;

    public WorklistItem(MethodReference methodReference) {
        this.context = Context.newInstance(methodReference);
        this.methodReference = methodReference;
    }

    @Override
    public int hashCode() { return this.methodReference.getSignature().hashCode();}
}
