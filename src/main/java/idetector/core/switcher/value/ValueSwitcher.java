package idetector.core.switcher.value;

import lombok.Getter;
import lombok.Setter;
import soot.Unit;
import soot.jimple.AbstractJimpleValueSwitch;
import idetector.core.container.ChainContainer;
import idetector.dal.caching.bean.ref.MethodReference;
import idetector.core.data.Context;
import idetector.core.container.DataContainer;
import idetector.core.data.idetectorVariable;


@Getter
@Setter
public abstract class ValueSwitcher extends AbstractJimpleValueSwitch {

    public boolean unbind = false;
    public boolean reset = true;

    public Context context;
    public DataContainer dataContainer;
    public ChainContainer chainContainer;
    public MethodReference methodRef;
    public idetectorVariable rvar;
    public Unit unit;

}
