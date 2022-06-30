import java.util.HashMap;

import it.unisa.dia.gas.jpbc.Element;

public class StorageOwner {
    PublicParams pp;
    String[][] m;
    Element t;
    FileTag tau;
    Element[] sigma;
    MerkleTree mht;
    public StorageOwner(PublicParams _pp, StorageContent sc) throws Exception{
        pp = _pp;
        m = sc.m;
        t = sc.t;
        tau = sc.tau;
        sigma = sc.sigma;
        mht = new MerkleTree(sigma);
    }
    public Proof genProof(HashMap<Integer, Element> querySet, PK pk) throws Exception{
        //(mark) I HAVEN"T VERIFY SIGNATURE HERE
        Element r = pp.Zr.newRandomElement().getImmutable();
        Element R = pp.g2.powZn(r).getImmutable();
        Element RI = pp.g1.powZn(r.invert()).getImmutable();
        // Calculate N
        Element N = pp.Zr.newOneElement();
        for (Element v_i : querySet.values()) {
            N = N.mul(v_i);
        }
        // Done with N calculation
        N = N.getImmutable();
        // Calculate d
        Element d = r.add(t.mul(N)).getImmutable();
        // Calculate \mu_j
        Element[] mu = new Element[tau.s];
        // use temp variable sum
        Element sum;
        for(int j=0; j<tau.s; j++){
            // init sum to zero
            sum = pp.Zr.newElement(0);
            for (Integer theta_i : querySet.keySet()) {
                //(mark) use hash again here
                sum = sum.add(pp.H_0(m[theta_i][j].getBytes()).mul(N).div(querySet.get(theta_i)));
            }
            mu[j] = sum.getImmutable();
        }
        // Calculate \zita
        Element zita;
        // use temp variable prod, and init it to 1
        Element prod = pp.G1.newOneElement();
        for (Integer theta_i : querySet.keySet()) {
            prod = prod.mul(sigma[theta_i].powZn(querySet.get(theta_i).invert()));
        }
        zita = prod.getImmutable();
        
        // Create Proof obj
        Proof proof = new Proof(zita, mu, R, RI, d);
        return proof;
    }
    // Stage 2/3 of Update
    public UpdateDataCloud updateResponse(UpdateDataClient data) throws Exception{
        switch(data.opType){
            case 0:
                mht.modifyLeaf(data.position, data.sigmaStar);
                break;
            case 1:
                mht.insertLeaf(data.position, data.sigmaStar);
                break;
            case 2:
                mht.deleteLeaf(data.position);
        }
        // cal new root
        MerkleTree.calculateRoot((NodeIntern)mht.getRoot());
        UpdateDataCloud cloudData = new UpdateDataCloud(mht, mht.getRoot().value, tau);
        return cloudData;
    }
}
