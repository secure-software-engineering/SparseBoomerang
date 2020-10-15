import java.util.LinkedList;
import java.util.List;

class Foo {
	void bar() {
		System.out.println("zomg bar");
	}

	public void baz() {
		System.out.println("zomg baz");
	}
}

public class Test {
	public static List<Foo> foos() {
		Foo foo = new Foo();
		foo.baz();
		System.out.println(foo);
		List<Foo> x = new LinkedList<>();
		x.add(foo);
		foo.bar();
		return x;
	}

	public static void main(String[] args) {
		System.out.println(foos());
	}
}