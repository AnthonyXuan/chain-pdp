import it.unisa.dia.gas.jpbc.Element;

public class UpdateMetaData{

}
/** 1 Use for User Intitate update scheme */
class UpdateDataClient{
    // opType = 0/1/2 
    // 0: modify 1: insert 2: delete
    int opType;
    int position;
    // (mark) block is not avaliable on blockchain,
    // because it is private data (change this in later time)
    String[] block;
    Element sigmaStar;
    public UpdateDataClient(int _opType, int _position, String[] _block, Element _sigmaStar){
        opType = _opType;
        position = _position;
        block = _block;
        sigmaStar = _sigmaStar;
    }
}

/** 2 Use for Cloud Response update scheme */
class UpdateDataCloud{
    MerkleTree mht;
    byte[] root;
    FileTag tau;
    public UpdateDataCloud(MerkleTree _mht, byte[] _root, FileTag _tau){
        mht = _mht;
        root = _root;
        tau = _tau;
    }
}

/** 3 Use for User update verify */
class UpdateDataVerify{
    boolean result;
    FileTag tau;
    public UpdateDataVerify(boolean _result, FileTag _tau){
        result = _result;
        tau = _tau;
    }
}