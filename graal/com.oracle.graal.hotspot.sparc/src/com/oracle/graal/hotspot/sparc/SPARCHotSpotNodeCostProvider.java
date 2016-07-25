/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.hotspot.sparc;

import com.oracle.graal.graph.Node;
import com.oracle.graal.hotspot.nodes.HotSpotNodeCostProvider;
import com.oracle.graal.hotspot.nodes.JumpToExceptionHandlerNode;
import com.oracle.graal.nodeinfo.NodeCycles;
import com.oracle.graal.nodeinfo.NodeSize;
import com.oracle.graal.nodes.ReturnNode;

public class SPARCHotSpotNodeCostProvider extends HotSpotNodeCostProvider {

    @Override
    public NodeCycles cycles(Node n) {
        if (n instanceof ReturnNode) {
            return NodeCycles.CYCLES_6;
        } else if (n instanceof JumpToExceptionHandlerNode) {
            // restore caller window
            return NodeCycles.CYCLES_3;
        }
        return super.cycles(n);
    }

    @Override
    public NodeSize size(Node n) {
        if (n instanceof ReturnNode) {
            return NodeSize.SIZE_4;
        } else if (n instanceof JumpToExceptionHandlerNode) {
            // restore caller window
            return NodeSize.SIZE_3;
        }
        return super.size(n);
    }
}
