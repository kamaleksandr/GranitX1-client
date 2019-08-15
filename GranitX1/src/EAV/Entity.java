package EAV;

import java.util.*;

/**
 * Implementation of the Entity from EAV model.
 *
 * @author kamyshev.a
 */
public class Entity {

    public int num, type;
    public final TreeSet<Attribute> attributes;
    private final Comparator<Attribute> comparator = (Attribute o1, Attribute o2) -> {
        if (o1.type == o2.type) {
            return o1.num - o2.num;
        } else {
            return o1.type - o2.type;
        }
    };

    /**
     *
     * @param num Entity number;
     * @param type Entity type;
     */
    public Entity(int num, int type) {
        this.num = num;
        this.type = type;
        attributes = new TreeSet<>(comparator);
    }
}
