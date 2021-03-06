package annotations.el;

import java.util.LinkedHashMap;

import annotations.Annotation;
import annotations.util.coll.VivifyingMap;

/*>>>
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.javari.qual.*;
*/

/** An annotated class */
public final class AClass extends ADeclaration {
    /** The class's annotated type parameter bounds */
    public final VivifyingMap<BoundLocation, ATypeElement> bounds =
            ATypeElement.<BoundLocation>newVivifyingLHMap_ATE();

    public final VivifyingMap<TypeIndexLocation, ATypeElement> extendsImplements =
        ATypeElement.<TypeIndexLocation>newVivifyingLHMap_ATE();

    private static VivifyingMap<String, AMethod> createMethodMap() {
        return new VivifyingMap<String, AMethod>(
                new LinkedHashMap<String, AMethod>()) {
            @Override
            public  AMethod createValueFor(String k) {
                return new AMethod(k);
            }

            @Override
            public boolean subPrune(AMethod v) {
                return v.prune();
            }
        };
    }

    private static VivifyingMap<Integer, ABlock> createInitBlockMap() {
        return new VivifyingMap<Integer, ABlock>(
                new LinkedHashMap<Integer, ABlock>()) {
            @Override
            public  ABlock createValueFor(Integer k) {
                return new ABlock(k);
            }

            @Override
            public boolean subPrune(ABlock v) {
                return v.prune();
            }
        };
    }

    private static VivifyingMap<String, AExpression> createFieldInitMap() {
        return new VivifyingMap<String, AExpression>(
                new LinkedHashMap<String, AExpression>()) {
            @Override
            public  AExpression createValueFor(String k) {
                return new AExpression(k);
            }

            @Override
            public boolean subPrune(AExpression v) {
                return v.prune();
            }
        };
    }


    /**
     * The class's annotated methods; a method's key consists of its name
     * followed by its erased signature in JVML format.
     * For example, <code>foo()V</code> or
     * <code>bar(B[I[[Ljava/lang/String;)I</code>.  The annotation scene library
     * does not validate the keys, nor does it check that annotated subelements
     * of the {@link AMethod}s exist in the signature.
     */
    public final VivifyingMap<String, AMethod> methods =
        createMethodMap();

    public final VivifyingMap<Integer, ABlock> staticInits =
        createInitBlockMap();

    public final VivifyingMap<Integer, ABlock> instanceInits =
        createInitBlockMap();

    /** The class's annotated fields; map key is field name */
    public final VivifyingMap<String, AField> fields =
        AField.<String>newVivifyingLHMap_AF();

    public final VivifyingMap<String, AExpression> fieldInits =
        createFieldInitMap();

    private final String className;

    // debug fields to keep track of all classes created
    // private static List<AClass> debugAllClasses = new ArrayList<AClass>();
    // private final List<AClass> allClasses;

    AClass(String className) {
      super("class: " + className);
      this.className = className;
      // debugAllClasses.add(this);
      // allClasses = debugAllClasses;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(/*>>> @ReadOnly AClass this,*/
            /*@ReadOnly*/ Object o) {
        return o instanceof AClass
            && ((/*@ReadOnly*/ AClass) o).equalsClass(this);
    }

    final boolean equalsClass(/*>>> @ReadOnly AClass this,*/
            /*@ReadOnly*/ AClass o) {
        return super.equals(o)
            && className.equals(o.className)
            && bounds.equals(o.bounds)
            && methods.equals(o.methods)
            && fields.equals(o.fields)
            && extendsImplements.equals(o.extendsImplements);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode(/*>>> @ReadOnly AClass this*/) {
        return super.hashCode() + bounds.hashCode()
            + methods.hashCode() + fields.hashCode()
            + staticInits.hashCode() + instanceInits.hashCode()
            + extendsImplements.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean prune() {
        return super.prune() & bounds.prune()
            & methods.prune() & fields.prune()
            & staticInits.prune() & instanceInits.prune()
            & extendsImplements.prune();
    }

    @Override
    public String toString() {
        return "AClass: " + className;
    }

    public String unparse() {
        return unparse("");
    }

    public String unparse(String linePrefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(linePrefix);
        sb.append(toString());
        sb.append("\n");
        sb.append(linePrefix);
        sb.append("Annotations:\n");
        for (Annotation a : tlAnnotationsHere) {
            sb.append(linePrefix);
            sb.append("  " + a + "\n");
        }
        sb.append(linePrefix);
        sb.append("Bounds:\n");
        plume.UtilMDE.mapToString(sb, bounds, linePrefix + "  ");
        sb.append(linePrefix);
        sb.append("Extends/implements:\n");
        plume.UtilMDE.mapToString(sb, extendsImplements, linePrefix + "  ");
        sb.append(linePrefix);
        sb.append("Fields:\n");
        plume.UtilMDE.mapToString(sb, fields, linePrefix + "  ");
        sb.append(linePrefix);
        sb.append("Field Initializers:\n");
        plume.UtilMDE.mapToString(sb, fieldInits, linePrefix + "  ");
        sb.append(linePrefix);
        sb.append("Static Initializers:\n");
        plume.UtilMDE.mapToString(sb, staticInits, linePrefix + "  ");
        sb.append(linePrefix);
        sb.append("Instance Initializers:\n");
        plume.UtilMDE.mapToString(sb, instanceInits, linePrefix + "  ");
        sb.append(linePrefix);
        sb.append("AST Typecasts:\n");
        plume.UtilMDE.mapToString(sb, insertTypecasts, linePrefix + "  ");
        sb.append(linePrefix);
        sb.append("AST Annotations:\n");
        plume.UtilMDE.mapToString(sb, insertAnnotations, linePrefix + "  ");
        sb.append(linePrefix);
        sb.append("Methods:\n");
        plume.UtilMDE.mapToString(sb, methods, linePrefix + "  ");
        return sb.toString();
    }

    @Override
    public <R, T> R accept(ElementVisitor<R, T> v, T t) {
        return v.visitClass(this, t);
    }
}
