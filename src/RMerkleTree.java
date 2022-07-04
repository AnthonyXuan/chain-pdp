import it.unisa.dia.gas.jpbc.Element;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class RMerkleTree implements Cloneable{
    private InternNode root;

    // use this constructor to construct a new MHT
    public RMerkleTree(Element[] sigma) throws Exception{
        constructTree(sigma);
    }

    // use this constructor when you already have a MHT
    public RMerkleTree(InternNode _root) {
        root = _root;
    }

    void constructTree(Element[] sigma) throws Exception {
        if (sigma.length <= 1) {
            throw new IllegalArgumentException("Must be at least two signatures to construct a Merkle tree");
        }
        List<InternNode> parents = bottomLevel(sigma);
        while (parents.size() > 1) {
            parents = internalLevel(parents);
        }
        root = parents.get(0);
    }   
        /**
     * Constructs the bottom part of the tree - the leaf nodes and their
     * immediate parents. Returns a list of the parent nodes.
     */
    List<InternNode> bottomLevel(Element[] sigma) throws Exception {
        int len = sigma.length / 2;
        List<InternNode> parents = new ArrayList<InternNode>(len);

        for (int i = 0; i < len - 1; i += 1) {
            Node leaf1 = constructLeafNode(sigma[2*i]);
            Node leaf2 = constructLeafNode(sigma[2*i + 1]);
            InternNode parent = constructInternalNode(leaf1, leaf2);
            parents.add(parent);
        }

        // if odd number of leafs
        if (sigma.length % 2 != 0) {
            Node leaf0 = constructLeafNode(sigma[sigma.length - 3]);
            Node leaf1 = constructLeafNode(sigma[sigma.length - 2]);
            Node leaf2 = constructLeafNode(sigma[sigma.length - 1]);
            Node temp = constructInternalNode(leaf1, leaf2);
            InternNode parent = constructInternalNode(leaf0, temp);
            parents.add(parent);
        }
        else{
        // if even number of leafs
            Node leaf1 = constructLeafNode(sigma[sigma.length - 2]);
            Node leaf2 = constructLeafNode(sigma[sigma.length - 1]);
            InternNode parent = constructInternalNode(leaf1, leaf2);
            parents.add(parent);
        }

        return parents;
    }

    /**
     * Constructs an internal level of the tree
     */
    List<InternNode> internalLevel(List<InternNode> children) throws Exception {
        int len = children.size() / 2;
        List<InternNode> parents = new ArrayList<InternNode>(len);

        for (int i = 0; i < children.size() - 1; i += 2) {
            Node child1 = children.get(i);
            Node child2 = children.get(i + 1);

            InternNode parent = constructInternalNode(child1, child2);
            parents.add(parent);
        }

        // if odd number of childs
        if (children.size() % 2 != 0) {
            Node child0 = children.get(children.size() - 3);
            Node child1 = children.get(children.size() - 2);
            Node child2 = children.get(children.size() - 1);
            Node temp = constructInternalNode(child1, child2);
            InternNode parent = constructInternalNode(child0, temp);
            parents.add(parent);
        }
        else{
        // if even number of childs
            Node child1 = children.get(children.size() - 2);
            Node child2 = children.get(children.size() - 1);
            InternNode parent = constructInternalNode(child1, child2);
            parents.add(parent);
        }

        return parents;
    }

    private LeafNode constructLeafNode(Element sig) throws Exception {
        LeafNode leaf = new LeafNode();
        leaf.isLeaf = true;
        leaf.rank = 1;
        // field value
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(sig.toBytes());
        md.update(Tool.toByte(leaf.rank));
        leaf.value = md.digest();
        // field sigma
        leaf.sigma = sig;

        return leaf;
    }

    private InternNode constructInternalNode(Node child1, Node child2) throws Exception {
        InternNode parent = new InternNode();
        parent.isLeaf = false;

        // let parent know its childs
        parent.left = child1;
        parent.right = child2;
        parent.rank = child1.rank + child2.rank;
        // let childs know their parent
        child1.parent = parent;
        child2.parent = parent;

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(child1.value);
        md.update(Tool.toByte(child1.rank));
        md.update(child2.value);
        md.update(Tool.toByte(child2.rank));
        parent.value = md.digest();

        return parent;
    }

    private void recomputeLeafNode(LeafNode leaf, Element sig) throws Exception{
        leaf.isLeaf = true;
        leaf.rank = 1;
        // field value
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(sig.toBytes());
        md.update(Tool.toByte(leaf.rank));
        leaf.value = md.digest();
        // field sigma
        leaf.sigma = sig;
    }

    private void recomputeInternalNode(InternNode parent, Node child1, Node child2) throws Exception{
        parent.isLeaf = false;

        // let parent know its childs
        parent.left = child1;
        parent.right = child2;
        parent.rank = child1.rank + child2.rank;
        // let childs know their parent
        child1.parent = parent;
        child2.parent = parent;

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(child1.value);
        md.update(Tool.toByte(child1.rank));
        md.update(child2.value);
        md.update(Tool.toByte(child2.rank));
        parent.value = md.digest();
    }

    public void modifyLeaf(int index, Element sigmaStar) throws Exception {
        LeafNode leaf = searchLeaf(index);
        recomputeLeafNode(leaf, sigmaStar);
        update(leaf);
    }
    
    public void insertLeaf(int index, Element sigmaStar) throws Exception {
    // Get current position leaf node & parent infos
    LeafNode curLeaf = searchLeaf(index);
    InternNode parent = curLeaf.parent;
    int leftOrRight = 0;
    if(parent.left.equals(curLeaf)){
        leftOrRight = 0;
    }
    else{
        leftOrRight = 1;
    }
    // Create a new leaf node
    LeafNode newLeaf = constructLeafNode(sigmaStar);
    // Create a new intern node
    InternNode newIntern = constructInternalNode(curLeaf, newLeaf);
    newIntern.parent = parent;
    if(leftOrRight == 0){
        parent.left = newIntern;
    }
    else{
        parent.right = newIntern;
    }

    update(newLeaf);
    }

    public void deleteLeaf(int index) throws Exception {
    // Get current position leaf node 
    LeafNode curLeaf = searchLeaf(index);
    // Get parent,grandparent and position
    InternNode parent = curLeaf.parent;
    InternNode grandParent = curLeaf.parent.parent; 

    if(grandParent.left.equals(parent)){
        if(parent.left.equals(curLeaf)){
            //LL
            grandParent.left = parent.right;
            recomputeInternalNode(grandParent, grandParent.left, grandParent.right);
            _update(grandParent.left);
        }
        else{
            //LR
            grandParent.left = parent.left;
            recomputeInternalNode(grandParent, grandParent.left, grandParent.right);
            _update(grandParent.left);
        }
    }
    else{
        if(parent.left.equals(curLeaf)){
            //RL
            grandParent.right = parent.right;
            recomputeInternalNode(grandParent, grandParent.left, grandParent.right);
            _update(grandParent.right);
        }
        else{
            //RR
            grandParent.right = parent.left;
            recomputeInternalNode(grandParent, grandParent.left, grandParent.right);
            _update(grandParent.right);
        }
    }
    // Release parent and curLeaf
    parent = null;
    curLeaf = null;
    }

    public void update(LeafNode leaf) throws Exception{
    // update from this leaf to the root
        _update(leaf);
    }

    public void calculateRoot() throws Exception{
        _calculateRoot(this.root);
    }

    private int _calculateRoot(Node node) throws Exception{
        if(node.isLeaf){
            return 0;
        }

        InternNode temp = (InternNode)node;

        if(temp.left.isLeaf && temp.right.isLeaf){
            recomputeInternalNode(temp, temp.left, temp.right);
        }
        else{       
            _calculateRoot(temp.left);
            _calculateRoot(temp.right);
            recomputeInternalNode(temp, temp.left, temp.right);
        }
        return 0;
    }

    private InternNode _update(Node node) throws Exception {
        // update current node's parent
        InternNode parent = node.parent;
        if(parent == null){
            return (InternNode)node;
        }else{
            if(parent.left.equals(node)){
                recomputeInternalNode(parent, node, parent.right);
            }else{
                recomputeInternalNode(parent, parent.left, node);
            }
            return _update(node.parent);
        }
      }

    public LeafNode searchLeaf(int index){
        LeafNode leaf = (LeafNode)_searchNode(this.root, index + 1);
        return leaf;
    }
    private Node _searchNode(Node node, int rank_value){
        if(node.isLeaf){
            return node;
        }
        else{
            InternNode temp = (InternNode)node;
            if(rank_value <= temp.left.rank){
                
                return _searchNode(temp.left, rank_value);
            }
            else{
                return _searchNode(temp.right, rank_value-node.rank);
            }
        }
    }

    /**
     * This function is used by server to generate proof according to the challenged index. This function return a Rank-based MHT. the greatest thing is that since we are returning an R-MHT as proof, the client can run other functions defined for R-MHT on this R-MHT proof.
     * @param index
     * @return
     * @throws Exception
     */
    public RMerkleTree gen_mht_proof(int[] index) throws Exception {
        // clone a mht first
        RMerkleTree proof_mht = (RMerkleTree) this.clone();

        for (int i : index) {
            // search to get leaf[i]
            LeafNode leaf = proof_mht.searchLeaf(i);
            // iterate to get critical path
            InternNode parent = leaf.parent;
            while(parent!=proof_mht.root){
                parent.track = true;
                parent = parent.parent;
            }
            // mark the critical path
            proof_mht.root.track = true;
        }
        // make the proof tree according to several ciritical paths
        _make_proof_tree(proof_mht.root);
        return proof_mht;
    }

    private int _make_proof_tree(Node node) throws Exception{
        // recurse to clear all unimportant pointers
        if(node.isLeaf){
            return 0;
        }
        InternNode temp = (InternNode)node;
        if(temp.track){
            _make_proof_tree(temp.left);
            _make_proof_tree(temp.right);
        }else{
            // not important stems, pointers are set to null
            temp.left = null;
            temp.right = null;
        }
        return 0;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
class MerkleProof{
    Element[] sigma;

}
class Node implements Cloneable{
    public byte[] value;
    public int rank;
    public InternNode parent;
    public boolean isLeaf;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

class InternNode extends Node {
    public Node left;
    public Node right;
    // track field is used to record a critical path
    public boolean track;
}

class LeafNode extends Node {
    public Element sigma;
}
