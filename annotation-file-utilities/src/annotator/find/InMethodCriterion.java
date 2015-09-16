package annotator.find;

import javax.lang.model.element.Modifier;

import annotations.util.JVMNames;
import annotator.Main;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;

/**
 * Represents the criterion that a program element is in a method with a
 * certain name.
 */
final class InMethodCriterion implements Criterion {

  public final String name;
  private final IsSigMethodCriterion sigMethodCriterion;

  InMethodCriterion(String name) {
    this.name = name;
    sigMethodCriterion = new IsSigMethodCriterion(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Kind getKind() {
    return Kind.IN_METHOD;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isSatisfiedBy(TreePath path, Tree leaf) {
    assert path == null || path.getLeaf() == leaf;
    return isSatisfiedBy(path);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isSatisfiedBy(TreePath path) {
    Criteria.dbug.debug("InMethodCriterion.isSatisfiedBy(%s); this=%s%n",
        Main.pathToString(path), this.toString());
    boolean staticDecl = false;
    boolean result = false;

  loop:
    do {
      Tree leaf = path.getLeaf();
      switch (leaf.getKind()) {
      case CLASS:
        JCTree.JCClassDecl clazz = (JCTree.JCClassDecl) leaf;
        result = InheritedSymbolFinder.isInheritedIn(clazz.sym, name)
            && !implementedInClass(clazz);
        break loop;
      case METHOD:
        result = sigMethodCriterion.isSatisfiedBy(path);
        break loop;
      case VARIABLE:
        ModifiersTree mods = ((VariableTree) leaf).getModifiers();
        staticDecl = mods.getFlags().contains(Modifier.STATIC);
        break;
      default:
        break;
      }
      path = path.getParentPath();
    } while (path != null);

    result |= (staticDecl ? "<clinit>()V" : "<init>()V").equals(name);

    Criteria.dbug.debug("InMethodCriterion.isSatisfiedBy => %s%n", result);
    return result;
  }

  private boolean implementedInClass(ClassTree clazz) {
    for (Tree member : clazz.getMembers()) {
      if (member.getKind() == Tree.Kind.METHOD) {
        MethodTree method = (MethodTree) member;
        if (name.equals(JVMNames.getJVMMethodName(method))) {
          return method.getBody() != null;
        }
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "in method '" + name + "'";
  }
}
