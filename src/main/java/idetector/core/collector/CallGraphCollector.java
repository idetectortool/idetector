package idetector.core.collector;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import soot.Modifier;
import soot.SootMethod;
import idetector.core.container.ChainContainer;
import idetector.core.container.DataContainer;
import idetector.core.data.Context;
import idetector.core.data.WorklistItem;
import idetector.core.switcher.Switcher;
import idetector.dal.caching.bean.ref.MethodReference;


@Slf4j
@Service
@Setter
public class CallGraphCollector {


    public void collect(WorklistItem worklistItem, DataContainer dataContainer, ChainContainer chainContainer) {
        try {
            MethodReference methodRef = worklistItem.getMethodReference();

            SootMethod method = methodRef.getMethod();
            if (method == null) return;

            if (method.isPhantom()
                    || methodRef.isSink()
                    || methodRef.isIgnore()
                    || method.isAbstract()
                    || Modifier.isNative(method.getModifiers())) {
                methodRef.setInitialed(true);
                methodRef.setPolluted(methodRef.isSink());
                return;
            }
            if (methodRef.getSubSignature().contains("udpateFunction")){
                System.out.println("x");
            }


            Context context = worklistItem.getContext();

            Switcher.doMethodAnalysis(context, dataContainer, chainContainer, methodRef);
            context.clear();

        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

}
