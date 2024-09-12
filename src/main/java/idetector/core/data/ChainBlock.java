package idetector.core.data;

import com.google.gson.Gson;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;


@Getter
public class ChainBlock {
    public String methodSignature;
    public List<Integer> pollutedLocation;

    public ChainBlock(String methodSignature, List<Integer> pollutedLocation) {
        this.methodSignature = methodSignature;
        this.pollutedLocation = pollutedLocation;
    }

    @Override
    public String toString(){
        return methodSignature;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ChainBlock))
            return false;
        if (obj == this)
            return true;
        return this.methodSignature.equals(((ChainBlock) obj).methodSignature);
    }

    public int hashCode(){
        return methodSignature.hashCode();
    }
}
