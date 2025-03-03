package test.cases.lists;

import org.junit.Test;
import test.TestMethod;
import test.cases.fields.Alloc;
import test.core.QueryMethods;
import test.core.selfrunning.AllocatedObject;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ArrayListsLongTarget {

    @TestMethod
    public void addAndRetrieveWithIterator() {
        List<Object> set = new ArrayList<>();
        AllocatedObject alias = new AllocatedObject() {};
        set.add(alias);
        Object alias2 = null;
        for (Object o : set) alias2 = o;
        Object ir = alias2;
        Object query2 = ir;
        QueryMethods.queryFor(query2);
    }

    @TestMethod
    public void addAndRetrieveByIndex1() {
        List<Object> list = new ArrayList<>();
        Alloc alias = new Alloc();
        list.add(alias);
        Object ir = list.get(0);
        Object query2 = ir;
        QueryMethods.queryFor(query2);
    }

    @TestMethod
    public void addAndRetrieveByIndex2() {
        List<Object> list = new ArrayList<>();
        AllocatedObject alias = new AllocatedObject() {};
        list.add(alias);
        Object ir = list.get(1);
        Object query2 = ir;
        QueryMethods.queryFor(query2);
    }

    @TestMethod
    public void addAndRetrieveByIndex3() {
        ArrayList<Object> list = new ArrayList<>();
        Object b = new Object();
        Object a = new Alloc();
        list.add(a);
        list.add(b);
        Object c = list.get(0);
        QueryMethods.queryFor(c);
    }
}
