import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.unisa.dia.gas.jpbc.Element;

public class Tool {
    public static void main(String[] args) throws Exception {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        Test.startTest(600, 1, 100, 600, 30);
        Test.showPlot();
    }
    
    public static byte[] toByte(int n) {  
        byte[] b = new byte[4];  
        b[0] = (byte) (n & 0xff);  
        b[1] = (byte) (n >> 8 & 0xff);  
        b[2] = (byte) (n >> 16 & 0xff);  
        b[3] = (byte) (n >> 24 & 0xff);  
        return b;  
    }
}

class TestResult {
    public List<Integer> blocks;
    public List<Integer> genproofMill;
    public List<Integer> verifyMill;

    public TestResult(List<Integer> _blocks, List<Integer> _proof, List<Integer> _verify) {
        blocks = _blocks;
        genproofMill = _proof;
        verifyMill = _verify;
    }

    public TestResult() {
        blocks = new ArrayList<Integer>();
        genproofMill = new ArrayList<Integer>();
        verifyMill = new ArrayList<Integer>();
    }

    public void add(int _block, int _proof, int _verify) {
        this.blocks.add(_block);
        this.genproofMill.add(_proof);
        this.verifyMill.add(_verify);
    }
}

class Test {
    public static void startTest(int num, int s, int start, int stop, int step) throws Exception {
        // init system, init client
        PublicParams pp = App.setup();
        DataOwner user = new DataOwner(pp);
        // gentag
        StorageContent sc = user.genTag(num, s);
        // init CSP
        StorageOwner cloud = new StorageOwner(pp, sc);
        // init miner
        Verifier miner = new Verifier(pp);
        long t1, t2;
        int slot1, slot2;
        TestResult tResult = new TestResult();

        for (int n = start; n < stop; n += step) {
            // challenge
            HashMap<Integer, Element> querySet = user.query(n);

            t1 = System.currentTimeMillis();
            // prove
            Proof proof = cloud.genProof(querySet, user.pk);
            t2 = System.currentTimeMillis();
            slot1 = (int) (t2 - t1);

            t1 = System.currentTimeMillis();
            // verify
            miner.verifyProof(querySet, user.pk, proof, cloud.tau);
            t2 = System.currentTimeMillis();
            slot2 = (int) (t2 - t1);

            tResult.add(n, slot1, slot2);
        }
        ObjectMapper om = new ObjectMapper();
        om.writeValue(new File("../pdp-plot/testResultfile-myscheme.json"), tResult);
    }

    public static int showPlot() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder("python","../pdp-plot/plot.py");
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        return exitCode;
    }
}
