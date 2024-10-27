package idetector.core.switcher.stmt;

import lombok.Getter;
import lombok.Setter;
import soot.jimple.AbstractStmtSwitch;
import idetector.core.container.ChainContainer;
import idetector.dal.caching.bean.ref.MethodReference;
import idetector.core.data.Context;
import idetector.core.container.DataContainer;
import idetector.core.switcher.value.ValueSwitcher;


@Getter
@Setter
public abstract class StmtSwitcher extends AbstractStmtSwitch {

    public Context context;
    public DataContainer dataContainer;
    public ChainContainer chainContainer;
    public MethodReference methodRef;
    public ValueSwitcher leftValueSwitcher;
    public ValueSwitcher rightValueSwitcher;
    public boolean reset = true;
}
