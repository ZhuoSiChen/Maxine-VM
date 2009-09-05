/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.c1x.lir;

import com.sun.c1x.ci.*;
import com.sun.c1x.util.*;


/**
 * The <code>LIRConstant</code> class definition.
 *
 * @author Marcelo Cintra
 */
public class LIRConstant extends LIROperand {

    public final CiConstant value;

    /**
     * Create a LIRConstant from a ConstType object.
     * @param value the value
     */
    public LIRConstant(CiConstant value) {
        super(value.basicType);
        this.value = value;
    }

    /**
     * Converts this constant to an int.
     *
     * @return the int value of the constant, if it is an int
     */
    public int asInt() {
        assertType(this, CiKind.Int);
        return value.asInt();
    }

    /**
     * Converts this constant to a long.
     *
     * @return the long value of the constant, if it is a long
     */
    public long asLong() {
        assertType(this, CiKind.Long);
        return value.asLong();
    }

    /**
     * Converts this constant to a Float.
     *
     * @return the float value of the constant, if it is a float
     */
    public float asFloat() {
        assertType(this, CiKind.Float);
        return value.asFloat();
    }

    /**
     * Converts this constant to a Double.
     *
     * @return the double value of the constant, if it is a double
     */
    public double asDouble() {
        assertType(this, CiKind.Double);
        return value.asDouble();
    }

    /**
     * Gets the object referred to by this constant, if it is an object reference.
     *
     * @return a reference to the object if this is a reference to an object
     */
    public Object asObject() {
        assertType(this, CiKind.Object);
        return value.asObject();
    }

    /**
     * Gets the low order 32 bits of this constant; The constant type must be long.
     *
     * @return the lower order 32 bits of this constant
     */
    public int asIntLo() {
        assertType(this, CiKind.Long);
        return (int) value.asLong();
    }

    /**
     * Gets the high order 32 bits of a long constant; The constant must be of type long.
     *
     * @return the high order 32 bits of this constant
     */
    public int asIntHi() {
        assertType(this, CiKind.Long);
        return (int) (value.asLong() >> 32);
    }

    /**
     * Asserts that a given constant c if of type t.
     *
     * @return the reference to the input constant if succeeded.
     */
    public static LIRConstant assertType(LIRConstant c, CiKind t) {
        assert c.kind == t : "constant has wrong type";
        return c;
    }

    /**
     * Converts a float constant into an int constant.
     *
     * @return the int value of the constant.
     */
    public int asIntBits() {
        if (this.kind.isFloat()) {
            return Float.floatToIntBits(this.asFloat());
        } else if (this.kind == CiKind.Int) {
            return this.asInt();
        } else {
            throw Util.shouldNotReachHere();
        }
    }

    /**
     * Get the low order 32 bits of a double constant as an int.
     *
     * @return the int value of the low order 32 bits of a double constant
     */
    public int asIntLoBits() {
        // TODO: floats, longs
        if (value.basicType.isDouble()) {
            return (int) Double.doubleToLongBits(value.asDouble());
        } else {
            return value.asInt();
        }
    }

    /**
     * Get the high order 32 bits of a double constant as an int.
     *
     * @return the int value of the high order 32 bits of a double constant
     */
    public int asIntHiBits() {
        // TODO: floats, longs
        if (value.basicType.isDouble()) {
            return (int) (Double.doubleToLongBits(value.asDouble()) >> 32);
        } else {
            return asIntHi();
        }
    }

    /**
     * Converts a double constant to a long constant.
     *
     * @return the long value of the constant, if it is a double constant.
     */
    public long asLongBits() {
        // TODO: floats, longs
      if (value.basicType.isDouble()) {
        return Double.doubleToLongBits(value.asDouble());
      } else {
        return asLong();
      }
    }

    /**
     * Checks if the float constant has the value 0.0f.
     * @return true if the constant is zero, false otherwise.
     */
    public boolean isZeroFloat() {
        return asFloat() == 0.0f;
      }

    /**
     * Checks if the float constant has the value 1.0f.
     * @return true if the constant is 1.0, false otherwise.
     */
    public boolean isOneFloat() {
        return asFloat() == 1.0f;
      }

    /**
     * Checks if the double constant has the value 0.0.
     * @return true if the constant is zero, false otherwise.
     */
    public boolean isZeroDouble() {
        return asDouble() == 0.0;
    }

    /**
     * Checks if the double constant has the value 1.0.
     * @return true if the constant is zero, false otherwise.
     */
    public boolean isOneDouble() {
        return asDouble() == 1.0;
      }

    public float asJfloat() {
        return this.asFloat();
    }

    public double asJdouble() {
        return this.asDouble();
    }

    public Object asJobject() {
        return this.asObject();
    }

    @Override
    public String toString() {
        switch (kind) {
            case Int:
                return String.format("int:%d", asInt());
            case Long:
                return String.format("lng:%d", asLong());
            case Float:
                return String.format("flt:%f", this.asFloat());
            case Double:
                return String.format("dbl:%f", this.asDouble());
            case Object:
                return String.format("obj:%s", this.asObject());
            default:
                return String.format("%3d:0x%x", kind, this.asDouble());
        }
    }
}
