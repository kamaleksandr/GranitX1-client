package EAV;

import EAV.AttributeValue.ValueType;

/**
 *
 * @author kamyshev.a
 */
public class AttributeDescriptor {

    public final ValueType valueType;
    public final String name;

    /*@Override
    public String toString(){
        return "value type " + (valueType.ordinal());
    }*/
    public AttributeDescriptor(String s, int vt) {
        name = s;
        if (vt == ValueType.i8.ordinal()) {
            valueType = ValueType.i8;
        } else if (vt == ValueType.i16.ordinal()) {
            valueType = ValueType.i16;
        } else if (vt == ValueType.i32.ordinal()) {
            valueType = ValueType.i32;
        } else if (vt == ValueType.i64.ordinal()) {
            valueType = ValueType.i64;
        } else if (vt == ValueType.f4.ordinal()) {
            valueType = ValueType.f4;
        } else if (vt == ValueType.f8.ordinal()) {
            valueType = ValueType.f8;
        } else if (vt == ValueType.s.ordinal()) {
            valueType = ValueType.s;
        } else if (vt == ValueType.b.ordinal()) {
            valueType = ValueType.b;
        } else {
            valueType = ValueType.i8;
        }
    }
}
