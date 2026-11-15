package com.ouroboros.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

public class TreesMoreCoverageTest {

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

    List<Node> getChildren() {
      return children;
    }

    void setChildren(Collection<Node> nodes) {
      children.clear();
      if (nodes != null) {
        children.addAll(new ArrayList<>(nodes));
        children.forEach(child -> child.parent = this);
      }
    }

    Node copy() {
      return new Node(id);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Node && id.equals(((Node) obj).id);
    }
  }

  @Test
  public void coversWalkerTraversalShapes() {
    var root = sampleTree();

    var topDown = new ArrayList<>();
    Trees.walker(Collections.singletonList(root), Node::getChildren).topDown(node -> topDown.add(node.id));
    assertTrue(topDown.contains("A"));

    var topDownParents = new ArrayList<>();
    Trees.walker(Collections.singletonList(root), Node::getChildren).topDown((Trees.NodeWithParentVisitor<Node>) (Node node, Node parent) -> topDownParents.add(parent == null ? "null" : parent.id));
    assertTrue(topDownParents.contains("null"));

    var topDownPaths = new ArrayList<>();
    Trees.walker(Collections.singletonList(root), Node::getChildren).topDown((Trees.NodeWithPathVisitor<Node>) (Node node, List<Node> path) -> topDownPaths.add(path.toString()));
    assertTrue(topDownPaths.contains("[]"));

    var bottomUp = new ArrayList<>();
    Trees.walker(Collections.singletonList(root), Node::getChildren).bottomUp(node -> bottomUp.add(node.id));
    assertTrue(bottomUp.contains("A"));

    var bottomUpParents = new ArrayList<>();
    Trees.walker(Collections.singletonList(root), Node::getChildren).bottomUp((Trees.NodeWithParentVisitor<Node>) (Node node, Node parent) -> bottomUpParents.add(parent == null ? "null" : parent.id));
    assertTrue(bottomUpParents.contains("null"));

    var bottomUpPaths = new ArrayList<>();
    Trees.walker(Collections.singletonList(root), Node::getChildren).bottomUp((Trees.NodeWithPathVisitor<Node>) (Node node, List<Node> path) -> bottomUpPaths.add(path.toString()));
    assertTrue(bottomUpPaths.contains("[]"));

    assertEquals(Optional.of(root.children.get(0)), Trees.walker(Collections.singletonList(root), Node::getChildren).findFirstParent(node -> node.id.equals("D")));
    assertTrue(Trees.walker(Collections.singletonList(root), Node::getChildren).findFirst((node, parent) -> parent != null && parent.id.equals("B")).isPresent());
    assertEquals(2, Trees.walker(Collections.singletonList(root), Node::getChildren).findAll((node, parent) -> parent != null && parent.id.equals("A")).size());
  }

  @Test
  public void coversEdgeCasesCloneAndModifierBranches() {
    assertEquals(Collections.emptyList(), Trees.findPath((Node) null, new Node("X"), Node::getChildren));
    assertEquals(Collections.emptyList(), Trees.findPathBetween((Node) null, new Node("X"), new Node("Y"), Node::getChildren));
    assertEquals(Optional.empty(), Trees.findLowestCommonAncestor((Node) null, new Node("X"), new Node("Y"), Node::getChildren));
    assertEquals(Collections.emptyList(), Trees.getSiblings((Node) null, node -> node.parent, Node::getChildren));
    assertEquals(Collections.emptyList(), Trees.findPath(Collections.<Node>emptyList(), node -> true, Node::getChildren));
    assertEquals(Collections.emptyList(), Trees.findPathBetween(Collections.<Node>emptyList(), node -> true, node -> true, Node::getChildren));
    assertEquals("#", Trees.<Node>serialize((Node) null, Node::getChildren, node -> node.id));
    assertEquals("", Trees.serialize(Collections.<Node>emptyList(), Node::getChildren, node -> node.id));
    assertEquals(-1, Trees.<Node>calculateHeight((Node) null, Node::getChildren));

    var root = sampleTree();
    assertEquals(root.id, Trees.cloneSubtree(root, Node::getChildren, Node::copy, Node::setChildren).id);
    assertEquals("B", Trees.cloneSubtree(root.children.get(0), Node::getChildren, Node::copy, Node::setChildren).id);

    var roots = Collections.singletonList(sampleTree());
    var modifier = Trees.modifier(roots, Node::getChildren, Node::setChildren);
    assertTrue(modifier.remove(node -> node.id.equals("E")));
    assertTrue(modifier.prune(node -> node.id.equals("C")));
    roots = Collections.singletonList(sampleTree());
    modifier = Trees.modifier(roots, Node::getChildren, Node::setChildren);
    assertTrue(modifier.move(node -> node.id.equals("F"), node -> node.id.equals("B")));
    roots = Collections.singletonList(sampleTree());
    modifier = Trees.modifier(roots, Node::getChildren, Node::setChildren);
    assertTrue(modifier.move(node -> node.id.equals("F"), node -> node.id.equals("B"), list -> 0));
  }

  private static Node sampleTree() {
    var d = new Node("D");
    var e = new Node("E");
    var f = new Node("F");
    var b = Node.of("B", d, e);
    var c = Node.of("C", f);
    return Node.of("A", b, c);
  }
}
