package idetector.core.data;

import com.google.gson.Gson;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class Chain {
    public String id;
    public List<ChainBlock> chain = new ArrayList<>();

    public Chain() {
        this.id = "chain-" + UUID.randomUUID().toString().replace("-", "");
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Chain))
            return false;
        if (obj == this)
            return true;
        if (this.chain.size() != ((Chain) obj).chain.size())
            return false;
        return chain.toString().equals(((Chain) obj).chain.toString());
    }
    public int hashCode(){
        return chain.toString().hashCode();
    }


}
