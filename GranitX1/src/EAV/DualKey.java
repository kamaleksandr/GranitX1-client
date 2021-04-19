package EAV;

/**
 *
 * @author kamyshev.a
 */
public class DualKey implements Comparable<DualKey> {

    public int num, type;

    public DualKey(int n, int t) {
        num = n;
        type = t;
    }
    
    public DualKey set(int n, int t){
        this.num = n;
        this.type = t;
        return this;
    }

    @Override
    public int compareTo(DualKey o) {
        if (this.type == o.type) {
            return this.num - o.num;
        } else {
            return this.type - o.type;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.num;
        hash = 29 * hash + this.type;
        return hash;
    }

    @Override
    public boolean equals(Object o) {    
        if (getClass() != o.getClass()) {
            return false;
        }
        DualKey key = (DualKey)o;
        return num == key.num && type == key.type;
    }

}
