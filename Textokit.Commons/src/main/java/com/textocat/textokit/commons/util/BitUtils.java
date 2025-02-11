
/*
 *    Copyright 2015 Textocat
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */


package com.textocat.textokit.commons.util;

import java.util.BitSet;

/**
 * @author Rinat Gareev
 */
public class BitUtils {

    private BitUtils() {
    }

    /**
     * @param arg
     * @param filter
     * @return true only if arg contains all bits from filter
     */
    public static boolean contains(BitSet arg, BitSet filter) {
        for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i + 1)) {
            if (!arg.get(i)) {
                return false;
            }
        }
        return true;
    }

}