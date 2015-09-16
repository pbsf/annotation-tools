package annotator.find;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.sun.tools.javac.util.Pair;

import annotations.util.JVMNames;
import type.ArrayType;
import type.DeclaredType;
import type.Type;

public class MethodInsertion extends TypedInsertion {
  final String methodName;
  final boolean isCon;
  ReceiverInsertion receiverInsertion = null;
  Set<Insertion> declarationInsertions = new LinkedHashSet<Insertion>();
  Map<Integer, Set<Insertion>> parameterInsertions;

  /**
   * Construct a MethodInsertion.
   * 
   * @param type the return type of the method.
   * @param name the method's name.
   * @param isNullaryConstructor whether the method is a nullary constructor.
   * @param criteria where to insert the text.
   * @param innerTypeInsertions the inner types to go on this method.
   */
  protected MethodInsertion(Type type, String name,
      boolean isNullaryConstructor, Criteria criteria,
      List<Insertion> innerTypeInsertions) {
    super(type, criteria, true, innerTypeInsertions);
    methodName = name;
    isCon = isNullaryConstructor;
    parameterInsertions = new TreeMap<Integer, Set<Insertion>>();
  }

  /**
   * Construct a MethodInsertion.
   * 
   * @param type the return type of the method.
   * @param name the method's name.
   * @param criteria where to insert the text.
   * @param innerTypeInsertions the inner types to go on this method.
   */
  public MethodInsertion(Type type, String name, Criteria criteria,
      List<Insertion> innerTypeInsertions) {
    this(type, name, "<init>()V".equals(name), criteria, innerTypeInsertions);
  }

  /**
   * Construct a MethodInsertion for a constructor.
   * <p>
   * To insert the annotation and the constructor (for example,
   * {@code @Anno Type this}) the name should be set to the type to insert.
   * This can either be done before calling this constructor, or by modifying
   * the return value of {@link #getType()}.
   * 
   * @param type the type to use when inserting the constructor.
   * @param criteria where to insert the text.
   * @param innerTypeInsertions the inner types to go on this constructor.
   */
  public MethodInsertion(Type type, Criteria criteria,
      List<Insertion> innerTypeInsertions) {
    this(type, "<init>()V", true, criteria, innerTypeInsertions);
  }

  /** {@inheritDoc} */
  @Override
  protected String getText(boolean comments, boolean abbreviate) {
    StringBuilder b = new StringBuilder();
    boolean isCon = methodName.startsWith("<init>");
    if (annotationsOnly) {
      if (isCon) { return ""; }
      List<String> annotations = type.getAnnotations();
      if (annotations.isEmpty()) { return ""; }
      for (String a : annotations) {
        b.append(a).append(' ');
      }
      return new AnnotationInsertion(b.toString(), getCriteria(), false)
          .getText(comments, abbreviate);
    } else {
      boolean commentAnnotation =
          comments && getBaseType().getName().isEmpty();
      String nl = System.lineSeparator();
      String typeString = !isCon ? methodName
          : typeToString(type, false, abbreviate);
      {
        // FIXME: account for '$' in source class name
        int ix = typeString.indexOf('(');
        String s = ix < 0 ? typeString : typeString.substring(0, ix);
        ix = s.lastIndexOf('$');
        typeString = ix < 0 ? typeString : typeString.substring(ix+1);
      }

      int lp = typeString.indexOf('(');
      int rp = typeString.lastIndexOf(')');
      String nameOnly = lp < 0 ? typeString : typeString.substring(0, lp);

      if (isCon) {
        for (Insertion i : declarationInsertions) {
          b.append(nl).append(i.getText(commentAnnotation, abbreviate));
          if (abbreviate) {  // TODO: ensure no abbreviation conflicts
            packageNames.addAll(i.getPackageNames());
          }
        }
        b.append(nl).append("public ").append(typeString);
      } else {
        Type rt = parseJVMLType(typeString, rp+1).fst;
        decorateType(innerTypeInsertions, rt);
        b.append(nl).append("public ");  // FIXME: use mods from impl class
        for (Insertion i : declarationInsertions) {
          b.append(i.getText(commentAnnotation, abbreviate)).append(" ");
          if (abbreviate) {  // TODO: ensure no abbreviation conflicts
            packageNames.addAll(i.getPackageNames());
          }
        }
        b.append(typeToString(rt, false, abbreviate))
            .append(" ").append(nameOnly);
      }
      b.append("(");
      if (receiverInsertion != null && !receiverInsertion.getInserted()) {
        b.append(receiverInsertion.getText(comments, abbreviate));
      }
      if (isCon) {
        b.append(") { super(); }");
      } else {
        int i = 0;
        String paramString = typeString.substring(lp+1, rp);
        List<Type> paramTypes = paramTypes(paramString);
        Iterator<Type> iter = paramTypes.iterator();
        if (iter.hasNext()) {
          String s = typeToString(iter.next(), commentAnnotation, true);
          if (parameterInsertions.containsKey(i)) {
            Set<Insertion> pins = parameterInsertions.get(i);
            for (Insertion pin : pins) {
              b.append(pin.getText(comments, abbreviate)).append(" ");
            }
          }
          b.append(s).append(" arg").append(i++);
          while (iter.hasNext()) {
            s = typeToString(iter.next(), commentAnnotation, true);
            b.append(", ").append(s).append(" arg").append(i++);
          }
        }
        b.append(") { ");
        if (!"void".equals(typeString)) { b.append("return "); }
        b.append("super.").append(nameOnly).append("(");
        if (i > 0) {
          b.append("arg0");
          for (int j = 1; j < i; j++) { b.append(", arg").append(j); }
        }
        b.append("); }");
      }
      return b.toString();
    }
  }

  private List<Type> paramTypes(String paramString) {
    int n = paramString.length();
    int i = 0;
    List<Type> ts = new ArrayList<Type>();
    while (i < n) {
      Pair<Type, Integer> pair = parseJVMLType(paramString, i);
      ts.add(pair.fst);
      i = pair.snd;
    }
    return ts;
  }

  private Pair<Type, Integer> parseJVMLType(String typeString, int i) {
    char c = typeString.charAt(i);
    if (c == '[') {
      Pair<Type, Integer> pair = parseJVMLType(typeString, ++i);
      return Pair.of((Type) new ArrayType(pair.fst), pair.snd);
    }

    int j = (c == 'L' ? typeString.indexOf(';', i) : i) + 1;
    String s = typeString.substring(i, j);
    Type t = new DeclaredType(JVMNames.jvmlStringToJavaTypeString(s));
    return Pair.of(t, j);
  }

  protected ReceiverInsertion getReceiverInsertion() {
    return receiverInsertion;
  }

  public void addReceiverInsertion(ReceiverInsertion recv) {
    if (receiverInsertion == null) {
      receiverInsertion = recv;
    } else {
      receiverInsertion.getInnerTypeInsertions()
          .addAll(recv.getInnerTypeInsertions());
    }
  }

  protected Set<Insertion> getParameterInsertions() {
    Set<Insertion> pins = new LinkedHashSet<Insertion>();
    for (Set<Insertion> s : parameterInsertions.values()) {
      pins.addAll(s);
    }
    return pins;
  }

  public void addParameterInsertion(Insertion ins, int ix) {
    Set<Insertion> pins = parameterInsertions.get(ix);
    if (pins == null) {
      pins = new LinkedHashSet<Insertion>();
      parameterInsertions.put(ix, pins);
    }
    pins.add(ins);
  }

  protected Set<Insertion> getDeclarationInsertions() {
    return declarationInsertions;
  }

  public void addDeclarationInsertion(Insertion ins) {
    declarationInsertions.add(ins);
    ins.setInserted(true);
  }

  protected boolean isDefaultConstructorInsertion() {
    return isCon;
  }

  /** {@inheritDoc} */
  @Override
  protected boolean addLeadingSpace(boolean gotSeparateLine, int pos,
      char precedingChar) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  protected boolean addTrailingSpace(boolean gotSeparateLine) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public Kind getKind() {
    return isCon ? Kind.CONSTRUCTOR : Kind.METHOD;
  }
}
