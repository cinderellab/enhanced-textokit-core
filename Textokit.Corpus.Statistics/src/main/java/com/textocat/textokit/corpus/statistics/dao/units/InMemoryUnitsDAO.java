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

import com.google.common.collect.Maps;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class InMemoryUnitsDAO implements UnitsDAO {

    private Map<UnitLocation, Unit> unitByUnitLocation = Maps.newHashMap();

    @Override
    public void addUnitItem(URI documentURI, int begin, int end,
                            String annotatorId, String annotatorClass) {
        UnitLocation location = new UnitLocation(documentURI, begin, end);
        if (unitByUnitLocation.containsKey(location)) {
            unitByUnitLocation.get(location).putClassByAnnotatorId(annotatorId,
                    annotatorClass);
        } else {
            unitByUnitLocation.put(location, new 