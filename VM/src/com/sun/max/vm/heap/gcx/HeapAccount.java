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
package com.sun.max.vm.heap.gcx;

import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;
import static com.sun.max.vm.heap.gcx.HeapRegionManager.*;

import com.sun.max.annotate.*;

/**
 * Backing storage for a heap is managed via a heap account created on demand by the {@link HeapRegionManager}.
 * A heap account provides a guaranteed reserve of space, corresponding to the maximum space required
 * by the account owner. Space is expressed in terms of number of heap regions, whose size is defined
 * in {@link HeapRegionConstants}.
 * The account owner can allocate regions on demand up to the account's reserve.
 *
 *
 * @author Laurent Daynes
 */
public class HeapAccount<T extends HeapAccountOwner>{
    /**
     * Owner of the account.
     */
    private final T owner;
    /**
     * Guaranteed reserve of regions for this account.
     */
    private int reserve;

    /**
     * List of regions allocated to the account owner. All allocated regions are committed
     */
    @CONSTANT_WHEN_NOT_ZERO
    private HeapRegionList allocated;

    public HeapAccount(T owner) {
        this.owner = owner;
    }

    /**
     * Open the account with the specified amount of space.
     * @param spaceReserve
     * @return true if the account is opened with the guaranteed reserve of space rounded up to an integral number of regions.
     */
    public boolean open(int numRegions) {
        if (reserve > 0) {
            // Can't open an account twice.
            return false;
        }
        if (theHeapRegionManager.reserve(numRegions)) {
            reserve = numRegions;
            allocated = HeapRegionList.RegionListUse.ACCOUNTING.createList();
            return true;
        }
        return false;
    }

    public void close() {
        if (reserve > 0) {
            // FIXME:
            // HERE NEED SOME GUARANTEE THAT THE ACCOUNT HOLDS NO LIVE OBJECTS.
            // Free all the regions. Should we uncommit them too ?
            theHeapRegionManager.release(reserve);
            reserve = 0;
        }
    }

    /**
     * Number of regions in the reserve.
     * @return a number of regions.
     */
    public int reserve() {
        return reserve;
    }

    /**
     * The owner of the heap account.
     * @return an object
     */
    public T owner() { return owner; }

    /**
     * Allocate region, commit their backing storage.
     * @return
     */
    public synchronized int allocate() {
        if (allocated.size() < reserve) {
            int regionID = theHeapRegionManager.regionAllocator().allocate();
            if (regionID != INVALID_REGION_ID) {
                RegionTable.theRegionTable().regionInfo(regionID).setOwner(owner);
                allocated.prepend(regionID);
            }
        }
        return INVALID_REGION_ID;
    }

    private void recordAllocated(int regionID, int numRegions, HeapRegionList recipient, boolean prepend) {
        final int lastRegionID = regionID + numRegions - 1;

        // Record the allocated regions for accounting, initialize their region information,
        // and add them to their recipient in the desired order.
        if (prepend) {
            int r = lastRegionID;
            HeapRegionInfo regionInfo = RegionTable.theRegionTable().regionInfo(r);
            while (r >= regionID) {
                regionInfo.setOwner(owner);
                allocated.prepend(r);
                recipient.prepend(r);
                regionInfo = regionInfo.prev();
            }
        } else {
            int r = regionID;
            HeapRegionInfo regionInfo = RegionTable.theRegionTable().regionInfo(r);
            while (r <= lastRegionID) {
                regionInfo.setOwner(owner);
                allocated.append(r);
                recipient.append(r);
                regionInfo = regionInfo.next();
            }
        }
    }

    /**
     * Allocate regions in a minimum number of discontinuous range and add them in the specified region list.
     *
     * @param numRegions number of contiguous regions requested
     * @param recipient the heap region list where the regions will be added if the request succeeds
     * @param prepend if true, insert the allocated regions at the head of the list, otherwise at the tail.
     * @return true if the requested number of regions is allocated, false otherwise.
     */
    public synchronized boolean allocate(int numRegions, HeapRegionList recipient, boolean prepend) {
        if (allocated.size() + numRegions >= reserve) {
            return false;
        }
        final FixedSizeRegionAllocator regionAllocator = theHeapRegionManager.regionAllocator();
        int numRegionsNeeded = numRegions;
        do {
            RegionRange range = regionAllocator.allocateLessOrEqual(numRegions);
            // For now, every allocated region is always committed
            // Probably only want to do that on not already committed regions. The
            // region allocator should be able to discriminate that.
            regionAllocator.commit(range.firstRegion(), range.numRegions());
            recordAllocated(range.firstRegion(), range.numRegions(), recipient, prepend);
            numRegionsNeeded -= range.numRegions();
        } while(numRegionsNeeded == 0);
        return true;
    }

    /**
     * Allocate contiguous regions and add them in the specified region list.
     *
     * @param numRegions number of contiguous regions requested
     * @param recipient the heap region list where the regions will be added if the request succeeds
     * @param prepend if true, insert the regions at the head of the list, otherwise at the tail.
     * @return true if the requested number of contiguous regions is allocated, false otherwise.
     */
    public synchronized boolean allocateContiguous(int numRegions, HeapRegionList recipient, boolean prepend) {
        if (allocated.size() + numRegions >= reserve) {
            return false;
        }
        int regionID = theHeapRegionManager.regionAllocator().allocate(numRegions);
        if (regionID == INVALID_REGION_ID) {
            return false;
        }
        theHeapRegionManager.regionAllocator().commit(regionID, numRegions);
        recordAllocated(regionID, numRegions, recipient, prepend);
        return true;
    }
}