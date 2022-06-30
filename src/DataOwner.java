import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import it.unisa.dia.gas.jpbc.Element;

public class DataOwner {
    // (mark) Is alpha a big number or a group Zr element?
    PublicParams pp;
    // private SK sk;
    SK sk;
    PK pk;
    // (mark) Save tau.n to client locally?
    int n;
    // (mark) Save timestamp t locally?
    Element t;

    public DataOwner(PublicParams _pp) throws Exception {
        pp = _pp;

        Element alpha = pp.Zr.newRandomElement().getImmutable();
        Element U = pp.g2.powZn(alpha).getImmutable();
        System.out.println(U);
        // generate keypair
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("DSA");
        kpGen.initialize(2048);
        KeyPair kp = kpGen.genKeyPair();
        PrivateKey ssk = kp.getPrivate();
        PublicKey spk = kp.getPublic();

        sk = new SK(alpha, ssk);
        pk = new PK(U, spk);
    }

    public StorageContent genTag(int num, int s) throws Exception {
        /** RUN BY CLIENT */
        // create file blocks & file name
        String fname = "src/fname.txt";
        // String rawFile =
        // "近日，上海蒙山菜场2颗白菜卖93元，不少网友直呼白菜卖“白菜价”。29日，该菜场工作人员表示，视频中未提及斤数，顾客购买的2颗白菜一共有12斤，该顾客提出退货，菜场已当场帮其解决。当问及白菜7元一斤是否合理时，工作人员表示，目前白菜的进价是4块左右，7块多的售价差不多。虽然大家印象中白菜是很便宜的东西，但现在情况紧张，没有货源的情况下，上海本地内销，从源头上进货价就上去了。";
        // rawFile = "Luther was ordained to the priesthood in 1507. He came to reject
        // several teachings and practices of the Roman Catholic Church; in particular,
        // he disputed the view on indulgences. Luther proposed an academic discussion
        // of the practice and efficacy of indulgences in his Ninety-five Theses of
        // 1517. His refusal to renounce all of his writings at the demand of Pope Leo X
        // in 1520 and the Holy Roman Emperor Charles V at the Diet of Worms in 1521
        // resulted in his excommunication by the pope and condemnation as an outlaw by
        // the Holy Roman Emperor.";
        String rawFile = new String(readFile(fname));

        n = num;
        int sectors = s;
        String m[][] = preProceess(rawFile, n, sectors);

        /*
         * for(int i = 0; i<m.length; i++){
         * System.out.println(Arrays.toString(m[i]));
         * }
         */

        Element[] sigma = new Element[n];
        Element[] u = new Element[sectors];
        // (mark) get a random element as timestamp here
        t = pp.Zr.newRandomElement().getImmutable();
        for (int j = 0; j < sectors; j++) {
            u[j] = pp.G1.newRandomElement().getImmutable();
        }
        /** Get sigma_i */
        // (mark )hash of (fname) [don't have index information in hash]
        Element h_fname = pp.H(fname.getBytes()).getImmutable();
        // Calculate sigma_i
        for (int i = 0; i < n; i++) {
            sigma[i] = this.calSigma(h_fname, m[i], sectors, u);
        }

        /** Get root of mht */
        MerkleTree mht = new MerkleTree(sigma);
        byte[] rootValue = mht.getRoot().value;

        FileTag tau = new FileTag(fname, n, sectors, u, rootValue);
        StorageContent sc = new StorageContent(m, t, tau, sigma);

        return sc;
    }

    public HashMap<Integer, Element> query(int num) {
        Random ran = new Random();
        HashMap<Integer, Element> querySet = new HashMap<Integer, Element>();
        while (querySet.size() < num) {
            // (MARK) WRONG HERE should change num to tau.n
            querySet.put(ran.nextInt(n), pp.Zr.newRandomElement().getImmutable());
        }
        return querySet;
    }

    // Stage 1/3 of Update
    public UpdateDataClient updateInitiate(FileTag tau) throws Exception {
        // 1: generate a new tag
        String m[] = { "This is a new message block" };
        int opType = 0;
        int position = 2;
        Element sigmaStar = this.calSigma(pp.H(tau.fname.getBytes()), m, tau.s, tau.u);
        UpdateDataClient updateRequest = new UpdateDataClient(opType, position, m, sigmaStar);
        return updateRequest;
    }

    // Stage 3/3 of Update
    public UpdateDataVerify updateVerify(int opType, UpdateDataCloud data) throws Exception {
        // (mark) I skip the first verification here, because it's
        // inconvinient to maintain an old verison merkle tree
        // maybe I can fufill this by creating a method in MerkleTree class
        // BUt it is trivial and I'm not quite farmiliar with advanced Java features

        // Second verification
        boolean result = true;
        byte[] root1 = data.mht.getRoot().value;
        byte[] root2 = MerkleTree.calculateRoot((NodeIntern) data.mht.getRoot()).value;
        // 注意：这里不能使用 root1.equals(root2) [因为这种写法比较的是两个指针是否指向同一个对象，而不是值比较]
        if (!Arrays.equals(root1, root2)) {
            result = false;
        }
        // (mark) firt verify signature
        // update tau
        int n = 0;
        switch (opType) {
            case 0:
                n = data.tau.n;
                break;
            case 1:
                n = data.tau.n + 1;
                break;
            case 2:
                n = data.tau.n - 1;
        }
        FileTag newTau = new FileTag(data.tau.fname, n, data.tau.s, data.tau.u, data.mht.getRoot().value);
        UpdateDataVerify verifyOutput = new UpdateDataVerify(result, newTau);
        return verifyOutput;
    }

    private String[][] preProceess(String rawFile, int n, int s) {
        // 1 Divide file into blocks
        // initialize m[n][s]
        String[][] m = new String[n][s];
        String[] tempBlock = new String[n];
        // the unit is 'byte'
        int blockLen = rawFile.length() / n;
        int remainder = rawFile.length() % n;
        int lastBlockLen = blockLen + remainder;
        int i = 0;
        for (i = 0; i < n - 1; i++) {
            tempBlock[i] = rawFile.substring(i * blockLen, (i + 1) * blockLen);
        }
        tempBlock[i] = rawFile.substring(i * blockLen);
        // 2 Divide blocks into sectors
        int sectorLen = blockLen / s;
        for (i = 0; i < n - 1; i++) {
            int j = 0;
            for (j = 0; j < s - 1; j++) {
                m[i][j] = tempBlock[i].substring(sectorLen * j, sectorLen * (j + 1));
            }
            // 2.1 Deal with the last sector in normal blocks
            m[i][j] = tempBlock[i].substring(sectorLen * j);
        }
        // 2.2 Deal with the last block
        sectorLen = lastBlockLen / s;
        int j = 0;
        for (j = 0; j < s - 1; j++) {
            m[i][j] = tempBlock[i].substring(sectorLen * j, sectorLen * (j + 1));
        }
        // 2.3 Deal with last sector in the last block
        m[i][j] = tempBlock[i].substring(sectorLen * j);
        return m;
    }

    private static byte[] readFile(String fname) throws Exception {
        File f = new File(fname);
        byte[] content = new byte[(int) f.length()];
        DataInputStream dis = new DataInputStream(new FileInputStream(f));
        dis.readFully(content);
        dis.close();
        return content;
    }

    private Element calSigma(Element h_fname, String[] m, int sectors, Element[] u) throws Exception {
        // Calculate [ H(fname)*\prod_{j=1}^s (xxxx) ] as base
        // prod is init to identity element
        Element prod = pp.G1.newOneElement();
        Element m_j;
        for (int j = 0; j < sectors; j++) {
            // (mark) I use hash here
            m_j = pp.H_0(m[j].getBytes());
            prod = prod.mul(u[j].powZn(m_j));
        }
        Element base = prod.mul(h_fname).getImmutable();
        // Calculate [ 1/(\alpha+t) ] as index
        Element index = (sk.alpha.add(t)).invert().getImmutable();
        // System.out.println(sigma[i].toString());

        // (MARK del this)temp2 =
        // temp2.mul(tau.u[theta_i].powZn(pp.H_0(m[theta_i][j].getBytes())));
        return base.powZn(index).getImmutable();
    }
}

class SK {
    Element alpha;
    PrivateKey ssk;

    public SK(Element _alpha, PrivateKey _ssk) {
        alpha = _alpha;
        ssk = _ssk;
    }
}

class PK {
    Element U;
    PublicKey spk;

    public PK(Element _U, PublicKey _spk) {
        U = _U;
        spk = _spk;
    }
}
