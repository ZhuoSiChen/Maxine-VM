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

/**
 * @author Ben L. Titzer
 * @author Doug Simon
 */
#ifndef __threadLocals_h__
#define __threadLocals_h__ 1

#include "os.h"
#include "word.h"

/*
 * A thread locals block is a block of memory allocated on a page boundary (e.g. by valloc(3c)).
 * It contains all the VM and native thread local data for a thread.
 * This block of memory is laid out as follows:
 *
 * (low addresses)
 *
 *           page aligned --> +---------------------------------------------+ <-- threadLocalsBlock_current()
 *                            | X X X          unmapped page          X X X |
 *                            | X X X                                 X X X |
 *           page aligned --> +---------------------------------------------+
 *                            |           thread locals (triggered)         |
 *                            +---------------------------------------------+ <-- threadLocals_current()
 *                            |           thread locals (enabled)           |
 *                            +---------------------------------------------+
 *                            |           thread locals (disabled)          |
 *                            +---------------------------------------------+  <-- nativeThreadLocals_current()
 *                            |           NativeThreadLocalsStruct          |
 *                            +---------------------------------------------+
 *                            |                                             |
 *                            |               reference map                 |
 *                            |                                             |
 *                            +---------------------------------------------+
 *
 * (high addresses)
 *
 */

#define THREAD_LOCALS_FROM_TLBLOCK(tlBlock)        ((ThreadLocals)       (tlBlock + virtualMemory_getPageSize() - sizeof(Address) +  threadLocalsAreaSize()))
#define NATIVE_THREAD_LOCALS_FROM_TLBLOCK(tlBlock) ((NativeThreadLocals) (tlBlock + virtualMemory_getPageSize() - sizeof(Address) + (threadLocalsAreaSize() * 3)))

extern void threadLocals_initialize(int threadLocalsSize);

/**
 * Creates and initializes the thread locals block (see diagram above) for the current thread.
 * This includes protecting certain pages of the stack for stack overflow detection.
 * To clean up these resources, the threadLocalsBlock_destroy() function should be
 * called on the value returned by this function.
 *
 * @param id  > 0: the identifier reserved in the thread map for the thread being started
 *           == 0: the primordial thread
 *            < 0: temporary identifier (derived from the native thread handle) of a thread
 *                 that is being attached to the VM
 * @return the thread locals block for the current thread. This value has been registered
 *         as the value associated with the ThreadLocalsKey for this thread. The destructor
 *         function specified when registering the value is threadLocalsBlock_destroy().
 */
extern Address threadLocalsBlock_create(jint id);

/**
 * Releases the resources for the current thread allocated and protected by threadLocalsBlock_create().
 * This is the function specified as the destructor for the value associated with the ThreadLocalsKey
 * for this thread
 *
 * @param tlBlock a value returned by threadLocalsBlock_create()
 */
extern void threadLocalsBlock_destroy(Address tlBlock);

/**
 * The names and indexes of the VM thread locals accessed by native code.
 *
 * These values must be kept in sync with those declared in VmThreadLocals.java.
 * The boot image includes a copy of these values that are checked at image load time.
 *
 * All reads/write to these thread locals should use the 'getThreadLocal()' and 'setThreadLocal()' macros below.
 */
#define FOR_ALL_THREAD_LOCALS(macro) \
    macro(SAFEPOINT_LATCH, 0) \
    macro(SAFEPOINTS_ENABLED_THREAD_LOCALS, 1) \
    macro(SAFEPOINTS_DISABLED_THREAD_LOCALS, 2) \
    macro(SAFEPOINTS_TRIGGERED_THREAD_LOCALS, 3) \
    macro(NATIVE_THREAD_LOCALS, 4) \
    macro(FORWARD_LINK, 5) \
    macro(BACKWARD_LINK, 6) \
    macro(ID, 9) \
    macro(JNI_ENV, 11) \
    macro(LAST_JAVA_FRAME_ANCHOR, 12) \
    macro(TRAP_NUMBER, 15) \
    macro(TRAP_INSTRUCTION_POINTER, 16) \
    macro(TRAP_FAULT_ADDRESS, 17) \
    macro(TRAP_LATCH_REGISTER, 18) \
    macro(STACK_REFERENCE_MAP, 22) \
    macro(STACK_REFERENCE_MAP_SIZE, 23)

#define DECLARE_THREAD_LOCAL(name, index) name = index,
typedef enum ThreadLocal {
    FOR_ALL_THREAD_LOCALS(DECLARE_THREAD_LOCAL)
} ThreadLocal_t;

/**
 * This typedef is only to clarify intent when using thread locals.
 */
typedef Address ThreadLocals;

/**
 * Gets the block of memory allocated for the native and VM thread locals associated with the current thread.
 *
 * This value is accessed via the native thread library's thread locals mechanism (e.g. pthread_getspecific(3c)).
 */
extern Address threadLocalsBlock_current(void);

/**
 * Sets the block of memory allocated for the native and VM thread locals associated with the current thread.
 *
 * This value is accessed via the native thread library's thread locals mechanism (e.g. pthread_setspecific(3c)).
 */
extern void threadLocalsBlock_setCurrent(Address tlBlock);

/**
 * Gets a pointer to the safepoints-enabled copy of thread locals associated with the current thread.
 */
extern ThreadLocals threadLocals_current(void);

/**
 * Gets the size of a thread locals area.
 */
extern int threadLocalsAreaSize();

/**
 * Sets the value of a specified thread local.
 *
 * @param tl a ThreadLocals value
 * @param name the name of the thread local to access (a ThreadLocal_t value)
 * @param value the value to which the named thread local should be set
 */
#define setThreadLocal(tl, name, value) do { *((Address *) tl + name) = (Address) (value); } while (0)

/**
 * Gets the value of a specified thread local.
 *
 * @param type the type to which the retrieved thread local value is cast
 * @param tl a ThreadLocals value
 * @param name the name of the thread local to access (a ThreadLocal_t value)
 * @return the value of the named thread local, cast to 'type'
 */
#define getThreadLocal(type, tl, name) ((type) *((Address *) tl + name))

/**
 * Gets the address of a specified thread local.
 *
 * @param tl a ThreadLocals value
 * @param name the name of the thread local to address
 * @return the address of the named thread local, cast to Address
 */
#define getThreadLocalAddress(tl, name) ((Address) tl + (name * sizeof(Address)))

/**
 * Sets the value of a specified thread local to all three thread local spaces.
 *
 * @param tl a ThreadLocals value
 * @param name the name of the thread local to access (a ThreadLocal_t value)
 * @param value the value to which the named thread local should be set
 */
#define setConstantThreadLocal(tl, name, value) do { \
    *((Address *) getThreadLocal(ThreadLocals, tl, SAFEPOINTS_ENABLED_THREAD_LOCALS) + name) = (Address) (value); \
    *((Address *) getThreadLocal(ThreadLocals, tl, SAFEPOINTS_DISABLED_THREAD_LOCALS) + name) = (Address) (value); \
    *((Address *) getThreadLocal(ThreadLocals, tl, SAFEPOINTS_TRIGGERED_THREAD_LOCALS) + name) = (Address) (value); \
} while (0)

typedef struct {
    Address stackBase;
    Size stackSize;
    Address handle;    // e.g. pthread_self()
    Address tlBlock;
    Address tlBlockSize;
    Address stackYellowZone; // unmapped to cause a trap on access
    Address stackRedZone;    // unmapped always - fatal exit if accessed

    /*
     * The blue zone is a page that is much closer to the base of the stack and is optionally protected.
     * This can be used, e.g., to determine the actual stack size needed by a thread, or to avoid
     * reserving actual real memory until it is needed.
     */
    Address stackBlueZone;

    /*
     * Place to hang miscellaneous OS dependent record keeping data.
     */
    void *osData;  //
} NativeThreadLocalsStruct, *NativeThreadLocals;

/**
 * Gets a pointer to NativeThreadLocalsStruct associated with the current thread.
 */
extern NativeThreadLocals nativeThreadLocals_current(void);

/**
 * Prints a selection of the fields in a given ThreadLocals object.
 *
 * @param tl the ThreadLocals to be printed
 */
extern void threadLocals_println(ThreadLocals);

/**
 * Prints the elements in a list of thread locals.
 *
 * @param tl the head of a list of thread locals
 */
extern void threadLocals_printList(ThreadLocals tl);

#endif /*__threadLocals_h__*/
