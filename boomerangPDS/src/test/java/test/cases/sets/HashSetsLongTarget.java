package test.cases.sets;

import org.junit.Ignore;
import org.junit.Test;
import test.TestMethod;
import test.core.QueryMethods;
import test.core.selfrunning.AllocatedObject;

import java.util.HashSet;

@SuppressWarnings("unused")
public class HashSetsLongTarget {

    @TestMethod
    public void addAndRetrieve() {
        HashSet<Object> set = new HashSet<>();
        AllocatedObject alias = new AllocatedObject() {};
        AllocatedObject alias3 = new AllocatedObject() {};
        set.add(alias);
        set.add(alias3);
        Object alias2 = null;
        for (Object o : set) alias2 = o;
        Object ir = alias2;
        Object query2 = ir;
        QueryMethods.queryFor(query2);
    }
}
