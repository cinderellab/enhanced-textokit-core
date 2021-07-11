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
            unitByUnitLocation.put(location, new Unit(location, annotatorId,
                    annotatorClass));
        }
    }

    @Override
    public Iterable<Unit> getUnits() {
        return unitByUnitLocation.values();
    }

    @Override
    public void toTSV(Writer writer) {
        PrintWriter pw = new PrintWriter(writer);
        for (Unit unit : this.getUnits()) {
            URI documentURI = unit.getDocumentURI();
            int begin = unit.getBegin();
            int end = unit.getEnd();
            for (Map.Entry<String, String> annotatorIdAndClass : unit
                    .getClassesByAnnotatorId().entrySet()) {
                String annotatorId = annotatorIdAndClass.getKey();
                String annotatorClass = annotatorIdAndClass.getValue();
                pw.format("%s\t%d\t%d\t%s\t%s%n", documentURI, begin, end,
                        annotatorId, annotatorClass);
            }
        }
    }

    @Override
    public void addUnitsFromTSV(Reader reader) throws IOException,
            URISyntaxException {
        BufferedReader br = new BufferedReader(reader);
        String l;
        while ((l = br.readLine()) != null) {
            String[] columnDetail = l.split("\t", -1);
            URI documentURI = new URI(columnDetail[0]);
            int begin = Integer.parseInt(columnDetail[1]);
            int end = Integer.parseInt(columnDetail[2]);
            String annotatorId = columnDetail[3];
            String annotatorClass = columnDetail[4];
            this.addUnitItem(documentURI, begin, end, annotatorId,
                    annotatorClass);
        }
    }

}
