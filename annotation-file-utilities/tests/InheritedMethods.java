package annotator.tests;

abstract class Base {
  public abstract int f();
  public int[] g(String[] a, int i) { return new int[0]; }
}

public class InheritedMethods extends Base {
  public int f() { return 0; }
}

