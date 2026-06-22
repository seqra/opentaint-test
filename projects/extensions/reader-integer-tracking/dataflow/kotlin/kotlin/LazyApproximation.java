package kotlin;

import org.opentaint.ir.approximation.annotation.Approximate;

@Approximate(Lazy.class)
public class LazyApproximation {
    public Object getValue() {
        Lazy self = (Lazy) (Object) this;
        return self;
    }
}
