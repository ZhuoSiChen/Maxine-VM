/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.jit.sparc;

import com.sun.max.asm.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.sparc.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.sparc.*;

/**
 * Target method generated by the SPARC JIT compiler.
 *
 * @author Laurent Daynes
 */
public class SPARCJitTargetMethod extends JitTargetMethod implements SPARCTargetMethod {

    public SPARCJitTargetMethod(ClassMethodActor classMethodActor) {
        super(classMethodActor);
    }

    @Override
    public void forwardTo(TargetMethod newTargetMethod) {
        SPARCTargetMethod.Static.forwardTo(this, newTargetMethod);
    }

    @Override
    public InstructionSet instructionSet() {
        return SPARCTargetMethod.Static.instructionSet();
    }

    @Override
    public void patchCallSite(int callOffset, Word callEntryPoint) {
        SPARCTargetMethod.Static.patchCallSite(this, callOffset, callEntryPoint);
    }

    @Override
    public int registerReferenceMapSize() {
        return SPARCTargetMethod.Static.registerReferenceMapSize();
    }

    @Override
    public Pointer getFramePointer(Pointer cpuStackPointer, Pointer cpuFramePointer, Pointer osSignalIntegerRegisters) {
        return STACK_BIAS.SPARC_V9.unbias(cpuStackPointer).readWord(SPARCStackFrameLayout.offset_in_saved_window((GPR) abi().framePointer())).asPointer();
    }
}
