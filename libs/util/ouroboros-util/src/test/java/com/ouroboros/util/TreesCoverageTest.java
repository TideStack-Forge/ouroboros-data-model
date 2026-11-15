package com.ouroboros.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class TreesCoverageTest {

  static class Node {
    final String id;
    List<Node> children = new ArrayList<>();
    Node parent;

    Node(String id) {
      this.id = id;
    }

    static Node of(String id, Node... children) {
      var node = new Node(id);
      node.setChildren(Arrays.asList(children));
      return node;
    }

    Node copy() {
      return new Node(id);
    }

    List<Node> getChildren() {
      return children;
    }

    void setChildren(Collection<Node> nodes) {
      List<Node> copy = nodes == null ? Collections.<Node>emptyList() : new ArrayList<Node>(nodes);
      children.clear();
      if (nodes != null) {
        children.addAll(copy);
        children.forEach(child -> child.parent = this);
      }
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Node && id.equals(((Node) obj).id);
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    @Override
    public String toString() {
      return id;
    }
  }

  private static Node sampleTree() {
    var d = new Node("D");
    var e = new Node("E");
    var f = new Node("F");
    var b = Node.of("B", d, e);
    var c = Node.of("C", f);
    return Node.of("A", b, c);
  }

  private static List<Node> sampleRoots() {
    return Collections.singletonList(sampleTree());
  }

  @Test
  public void coversCoreTraversalQueryAndMetricAlgorithms() {
    var root = sampleTree();
    var b = root.children.get(0);
    var c = root.children.get(1);
    var d = b.children.get(0);
    var e = b.children.get(1);
    var f = c.children.get(0);

    var bottomUp = new ArrayList<>();
    Trees.walkTree(root, Node::getChildren, node -> bottomUp.add(node.id));
    assertEquals(Arrays.asList("D", "E", "B", "F", "C", "A"), bottomUp);

    var topDown = new ArrayList<>();
    Trees.walkTreeDown(root, Node::getChildren, node -> topDown.add(node.id));
    assertEquals(Arrays.asList("A", "B", "D", "E", "C", "F"), topDown);

    var visitorParents = new ArrayList<>();
    Trees.walkTree(root, Node::getChildren, (node, parent) -> visitorParents.add(parent == null ? "null" : parent.id));
    assertEquals(Arrays.asList("null", "null", "null", "null", "null", "null"), visitorParents);

    var downParents = new ArrayList<>();
    Trees.walkTreeDown(root, Node::getChildren, (node, parent) -> downParents.add(parent == null ? "null" : parent.id));
    assertEquals(Arrays.asList("null", "null", "null", "null", "null", "null"), downParents);

    assertEquals("A2", Trees.mapTree(root, Node::getChildren, (node, children) -> node.id + children.size()));
    assertEquals(1, Trees.mapTree(Collections.singletonList(root), Node::getChildren, (node, children) -> node.id).size());

    var mappedDown = Trees.mapTreeDown(root, Node::getChildren, Node::setChildren, (node, parent) -> node.copy());
    assertNotNull(mappedDown);
    assertEquals(2, mappedDown.children.size());

    assertEquals(Optional.of(e), Trees.findOne(root, Node::getChildren, node -> node.id.equals("E")));
    assertEquals(Arrays.asList(e, b), Trees.findAll(root, Node::getChildren, node -> node.id.equals("B") || node.id.equals("E")));
    assertEquals(Arrays.asList(root, b, e), Trees.findPath(root, e, Node::getChildren));
    assertEquals(Arrays.asList(d, b, root, c, f), Trees.findPathBetween(root, d, f, Node::getChildren));
    assertEquals(Arrays.asList(root, b, d, e, c, f), Trees.flattenTree(root, Node::getChildren));
    assertEquals(Arrays.asList(root, b, d, e, c, f), Trees.flattenTree(sampleRoots(), Node::getChildren));
    assertEquals(Arrays.asList(root, b, d, e, c, f), Trees.streamFlatter((Node node) -> node.getChildren().stream()).apply(root).collect(java.util.stream.Collectors.toList()));

    var filteredRoots = Trees.filterTree(sampleRoots(), node -> !node.id.equals("C") && !node.id.equals("F"), Node::getChildren, Node::setChildren);
    assertEquals(1, filteredRoots.size());
    assertEquals(Collections.singletonList("B"), filteredRoots.get(0).children.stream().map(node -> node.id).collect(java.util.stream.Collectors.toList()));

    var cloned = Trees.cloneSubtree(root, Node::getChildren, Node::copy, Node::setChildren);
    assertEquals("A", cloned.id);
    assertEquals(2, cloned.children.size());
    assertFalse(cloned == root);
    assertEquals("B", Trees.cloneSubtree(sampleRoots(), node -> node.id.equals("B"), Node::getChildren, Node::copy, Node::setChildren).id);

    assertEquals(2, Trees.calculateHeight(root, Node::getChildren));
    assertEquals(2, Trees.calculateDepth(root, e, Node::getChildren));
    assertEquals(6, Trees.countNodes(root, Node::getChildren));
    assertEquals(3, Trees.countLeaves(root, Node::getChildren));
    assertTrue(Trees.isLeaf(f, Node::getChildren));
    assertFalse(Trees.isLeaf(root, Node::getChildren));
    assertTrue(Trees.detectCycle(sampleRoots(), Node::getChildren) == false);
    assertTrue(Trees.isDescendant(b, e, Node::getChildren));
    assertFalse(Trees.isDescendant(b, b, Node::getChildren));

    assertEquals(Arrays.asList(b, root), Trees.getAncestors(e, node -> node.parent));
    assertEquals(Collections.singletonList(root), Trees.getAncestors(sampleRoots(), node -> node.id.equals("B"), Node::getChildren, node -> node.parent));
    assertEquals(Arrays.asList(d, e), Trees.getDescendants(b, Node::getChildren));
    assertEquals(Arrays.asList(d, e), Trees.getDescendants(sampleRoots(), node -> node.id.equals("B"), Node::getChildren));
    assertEquals(Arrays.asList(e), Trees.getSiblings(d, node -> node.parent, Node::getChildren));
    assertEquals(Arrays.asList(e), Trees.getSiblings(sampleRoots(), node -> node.id.equals("D"), Node::getChildren, node -> node.parent));

    assertEquals(b, Trees.findLowestCommonAncestor(root, d, e, Node::getChildren).orElse(null));
    assertEquals(b, Trees.findLowestCommonAncestor(sampleRoots(), node -> node.id.equals("D"), node -> node.id.equals("E"), Node::getChildren).orElse(null));
    assertEquals(Arrays.asList(root, b, e), Trees.findPath(sampleRoots(), node -> node.id.equals("E"), Node::getChildren));
    assertEquals(Arrays.asList(d, b, root, c, f), Trees.findPathBetween(sampleRoots(), node -> node.id.equals("D"), node -> node.id.equals("F"), Node::getChildren));

    var serialized = Trees.serialize(root, Node::getChildren, node -> node.id);
    assertTrue(serialized.startsWith("A,2,"));
    assertTrue(serialized.contains("D,0,"));
    assertTrue(Trees.serialize(sampleRoots(), Node::getChildren, node -> node.id).startsWith("["));

    assertTrue(Trees.detectCycle(Collections.singletonList(root), Node::getChildren) == false);
    assertFalse(Trees.detectCycle(Collections.singletonList(aCycle()), Node::getChildren) == false);
  }

  @Test
  public void coversBuildersWalkerAndModifierOperations() {
    var flat = Arrays.asList(sampleTree(), Node.of("X"));
    var root = flat.get(0);
    var x = flat.get(1);

    Trees.builder(Node.class, Node.class);
    var builderResult = Trees.<Node, Node>builder()
        .roots(() -> Collections.singletonList(root))
        .childrenGetter(node -> node.getChildren())
        .mapper((node, children) -> {
          var copy = node.copy();
          copy.setChildren(children);
          return copy;
        })
        .build();
    assertEquals(1, builderResult.size());

    var remapped = Trees.remapBuilder(flat, Node.class)
        .rootPredicate(node -> node.parent == null)
        .childPredicate((parent, child) -> parent == child.parent)
        .mapper((node, children) -> {
          var copy = node.copy();
          copy.setChildren(children);
          return copy;
        })
        .build();
    assertEquals(2, remapped.size());

    var simple = Trees.simpleBuilder(flat)
        .rootPredicate(node -> node.parent == null)
        .childPredicate((parent, child) -> parent == child.parent)
        .childrenSetter(Node::setChildren)
        .nodeCopier(Node::copy)
        .build();
    assertEquals(2, simple.size());

    var walker = Trees.walker(Collections.singletonList(root), Node::getChildren);
    assertEquals(Arrays.asList("A", "B", "D", "E", "C", "F"), walkerDepthFirst(walker));
    assertEquals(Arrays.asList("A", "B", "C", "D", "E", "F"), walkerBreadthFirst(walker));
    var depthFirstPaths = new ArrayList<>();
    walker.depthFirst((Trees.NodeWithPathVisitor<Node>) (Node node, List<Node> path) -> depthFirstPaths.add(node.id + ":" + path.size()));
    assertTrue(depthFirstPaths.contains("A:0"));
    assertTrue(depthFirstPaths.contains("D:2"));
    var breadthParents = new ArrayList<>();
    walker.breadthFirst((Trees.NodeWithParentVisitor<Node>) (Node node, Node parent) -> breadthParents.add(node.id + ":" + (parent == null ? "null" : parent.id)));
    assertTrue(breadthParents.contains("B:A"));
    var bottomUpPaths = new ArrayList<>();
    walker.bottomUp((Trees.NodeWithPathVisitor<Node>) (Node node, List<Node> path) -> bottomUpPaths.add(node.id + ":" + path.size()));
    assertTrue(bottomUpPaths.contains("F:2"));
    assertEquals(Optional.of(root.children.get(0)), walker.findFirst((node, parent) -> parent != null && parent.id.equals("A") && node.id.equals("B")));
    assertEquals(Arrays.asList("D", "E"), walker.findAll((node, parent) -> parent != null && parent.id.equals("B")).stream().map(node -> node.id).collect(java.util.stream.Collectors.toList()));
    assertEquals(Optional.of(x), Trees.walker(Collections.singletonList(x), Node::getChildren).findFirst(node -> node.id.equals("X")));
    assertEquals(Collections.singletonList(x), Trees.walker(Collections.singletonList(x), Node::getChildren).findAll(node -> node.id.equals("X")));
    assertEquals(Optional.of(root.children.get(0)), Trees.walker(Collections.singletonList(root), Node::getChildren).findFirstParent(node -> node.id.equals("D")));

    var roots = sampleRoots();
    var modifier = Trees.modifier(roots, Node::getChildren, Node::setChildren);
    assertTrue(modifier.remove(node -> node.id.equals("E")));
    assertTrue(modifier.prune(node -> node.id.equals("C")));

    roots = sampleRoots();
    modifier = Trees.modifier(roots, Node::getChildren, Node::setChildren);
    assertTrue(modifier.move(node -> node.id.equals("F"), node -> node.id.equals("B")));

    roots = sampleRoots();
    modifier = Trees.modifier(roots, Node::getChildren, Node::setChildren);
    assertTrue(modifier.move(node -> node.id.equals("F"), node -> node.id.equals("B"), list -> 0));
  }

  @Test(expected = Trees.CircularReferenceException.class)
  public void movingToSelfIsRejected() {
    var roots = sampleRoots();
    Trees.modifier(roots, Node::getChildren, Node::setChildren).move(node -> node.id.equals("B"), node -> node.id.equals("B"));
  }

  private static List<String> walkerDepthFirst(Trees.TreeWalker<Node> walker) {
    List<String> result = new ArrayList<String>();
    walker.depthFirst(node -> result.add(node.id));
    return result;
  }

  private static List<String> walkerBreadthFirst(Trees.TreeWalker<Node> walker) {
    List<String> result = new ArrayList<String>();
    walker.breadthFirst(node -> result.add(node.id));
    return result;
  }

  private static Node aCycle() {
    var a = new Node("cycle");
    a.children.add(a);
    return a;
  }
}
