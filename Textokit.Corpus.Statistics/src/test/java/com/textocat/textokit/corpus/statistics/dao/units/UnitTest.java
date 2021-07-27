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

package com.textocat.textokit.corpus.statistics.dao.units;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertArrayEquals;

public class UnitTest {

    @Test
    public void testGetSortedClasses() throws URISyntaxException {
        Unit unit = new Unit(new UnitLocation(new URI("1"), 10, 15), "vasya",
                "cat");
        assertArrayEquals(new String[]{"cat"}, unit.getSortedClasses());
        unit.putClassByAnnotatorId("petya", "dog");
        assertArrayEquals(new String[]{"dog", "cat"},
                unit.getSortedClasses());
        unit.putClassByAnnotatorId("sasha", "bird");
        assertArrayEquals(new String[]{"dog", "bird", "cat"},
                unit.getSortedClasses());
    }

}
