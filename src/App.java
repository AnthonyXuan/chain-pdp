import java.util.HashMap;

import it.unisa.dia.gas.jpbc.Element;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello, World!");
        PublicParams pp = setup();
        DataOwner user = new DataOwner(pp);

        long t1 = System.currentTimeMillis();
        StorageContent sc = user.genTag(100, 1);
        long t2 = System.currentTimeMillis();
        System.out.println("ddddddddd+" + (t2 - t1));

        StorageOwner cloud = new StorageOwner(pp, sc);
        HashMap<Integer, Element> querySet = user.query(50);
        Proof proof = cloud.genProof(querySet, user.pk);
        Verifier miner = new Verifier(pp);
        boolean result = miner.verifyProof(querySet, user.pk, proof, cloud.tau);
        System.out.println(result);
        System.out.println("******* Now Updating ********");

        UpdateDataClient dataClient = user.updateInitiate(sc.tau);
        UpdateDataCloud dataCloud = cloud.updateResponse(dataClient);
        UpdateDataVerify dataVerify = user.updateVerify(0, dataCloud);
        System.out.println(dataVerify.result);
    }

    public static PublicParams setup() throws Exception {
        PublicParams pp = new PublicParams();
        return pp;
    }
}
