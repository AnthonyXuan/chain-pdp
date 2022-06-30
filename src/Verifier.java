import java.util.HashMap;

import javax.swing.text.html.HTMLDocument.BlockElement;

import it.unisa.dia.gas.jpbc.Element;

public class Verifier {
    PublicParams pp;
    public Verifier(PublicParams _pp){
        pp = _pp;
    }
    public boolean verifyProof(HashMap<Integer, Element> querySet, PK pk, Proof proof, FileTag tau) throws Exception{
        /** verification stage 1 */ 
        Element firstLeftPart = pp.e(proof.R, proof.RI).getImmutable();
        Element firstRightPart = pp.e(pp.g1, pp.g2).getImmutable();
        if(firstLeftPart.equals(firstRightPart)){
            System.out.println("First equation verification passed!");
        }else{
            return false;
        }
        /** verification stage 2 */ 
        // 1 Calculation N locally by using v_i (mark)
        // Or maybe I should get the value from StorageOwner?? 

        // here equals to .newElement(1);
        Element N = pp.Zr.newOneElement();
        for (Integer theta_i : querySet.keySet()) {
            N = N.mul(querySet.get(theta_i));
        }
        N = N.getImmutable();
        // 2 Calculate the left part
        Element temp1 = pp.G1.newOneElement();
        for (Integer theta_i : querySet.keySet()) {
            temp1 = temp1.mul(pp.H(tau.fname.getBytes()).powZn(N.div(querySet.get(theta_i))));
        }
        temp1 = temp1.getImmutable();

        Element temp2 = pp.G1.newOneElement();
        for(int j = 0; j<tau.s; j++){
            temp2 = temp2.mul(tau.u[j].powZn(proof.mu[j]));
        }
        temp2 = temp2.getImmutable();

        Element secondLeftPart = pp.e(temp1.mul(temp2), pp.g2).getImmutable();
        // Calculate the righe part
        temp1 = pp.g2.powZn(proof.d).div(proof.R).getImmutable();
        temp2 = pk.U.powZn(N).getImmutable();
        //(mark) Here we have a problem,
        // miner should first verify sigma
        // then compute zita locally, then verify the following equation
        Element secondRightPart = pp.e(proof.zita, temp1.mul(temp2)).getImmutable();
        //System.out.println(secondLeftPart+"     "+secondRightPart);
        if(secondLeftPart.equals(secondRightPart)){
            System.out.println("Second equation verification passed!");
        }else{
            return false;
        }
        
        return true;
    }
}
