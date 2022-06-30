import it.unisa.dia.gas.jpbc.Element;
import java.util.Arrays;

public class StorageContent {
    String[][] m;
    Element t;
    FileTag tau;
    Element[] sigma;
    public StorageContent(String[][] _m, Element _t, FileTag _tau, Element[] _sigma){
        m = _m;
        t = _t;
        tau = _tau;
        sigma = _sigma;
    }
    public String toString(){
        String str1 = "";
        for(int i=0; i<m.length; i++){
            str1 += Arrays.toString(m[i]);
        }
        String str2 = "\nt="+ t;
        String str3 = "\ntau="+tau;
        String str4 = "\nsigma="+Arrays.toString(sigma);
        return str1+str2+str3+str4;
    }
}
