import java.security.Signature;
import java.util.Arrays;


import it.unisa.dia.gas.jpbc.Element;

public class FileTag {
    String fname;
    int n;
    int s;
    Element[] u;
    byte[] rootValue;
    Signature sig;

    public FileTag(String _fname, int _n, int _s, Element[] _u, byte[] _rootValue) {
        fname = _fname;
        n = _n;
        u = _u;
        s = _s;
        rootValue = _rootValue;
    }

    public String toString() {
        String str1 = "fname=" + fname;
        String str2 = "\nn=" + n;
        String str3 = "\nu=" + Arrays.toString(u);
        String str4 = "\ns=" + s;
        return str1 + str2 + str3 + str4;
    }
}