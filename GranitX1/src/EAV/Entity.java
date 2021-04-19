package EAV;

import java.util.*;

/**
 * Implementation of the Entity from EAV model.
 *
 * @author kamyshev.a
 */
public class Entity {

    public int num, type;

    public final LinkedList<Attribute> attributes;

    /**
     *
     * @param num Entity number;
     * @param type Entity type;
     */
    public Entity(int num, int type) {
        this.num = num;
        this.type = type;
        attributes = new LinkedList<>();
    }
}
