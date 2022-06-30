import java.security.MessageDigest;
import java.security.Signature;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;

public class PublicParams {
    // (mark)
    // 1 prime order p is unkonwable?
    // 2 Is Zr private?
    Field G1,G2,GT,Zr;
    private Pairing pairing;
    Element g1,g2;
    Signature sig;
    public PublicParams() throws Exception {
        TypeACurveGenerator pg = new TypeACurveGenerator(160, 320);
        PairingParameters typeAParams = pg.generate();
        pairing = PairingFactory.getPairing(typeAParams);
        // Group G1 and G2
        G1 = pairing.getG1();
        G2 = pairing.getG1();
        // Group G_T  (G_T stands for Target Group)
        GT = pairing.getGT();
        // Group Z_r
        Zr = pairing.getZr();
        // g1 & g2
        // 设定并存储生成元。由于椭圆曲线是加法群，所以G群中任意一个元素都可以作为生成元
        g1 = G1.newRandomElement().getImmutable();
        g2 = G2.newRandomElement().getImmutable();
        // Signature scheme ,using DSA here
        sig = Signature.getInstance("SHA256withDSA");
    }
    public Element e(Element a, Element b){
        // pairing function
        return pairing.pairing(a, b);
    }
    public Element H(byte[] data) throws Exception {
        // H : {0,1}^* --> G_1
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data);
        byte[] h_data = md.digest();
        return G1.newElementFromHash(h_data, 0, h_data.length);
    }
    public Element H_0(byte[] data) throws Exception {
        // H_0 : {0,1}^* --> Zr
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data);
        byte[] h_data = md.digest();
        return Zr.newElementFromHash(h_data, 0, h_data.length);
    }
}
