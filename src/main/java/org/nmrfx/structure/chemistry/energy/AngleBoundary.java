/*
 * NMRFx Structure : A Program for Calculating Structures 
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
package org.nmrfx.structure.chemistry.energy;

import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.InvalidMoleculeException;
import org.nmrfx.structure.chemistry.MolFilter;
import org.nmrfx.structure.chemistry.Molecule;
import org.nmrfx.structure.chemistry.SpatialSet;
import java.util.HashMap;
import java.util.List;

/**
 * This class determines if the angle boundary is valid - angle boundary for each bond. The angle must be between -180
 * degrees and 360 degrees
 */
public class AngleBoundary {

    private static HashMap<String, AngleProp> boundaries = new HashMap<String, AngleProp>();
    /**
     * Upper Angle Bound
     */
    final double upper;
    /**
     * Lower Angle Bound
     */
    final double lower;
    Atom atom = null;
    /**
     * Represents the particular angle properties of the angle whose boundary is specified
     */
    AngleProp angleProp = null;
    /**
     * Scale
     */
    final double scale;
    /**
     * Index to list of angles
     */
    int index = -1;
    final static double toRad = Math.PI / 180.0;

    public AngleBoundary(final String atomName, final double lower, final double upper, final double scale) throws InvalidMoleculeException {
        /*Changed from Original*/
        if ((lower < -180.0) || (upper > 360.0) || (upper < lower)) {
            throw new IllegalArgumentException("Invalid angle bounds: " + lower + " " + upper);
        }
        if ((lower > 180) && (upper > 180)) {
            throw new IllegalArgumentException("Invalid angle bounds: " + lower + " " + upper);
        }

        this.lower = lower * toRad;
        this.upper = upper * toRad;
        this.scale = scale;
        MolFilter molFilter = new MolFilter(atomName);
        List<SpatialSet> atoms = Molecule.matchAtoms(molFilter);
        if (atoms.size() == 0) {
            throw new IllegalArgumentException("Invalid atom " + atomName);
        }
        SpatialSet spatialSet = atoms.get(0);
        atom = spatialSet.atom;
        if (boundaries.containsKey(atom.getFullName())) {
            String angleName = boundaries.get(atom.getFullName()).angleName;
            this.angleProp = AngleProp.map.get(angleName);
        }

    }

    /**
     * public AngleBoundary(final String atomName, final double lower, final double upper, final double scale, double[]
     * target, double[] sigma, double[] height) {
     *
     * if ((lower < -180.0) || (upper > 360.0) || (upper < lower)) { throw new
     * IllegalArgumentException("Invalid angle bounds: " + lower + " " + upper);
     * } if ((lower > 180) && (upper > 180)) { throw new IllegalArgumentException("Invalid angle bounds: " + lower + " "
     * + upper); }
     *
     * this.lower = lower * toRad; this.upper = upper * toRad; this.scale = scale;
     *
     * MolFilter molFilter = new MolFilter(atomName); Vector atoms = Molecule.matchAtoms(molFilter); SpatialSet
     * spatialSet = (SpatialSet) atoms.elementAt(0); atom = spatialSet.atom; angleProp = new AngleProp(atom.name,
     * target, sigma, height); }
     */
    public AngleBoundary(final String atomName, final double lower, final double upper, final double scale, final String angleName) throws InvalidMoleculeException {
        if ((lower < -180.0) || (upper > 360.0) || (upper < lower)) {
            throw new IllegalArgumentException("Invalid angle bounds: " + lower + " " + upper);
        }
        if ((lower > 180) && (upper > 180)) {
            throw new IllegalArgumentException("Invalid angle bounds: " + lower + " " + upper);
        }

        this.lower = lower * toRad;
        this.upper = upper * toRad;
        this.scale = scale;

        MolFilter molFilter = new MolFilter(atomName);
        List<SpatialSet> atoms = Molecule.matchAtoms(molFilter);
        angleProp = AngleProp.map.get(angleName);
        for (int i = 0; i < atoms.size(); i++) {
            SpatialSet spatialSet = (SpatialSet) atoms.get(i);
            atom = spatialSet.atom;
            boundaries.put(spatialSet.atom.getFullName(), this.angleProp);
        }
    }

    public void setIndex(final int index) {
        this.index = index;
    }

    public Atom getAtom() {
        return atom;
    }
}
