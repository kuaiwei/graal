/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package com.oracle.svm.reflect.target;

import org.graalvm.compiler.serviceprovider.JavaVersionUtil;

import com.oracle.svm.core.snippets.KnownIntrinsics;
import com.oracle.svm.core.util.VMError;

class ReflectionHelper {
    static Target_java_lang_reflect_Executable getHolder(Target_java_lang_reflect_Executable executable) {
        Target_java_lang_reflect_Executable holder = getRoot(executable);
        if (holder == null) {
            holder = executable;
        }
        return holder;
    }

    static Target_java_lang_reflect_Method getHolder(Target_java_lang_reflect_Method method) {
        Target_java_lang_reflect_Method holder = getRoot(method);
        if (holder == null) {
            holder = method;
        }

        return holder;
    }

    static Target_java_lang_reflect_Constructor getHolder(Target_java_lang_reflect_Constructor constructor) {
        Target_java_lang_reflect_Constructor holder = getRoot(constructor);
        if (holder == null) {
            holder = constructor;
        }

        return holder;
    }

    static Target_java_lang_reflect_Field getHolder(Target_java_lang_reflect_Field field) {
        Target_java_lang_reflect_Field holder = field.root;
        if (holder == null) {
            holder = field;
        }

        return holder;
    }

    static <T> T requireNonNull(T object, String errorMessage) {
        if (object == null) {
            throw VMError.shouldNotReachHere(errorMessage);
        }
        return object;
    }

    private static Target_java_lang_reflect_Method asMethod(Object object) {
        return KnownIntrinsics.unsafeCast(object, Target_java_lang_reflect_Method.class);
    }

    private static Target_java_lang_reflect_Constructor asConstructor(Object object) {
        return KnownIntrinsics.unsafeCast(object, Target_java_lang_reflect_Constructor.class);
    }

    private static Target_java_lang_reflect_Executable asExecutable(Object object) {
        return KnownIntrinsics.unsafeCast(object, Target_java_lang_reflect_Executable.class);
    }

    private static Target_java_lang_reflect_AccessibleObject asAccessibleObject(Object object) {
        return KnownIntrinsics.unsafeCast(object, Target_java_lang_reflect_AccessibleObject.class);
    }

    /*
     * We need a separated getRoot() method for each of Method and Constructor to disambiguate
     * between the 2 types. This code is doing type conversions via KnownIntrinsics.unsafeCast()
     * which is not modeled during analysis, therefore no type filtering is performed and
     * getHolder() methods above could return, for example, both Method and Constructor at the same
     * time, which would be wrong.
     */

    private static Target_java_lang_reflect_Executable getRoot(Target_java_lang_reflect_Executable executable) {
        if (JavaVersionUtil.Java8OrEarlier) {
            return executable.getRoot();
        } else {
            return asExecutable(asAccessibleObject(executable).getRoot());
        }
    }

    private static Target_java_lang_reflect_Method getRoot(Target_java_lang_reflect_Method method) {
        if (JavaVersionUtil.Java8OrEarlier) {
            return asMethod(asExecutable(method).getRoot());
        } else {
            return asMethod(asAccessibleObject(method).getRoot());
        }
    }

    private static Target_java_lang_reflect_Constructor getRoot(Target_java_lang_reflect_Constructor constructor) {
        if (JavaVersionUtil.Java8OrEarlier) {
            return asConstructor(asExecutable(constructor).getRoot());
        } else {
            return asConstructor(asAccessibleObject(constructor).getRoot());
        }
    }
}
