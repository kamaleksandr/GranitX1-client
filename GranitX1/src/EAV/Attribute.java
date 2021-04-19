package EAV;

/**
 *
 * @author kamyshev.a
 */
public class Attribute {
    public int num, type;
    public AttributeValue value;
    
    public Attribute(int n, int t, AttributeValue v){
        num = n;
        type = t;
        value = v;
    }
}
