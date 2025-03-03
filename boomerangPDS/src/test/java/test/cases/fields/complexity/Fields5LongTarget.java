package test.cases.fields.complexity;

import org.junit.Test;
import test.TestMethod;
import test.core.QueryMethods;
import test.core.selfrunning.AllocatedObject;

public class Fields5LongTarget {

    private boolean staticallyUnknown() {
        return Math.random() > 0.5;
    }

    @TestMethod
    public void test() {
        TreeNode x = new TreeNode();
        TreeNode p = null;
        while (staticallyUnknown()) {
            if (staticallyUnknown()) {
                x.a = p;
            }
            if (staticallyUnknown()) {
                x.b = p;
            }
            if (staticallyUnknown()) {
                x.c = p;
            }
            if (staticallyUnknown()) {
                x.d = p;
            }
            if (staticallyUnknown()) {
                x.e = p;
            }
            p = x;
        }
        TreeNode t = null;
        if (staticallyUnknown()) {
            t = x.a;
        }
        if (staticallyUnknown()) {
            t = x.b;
        }
        if (staticallyUnknown()) {
            t = x.c;
        }
        if (staticallyUnknown()) {
            t = x.d;
        }
        if (staticallyUnknown()) {
            t = x.e;
        }
        TreeNode h = t;
        QueryMethods.queryFor(h);
    }

    private static class TreeNode implements AllocatedObject {
        TreeNode a = new TreeNode();
        TreeNode b = new TreeNode();
        TreeNode c = new TreeNode();
        TreeNode d = new TreeNode();
        TreeNode e = new TreeNode();
    }
}
