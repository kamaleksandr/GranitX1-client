package EAV;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of the Attribute from EAV model. The supported parameter types
 * are Byte, Short, Integer, Long, Float, Double, or String
 *
 * @author kamyshev.a
 */
public final class Attribute {

    public enum ValueType {
        b, i8, i16, i32, i64, f4, f8, s
    };
    public final int num, type;
    private ValueType value_type;
    private ByteBuffer value;

    /**
     * @param <T> Byte, Short, Integer, Long, Float, Double or String only.
     * @param num Attribute number
     * @param type Attribute type
     * @param val Value to initialize
     * @throws java.lang.Exception
     */
    public <T> Attribute(int num, int type, T val) throws Exception {
        this.num = num;
        this.type = type;
        setValue(val);
    }

    /**
     * @return Type of Attribute Value
     */
    public ValueType GetValueType() {
        return value_type;
    }
    
    /**
     *
     * @return Attribute Value as read only ByteBuffer
     */
    public ByteBuffer getValueAsBuffer(){ 
        return value.asReadOnlyBuffer();
    }

    /**
     * @param <T> Can be Byte, Short, Integer, Long, Float, Double or String.
     * @return Attribute Value
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        switch (value_type) {
            case b:
                return (T) (Boolean.class.cast(value.get(0) == 1));
            case i8:
                return (T) (Byte) value.get(0);
            case i16:
                return (T) (Short) value.getShort(0);
            case i32:
                return (T) (Integer) value.getInt(0);
            case i64:
                return (T) (Long) value.getLong(0);
            case f4:
                return (T) (Float) value.getFloat(0);
            case f8:
                return (T) (Double) value.getDouble(0);
            case s:
                String s = new String(value.array(), 0, value.capacity(), StandardCharsets.UTF_8);
                return (T) s;
        }
        return (T) (Integer) 0;
    }

    /**
     * @param <T> Must be Byte, Short, Integer, Long, Float, Double, or String.
     * @param val Value to set.
     * @throws java.lang.Exception
     */
    public <T> void setValue(T val) throws Exception {
        if (val instanceof String) {
            value_type = ValueType.s;
            byte[] buf = ((String) val).getBytes(StandardCharsets.UTF_8);
            value = ByteBuffer.allocate(buf.length);
            value.put(ByteBuffer.wrap(buf));
            return;
        }
        value = ByteBuffer.allocate(8);
        if (val instanceof Boolean) {
            value_type = ValueType.b;
            value.put(0, ((Boolean) val ? (byte) 1 : (byte) 0));
        } else if (val instanceof Byte) {
            value_type = ValueType.i8;
            value.put(0, (Byte) val);
        } else if (val instanceof Short) {
            value_type = ValueType.i16;
            value.putShort(0, (Short) val);
        } else if (val instanceof Integer) {
            value_type = ValueType.i32;
            value.putInt(0, (Integer) val);
        } else if (val instanceof Long) {
            value_type = ValueType.i64;
            value.putInt(0, (Integer) val);
        } else if (val instanceof Float) {
            value_type = ValueType.f4;
            value.putFloat(0, (Float) val);
        } else if (val instanceof Float) {
            value_type = ValueType.f8;
            value.putDouble(0, (Double) val);
        } else {
            value_type = ValueType.i32;
            value.putInt(0, (Integer) 0);
        }
    }
}
