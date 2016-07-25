/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.graal.nodes.java;

import static com.oracle.graal.nodeinfo.NodeCycles.CYCLES_10;
import static com.oracle.graal.nodeinfo.NodeSize.SIZE_4;

import com.oracle.graal.compiler.common.LocationIdentity;
import com.oracle.graal.compiler.common.type.StampFactory;
import com.oracle.graal.graph.NodeClass;
import com.oracle.graal.nodeinfo.NodeInfo;
import com.oracle.graal.nodes.ValueNode;
import com.oracle.graal.nodes.memory.AbstractMemoryCheckpoint;
import com.oracle.graal.nodes.memory.MemoryCheckpoint;
import com.oracle.graal.nodes.spi.Lowerable;
import com.oracle.graal.nodes.spi.LoweringTool;

import jdk.vm.ci.meta.JavaKind;
import sun.misc.Unsafe;

/**
 * Represents an atomic read-and-write operation like {@link Unsafe#getAndSetInt(Object, long, int)}
 * .
 */
@NodeInfo(cycles = CYCLES_10, size = SIZE_4)
public final class AtomicReadAndWriteNode extends AbstractMemoryCheckpoint implements Lowerable, MemoryCheckpoint.Single {

    public static final NodeClass<AtomicReadAndWriteNode> TYPE = NodeClass.create(AtomicReadAndWriteNode.class);
    @Input ValueNode object;
    @Input ValueNode offset;
    @Input ValueNode newValue;

    protected final JavaKind valueKind;
    protected final LocationIdentity locationIdentity;

    public AtomicReadAndWriteNode(ValueNode object, ValueNode offset, ValueNode newValue, JavaKind valueKind, LocationIdentity locationIdentity) {
        super(TYPE, StampFactory.forKind(newValue.getStackKind()));
        this.object = object;
        this.offset = offset;
        this.newValue = newValue;
        this.valueKind = valueKind;
        this.locationIdentity = locationIdentity;
    }

    public ValueNode object() {
        return object;
    }

    public ValueNode offset() {
        return offset;
    }

    public ValueNode newValue() {
        return newValue;
    }

    public JavaKind getValueKind() {
        return valueKind;
    }

    @Override
    public LocationIdentity getLocationIdentity() {
        return locationIdentity;
    }

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }
}
