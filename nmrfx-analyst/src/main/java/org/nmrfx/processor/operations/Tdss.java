/*
 * NMRFx Processor : A Program for Processing NMR Data
 * Copyright (C) 2004-2017 One Moon Scientific, Inc., Westfield, N.J., USA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.nmrfx.processor.operations;

import org.nmrfx.annotations.PythonAPI;
import org.nmrfx.math.units.Unit;
import org.nmrfx.processor.math.Vec;

/**
 * @author johnsonb
 */
@PythonAPI("pyproc")
public class Tdss extends Operation {

    private final int winSize;
    private final int nPasses;
    private final double shiftPoints;
    private final Unit unit;

    public Tdss(int winSize, int nPasses, double shiftPoints) {
        this.winSize = winSize;
        this.nPasses = nPasses;
        this.shiftPoints = shiftPoints;
        this.unit = null;
    }

    public Tdss(int winSize, int nPasses, Unit unit) {
        this.winSize = winSize;
        this.nPasses = nPasses;
        this.unit = unit;
        this.shiftPoints = 0;
    }

    @Override
    public Tdss eval(Vec vector) throws OperationException {
        tdss(vector);
        return this;
    }

    private void tdss(Vec vector) throws OperationException {
        double shift = shiftPoints;
        if (unit != null) {
            shift = unit.getDoublePosition(vector);
        }
        if (Math.abs(shift) > 0.5) {
            vector.phase(0.0, 360.0 * shift, false, false);
        }
        vector.tdSSWithFilter(winSize, nPasses);
        if (Math.abs(shift) > 0.5) {
            vector.phase(0.0, -360.0 * shift, false, false);
        }
    }
}
