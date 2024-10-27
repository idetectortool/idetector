package idetector.util;


//import config.SysConfig;
import soot.SootClass;
import soot.SootMethod;
import soot.tagkit.AbstractHost;
import soot.tagkit.AnnotationTag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.tagkit.VisibilityParameterAnnotationTag;
import idetector.dal.caching.bean.ref.ClassReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AnnotationUtil {

    public static List<AnnotationTag> getClassAnnoTags(SootClass sootClass) {
        List<AnnotationTag> result = new ArrayList<>();
        try {
            VisibilityAnnotationTag vat = (VisibilityAnnotationTag) sootClass.getTag("VisibilityAnnotationTag");
            if (vat != null && vat.hasAnnotations()){
                for (AnnotationTag tag : vat.getAnnotations()){
                    result.add(tag);
                }
            }
        } catch (Exception e) {}
        return result;
    }

    public static List<AnnotationTag> getMethodAnnoTags(SootMethod sootMethod) {
        List<AnnotationTag> result = new ArrayList<>();
        try {
            VisibilityAnnotationTag vat = (VisibilityAnnotationTag) sootMethod.getTag("VisibilityAnnotationTag");
            if (vat != null && vat.hasAnnotations()){
                for (AnnotationTag tag : vat.getAnnotations()){
                    result.add(tag);
                }
            }
        } catch (Exception e) {}
        return result;
    }

    public static List<AnnotationTag> getParamAnnoTags(SootMethod sootMethod) {
        List<AnnotationTag> result = new ArrayList<>();
        try {
            VisibilityParameterAnnotationTag vpat = (VisibilityParameterAnnotationTag) sootMethod.getTag("VisibilityParameterAnnotationTag");
            List<VisibilityAnnotationTag> vats =  vpat.getVisibilityAnnotations();

            for (VisibilityAnnotationTag vat: vats) {
                if (vat != null && vat.hasAnnotations()){
                    for (AnnotationTag tag : vat.getAnnotations()){
                        result.add(tag);
                    }
                }
            }
        } catch (Exception e) {}
        return result;
    }

}
