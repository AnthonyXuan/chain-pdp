import java.util.HashMap;

import it.unisa.dia.gas.jpbc.Element;

public class Develop {
    PublicParams pp;
    String[][] m;
    Element[] sigma;
    Element t;
    FileTag tau;
    Proof proof;
    PK pk;
    SK sk;
    HashMap<Integer, Element> querySet;
    private Element eeq1;
    private Element eeq2;
    private Element eeq3;
    private Element eeq4;

    public Develop(PublicParams _pp, HashMap<Integer, Element> _querySet, StorageContent sc, PK _pk, SK _sk, Proof _proof){
        pp = _pp;
        m = sc.m;
        sigma = sc.sigma;
        t = sc.t;
        tau = sc.tau;
        proof = _proof;
        querySet = _querySet;
        pk = _pk;
        sk = _sk;
    }
    public boolean test1(){
        Element g1 = pp.g1;
        Element g2 = pp.g2;
        Element N = calcN();
        Element eq1 = pp.e(proof.zita, (g2.powZn(proof.d).div(proof.R)).mul(pk.U.powZn(N)));
        eeq1 = eq1;

        Element temp_zita = pp.G1.newOneElement();
        for (Integer theta_i : querySet.keySet()) {
            temp_zita = temp_zita.mul(sigma[theta_i].powZn(querySet.get(theta_i).invert()));
        }

        Element eq2 = pp.e(temp_zita, (g2.powZn(t.mul(N)).mul(g2.powZn(sk.alpha.mul(N)))));
        eeq2 = eq2;

        return eq1.equals(eq2);
    }
    public boolean test2() throws Exception{
        Element g1 = pp.g1;
        Element g2 = pp.g2;
        Element N = calcN();
        Element eq2 = eeq2;
        
        Element h_name = pp.H(tau.fname.getBytes()).getImmutable();
        Element temp1,temp2;
        temp1 = pp.G1.newOneElement();
        for (Integer theta_i : querySet.keySet()) {
            temp2 = pp.G1.newOneElement();
            for (int j = 0; j < tau.s; j++) {
                temp2 = temp2.mul(tau.u[j].powZn(pp.H_0(m[theta_i][j].getBytes())));
            }
            temp2 = temp2.getImmutable();
            temp1 = temp1.mul((h_name.mul(temp2)).powZn(querySet.get(theta_i).invert()));
        }
        Element eq3 = pp.e(temp1, g2.powZn(N)).getImmutable();
        eeq3 = eq3;

        Element eq3b = pp.e(proof.zita.powZn(sk.alpha.add(t)),g2.powZn(N)).getImmutable();

        Element temp3 = pp.G1.newOneElement();
        for (Integer theta_i : querySet.keySet()) {
            temp3 = temp3.mul(sigma[theta_i].powZn(N.div(querySet.get(theta_i)).mul(sk.alpha.add(t))));
        }
        Element eq3c = pp.e(temp3,g2).getImmutable();

        //Element eq3c = pp.e();
        return eq3.equals(eq2);
    }
    public boolean test3() throws Exception{
        Element g1 = pp.g1;
        Element g2 = pp.g2;
        Element N = calcN();
        Element eq3 = eeq3;
        
        Element h_name = pp.H(tau.fname.getBytes()).getImmutable();
        Element temp1,temp2;
        temp1 = pp.G1.newOneElement();
        for (Integer theta_i : querySet.keySet()) {
            temp1 = temp1.mul(h_name.powZn(N.div(querySet.get(theta_i))));
        }
        temp1 = temp1.getImmutable();

        temp2 = pp.G1.newOneElement();
        for (int j = 0; j < tau.s; j++) {
            temp2 = temp2.mul(tau.u[j].powZn(proof.mu[j]));
        }
        temp2 = temp2.getImmutable();
        Element eq4 = pp.e(temp1.mul(temp2), g2).getImmutable();
        eeq4 = eq4;

        return eq3.equals(eq4);
    }
    public boolean testSigma() throws Exception{
        Element aggreSig1,aggreSig2;
        aggreSig1 = pp.G1.newOneElement();
        for (Integer theta_i : querySet.keySet()) {
            aggreSig1 = aggreSig1.mul(sigma[theta_i]);
        }
        aggreSig1 = aggreSig1.getImmutable();
        
        Element h_name = pp.H(tau.fname.getBytes()).getImmutable();
        Element temp1,temp2;
        temp1 = pp.G1.newOneElement();
        for (Integer theta_i : querySet.keySet()) {
            temp2 = pp.G1.newOneElement();
            for (int j = 0; j < tau.s; j++) {
                temp2 = temp2.mul(tau.u[j].powZn(pp.H_0(m[theta_i][j].getBytes())));
            }
            temp2 = temp2.getImmutable();
            temp1 = temp1.mul((h_name.mul(temp2)).powZn((sk.alpha.add(t)).invert()));
        }
        aggreSig2 = temp1.getImmutable();

        return aggreSig1.equals(aggreSig2);
    }
    public Element calcN(){
        Element N = pp.Zr.newOneElement();
        for (Integer theta_i : querySet.keySet()) {
            N = N.mul(querySet.get(theta_i));
        }
        return N.getImmutable();
    }
}
