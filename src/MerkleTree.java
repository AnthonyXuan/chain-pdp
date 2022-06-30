import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import it.unisa.dia.gas.jpbc.Element;

public class MerkleTree {

  private List<NodeLeaf> leafList;
  private Node root;

  // use this constructor to construct a new MHT
  public MerkleTree(Element[] sigma) throws Exception {
    constructTree(sigma);
  }

  // use this constructor when you already have a MHT
  public MerkleTree(NodeIntern _root, List<NodeLeaf> _leafList) {
    root = _root;
    leafList = _leafList;
  }

  void constructTree(Element[] sigma) throws Exception {
    if (sigma.length <= 1) {
      throw new IllegalArgumentException("Must be at least two signatures to construct a Merkle tree");
    }
    List<Node> parents = bottomLevel(sigma);
    while (parents.size() > 1) {
      parents = internalLevel(parents);
    }
    root = parents.get(0);
  }

  public Node getRoot() {
    return root;
  }

  /**
   * Constructs an internal level of the tree
   */
  List<Node> internalLevel(List<Node> children) throws Exception {
    List<Node> parents = new ArrayList<Node>(children.size() / 2);

    for (int i = 0; i < children.size() - 1; i += 2) {
      Node child1 = children.get(i);
      Node child2 = children.get(i + 1);

      Node parent = constructInternalNode(child1, child2);
      parents.add(parent);
    }

    if (children.size() % 2 != 0) {
      Node child = children.get(children.size() - 1);
      Node parent = constructInternalNode(child, null);
      parents.add(parent);
    }

    return parents;
  }

  /**
   * Constructs the bottom part of the tree - the leaf nodes and their
   * immediate parents. Returns a list of the parent nodes.
   */
  List<Node> bottomLevel(Element[] sigma) throws Exception {
    List<Node> parents = new ArrayList<Node>(sigma.length / 2);
    // init leaflist
    leafList = new ArrayList<NodeLeaf>(sigma.length);

    for (int i = 0; i < sigma.length - 1; i += 2) {
      Node leaf1 = constructLeafNode(sigma[i]);
      Node leaf2 = constructLeafNode(sigma[i + 1]);

      Node parent = constructInternalNode(leaf1, leaf2);
      parents.add(parent);
    }

    // if odd number of leafs, handle last entry
    if (sigma.length % 2 != 0) {
      Node leaf = constructLeafNode(sigma[sigma.length - 1]);
      Node parent = constructInternalNode(leaf, null);
      parents.add(parent);
    }

    return parents;
  }

  private Node constructInternalNode(Node child1, Node child2) throws Exception {
    NodeIntern parent = new NodeIntern();

    // let parent know its childs
    parent.left = child1;
    parent.right = child2;
    // let childs know their parent
    child1.parent = parent;

    if (child2 == null) {
      parent.value = child1.value;
    } else {
      parent.value = internalHash(child1.value, child2.value);
      child2.parent = parent;
    }

    return parent;
  }

  private Node constructLeafNode(Element sig) throws Exception {
    NodeLeaf leaf = new NodeLeaf();
    // field value
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(sig.toBytes());
    leaf.value = md.digest();
    // field sigma
    leaf.sigma = sig;
    // add leaflist <List> elements
    leafList.add(leaf);
    return leaf;
  }

  byte[] internalHash(byte[] leftChildValue, byte[] rightChildValue) throws Exception {
    // (mark) H(sig1,null) == H(null,sig1) here
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(leftChildValue);
    md.update(rightChildValue);
    return md.digest();
  }

  public void modifyLeaf(int index, Element sigmaStar) throws Exception {
    NodeLeaf leaf = this.leafList.get(index);
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    // change sigma
    leaf.sigma = sigmaStar;
    // change h(sigma)
    md.update(sigmaStar.toBytes());
    leaf.value = md.digest();
    // markthetree
    this.markTheTree(leaf);
  }

  public void insertLeaf(int index, Element sigmaStar) throws Exception {
    NodeLeaf leaf = this.leafList.get(index);
    // Create a new Leaf node and add it to <List>
    NodeLeaf insertLeaf = new NodeLeaf();
    this.leafList.add(index, insertLeaf);
    insertLeaf.sigma = sigmaStar;
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(sigmaStar.toBytes());
    insertLeaf.value = md.digest();
    // Create a new intern node
    NodeIntern newIntern = new NodeIntern();
    newIntern.parent = leaf.parent;
    if (leaf.parent.left.equals(leaf)) {
      leaf.parent.left = newIntern;
    } else {
      leaf.parent.right = newIntern;
    }
    // always insert before the original element
    newIntern.left = insertLeaf;
    newIntern.right = leaf;
    insertLeaf.parent = newIntern;
    // markthetree
    this.markTheTree(insertLeaf);
  }

  public void deleteLeaf(int index) throws Exception {
    NodeLeaf leaf = this.leafList.get(index);
    NodeIntern parent = leaf.parent;
    // remove from the <List>
    this.leafList.remove(index);
    int temp = 0;
    if (leaf.parent.left.equals(leaf)) {
      parent.left = null;
      temp = 1;
    } else {
      parent.right = null;
      temp = 2;
    }
    // find a two child grandparent
    NodeIntern newParent = parent;
    while (!newParent.have2child()) {
      // record the track here
      newParent.aux = true;
      newParent = newParent.parent;
    }
    Node startNode;
    if (temp == 1) {
      startNode = parent.right;
    } else {
      startNode = parent.left;
    }

    if (newParent.left.aux == true) {
      newParent.left = startNode;
    } else {
      newParent.right = startNode;
    }
    this.markTheTree(startNode);
    // (mark) This Shitty Code contains memory Leakage problem.
  }

  public void markTheTree(Node startNode) {
    NodeIntern n = startNode.parent;
    while (n != this.root) {
      n.avaliable = false;
      n = n.parent;
    }
  }

  // Update the whole merkle tree( only calculate critical path value )
  public static NodeIntern calculateRoot(NodeIntern p) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    if (!p.left.avaliable) {
      calculateRoot((NodeIntern) p.left);
    } else if (!p.right.avaliable) {
      calculateRoot((NodeIntern) p.right);
    }
    md.update(p.left.value);
    md.update(p.right.value);
    p.value = md.digest();
    p.avaliable = true;
    p.aux = false;// (mark) Is this accurate and neccessary here?
    return p;
  }
}

class Node {
  public byte[] value;
  public NodeIntern parent;
  // avaliable is init to be true
  boolean avaliable = true;
  // this field denote whether node value need recompute
  // aux bit use for tack
  boolean aux = false;
}

class NodeIntern extends Node {
  public Node left;
  public Node right;

  public boolean have2child() {
    if ((this.left != null) && (this.right != null)) {
      return true;
    }
    return false;
  }
}

class NodeLeaf extends Node {
  public Element sigma;
}