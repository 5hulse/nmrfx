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
package org.nmrfx.chemistry;

import org.nmrfx.chemistry.io.NMRStarReader;
import org.nmrfx.chemistry.utilities.NvUtil;
import org.nmrfx.peaks.*;
import org.nmrfx.star.Loop;
import org.nmrfx.star.ParseException;
import org.nmrfx.star.Saveframe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Bruce Johnson
 */
public class AtomResonance {

    private static final Logger log = LoggerFactory.getLogger(AtomResonance.class);

    Atom atom = null;
    public final static String[] resonanceLoopStrings = {
            "_Resonance.ID",
            "_Resonance.Name",
            "_Resonance.Resonance_set_ID",
            "_Resonance.Spin_system_ID ",
            "_Resonance.Resonance_linker_list_ID ",};
    public static final String[] resonanceCovalentLinkStrings = {
            "_Resonance_covalent_link.Resonance_ID_1",
            "_Resonance_covalent_link.Resonance_ID_2",};

    Object resonanceSet = null;
    Object ssID = null;
    boolean labelValid = true;
    String atomName = "";
    List<PeakDim> peakDims = new ArrayList<>();
    private List<String> names;
    private long id;

    public AtomResonance(long id) {
        this.names = null;
        this.id = id;
    }

    public AtomResonance copy() {
        AtomResonance copy = new AtomResonance(getID());
        copy.getPeakDims().addAll(getPeakDims());
        if (getNames() != null) {
            copy.setName(getNames());
            copy.setName(getName());
        }
        copy.setAtomName(getAtomName());
        copy.atom = atom;
        return copy;
    }

    public void setName(List<String> newNames) {
        if (names == null) {
            names = new ArrayList<>();
        }
        names.clear();
        if (newNames != null) {
            names.addAll(newNames);
        }
        boolean valid = true;
        if (newNames != null) {
            for (var name : newNames) {
                if (!isLabelValid(name)) {
                    valid = false;
                    break;
                }
            }
        }
        labelValid = valid;
    }

    private boolean isLabelValid(String name) {
        MoleculeBase molBase = MoleculeFactory.getActive();
        boolean result = true;
        if (!name.isBlank() && (molBase != null)) {
            Atom testAtom = molBase.findAtom(name);
            result = testAtom != null;
        }
        return result;
    }

    public boolean isLabelValid() {
        return labelValid;
    }

    public String getAtomName() {
        if (atom != null) {
            return atom.getFullName();
        } else {
            return atomName;
        }
    }

    public void setAtom(Atom atom) {
        this.atom = atom;
    }

    public Atom getAtom() {
        return atom;
    }

    public Atom getPossibleAtom() {
        if (atom != null) {
            return atom;
        } else {
            Atom possibleAtom = null;
            MoleculeBase molBase = MoleculeFactory.getActive();
            String name = getName();
            if (!name.isBlank() && (molBase != null)) {
                possibleAtom = molBase.findAtom(name);
            }
            return possibleAtom;
        }
    }

    public static void processSTAR3ResonanceList(final NMRStarReader nmrStar,
                                                 Saveframe saveframe, Map<String, Compound> compoundMap) throws ParseException {
        // fixme unused String listName = saveframe.getValue(interp,"_Resonance_linker_list","Sf_framecode");
        // FIXME String details = saveframe.getValue(interp,"_Resonance_linker_list","Details");

        //  FIXME Should have Resonance lists PeakList peakList = new PeakList(listName,nDim);
        Loop loop = saveframe.getLoop("_Resonance");
        if (loop == null) {
            throw new ParseException("No \"_Resonance\" loop");
        }
        List<String> idColumn = loop.getColumnAsList("ID");
        List<String> nameColumn = loop.getColumnAsList("Name");
        List<String> resSetColumn = loop.getColumnAsList("Resonance_set_ID");
        // fixme unused ArrayList ssColumn = loop.getColumnAsList("Spin_system_ID");
        ResonanceFactory resFactory = PeakList.resFactory();
        for (int i = 0, n = idColumn.size(); i < n; i++) {
            String value;
            long idNum;
            if ((value = NvUtil.getColumnValue(idColumn, i)) != null) {
                idNum = NvUtil.toLong(value);
            } else {
                continue;
            }

            AtomResonance resonance = (AtomResonance) resFactory.get(idNum);
            if (resonance == null) {
                resonance = (AtomResonance) resFactory.build(idNum);
            }
            if ((value = NvUtil.getColumnValue(nameColumn, i)) != null) {
                resonance.setName(value);
            }
        }

        loop = saveframe.getLoop("_Resonance_assignment");
        if (loop != null) {
            List<String> resSetIDColumn = loop.getColumnAsList("Resonance_set_ID");
            List<String> entityAssemblyIDColumn = loop.getColumnAsList("Entity_assembly_ID");
            List<String> entityIDColumn = loop.getColumnAsList("Entity_ID");
            List<String> compIdxIDColumn = loop.getColumnAsList("Comp_index_ID");
            List<String> compIDColumn = loop.getColumnAsList("Comp_ID");
            List<String> atomIDColumn = loop.getColumnAsList("Atom_ID");
            // fixme unused ArrayList atomSetIDColumn = loop.getColumnAsList("Atom_set_ID");
            for (int i = 0, n = resSetIDColumn.size(); i < n; i++) {
                String value;
                long idNum = 0;
                if ((value = NvUtil.getColumnValue(resSetIDColumn, i)) != null) {
                    idNum = NvUtil.toLong(value);
                } else {
                    continue;
                }
                String atomName = "";
                String iRes = "";
                String entityAssemblyID = "";
                String entityID = "";
                if ((value = NvUtil.getColumnValue(entityAssemblyIDColumn, i)) != null) {
                    entityAssemblyID = value;
                }
                if (entityAssemblyID.equals("")) {
                    entityAssemblyID = "1";
                }
                if ((value = NvUtil.getColumnValue(entityIDColumn, i)) != null) {
                    entityID = value;
                } else {
                    throw new ParseException("No entity ID");
                }
                if ((value = NvUtil.getColumnValue(compIdxIDColumn, i)) != null) {
                    iRes = value;
                } else {
                    throw new ParseException("No compound ID");
                }
                if ((value = NvUtil.getColumnValue(atomIDColumn, i)) != null) {
                    atomName = value;
                } else {
                    throw new ParseException("No atom ID");
                }
                // fixme if ((value = NvUtil.getColumnValue(atomSetIDColumn,i)) != null) {
                // fixme unused atomSetNum = NvUtil.toLong(interp,value);
                //}

                String mapID = entityAssemblyID + "." + entityID + "." + iRes;
                Compound compound = compoundMap.get(mapID);
                if (compound == null) {
                    log.warn("invalid compound in assignments saveframe \"{}\"", mapID);
                    continue;
                }
                Atom atom = compound.getAtomLoose(atomName);
                if (atom == null) {
                    if (atomName.equals("H")) {
                        atom = compound.getAtom(atomName + "1");
                    }
                }
                if (atom == null) {
                    log.warn("invalid atom in assignments saveframe \"{}.{}\"", mapID, atomName);
                } else {
                }
            }
        }
    }

    public String toSTARResonanceString() {
        StringBuilder result = new StringBuilder();
        String sep = " ";
        char stringQuote = '"';
        result.append(String.valueOf(getID())).append(sep);
        result.append(stringQuote);
        result.append(getName());
        result.append(stringQuote);
        result.append(sep);
        if (resonanceSet == null) {
            result.append(".");
        }
        result.append(sep);
        if (ssID == null) {
            result.append(".");
        } else {
            result.append(ssID);
        }
        result.append(sep);
        result.append("1");
        return result.toString();
    }

    public void clearPeakDims() {
        peakDims = null;
    }

    public List<String> getNames() {
        return names;
    }

    public void remove(PeakDim peakDim) {
        peakDims.remove(peakDim);
    }

    public String getName() {
        String result = "";
        if (names != null) {
            if (names.size() == 1) {
                result = names.get(0);
            } else if (names.size() > 1) {
                StringBuilder builder = new StringBuilder();
                for (String name : names) {
                    if (builder.length() > 0) {
                        builder.append(" ");
                    }
                    builder.append(name);
                }
                result = builder.toString();
            }
        }
        return result;
    }

    public void setName(String name) {
        setName(List.of(name));
    }

    public void setAtomName(String aName) {
        atomName = aName;
    }

    public String getIDString() {
        return String.valueOf(id);

    }

    public void setID(long value) {
        id = value;
    }

    public long getID() {
        return id;
    }

    public static void merge(AtomResonance resA, AtomResonance resB) {
        resA.merge(resB);
    }
    public void merge(AtomResonance resB) {
        if (resB != this) {
            Collection<PeakDim> peakDimsB = resB.getPeakDims();
            int sizeA = peakDims.size();
            int sizeB = peakDimsB.size();
            for (PeakDim peakDim : peakDimsB) {
                peakDim.setResonance(this);
                if (!peakDims.contains(peakDim)) {
                    peakDims.add(peakDim);
                }
            }
            peakDimsB.clear();
        }

    }

    public List<PeakDim> getPeakDims() {
        // fixme should be unmodifiable or copy
        return peakDims;
    }

    public void add(PeakDim peakDim) {
        peakDim.setResonance(this);
        if (!peakDims.contains(peakDim)) {
            peakDims.add(peakDim);
        }
    }

    public Double getPPMAvg(String condition) {
        double sum = 0.0;
        int n = 0;
        Double result = null;
        for (PeakDim peakDim : peakDims) {
            if (peakDim == null) {
                continue;
            }
            if ((condition != null) && (condition.length() > 0)) {
                String peakCondition = peakDim.getPeak().getPeakList().getSampleConditionLabel();
                if ((peakCondition == null) || (!condition.equals(peakCondition))) {
                    continue;
                }
            }
            if (peakDim.getChemShift() != null) {
                sum += peakDim.getChemShift();
                n++;
            }
        }
        if (n > 0) {
            result = sum / n;
        }
        return result;
    }

    public Double getWidthAvg(String condition) {
        double sum = 0.0;
        int n = 0;
        Double result = null;
        for (PeakDim peakDim : peakDims) {
            if (peakDim == null) {
                continue;
            }
            if ((condition != null) && (condition.length() > 0)) {
                String peakCondition = peakDim.getPeak().getPeakList().getSampleConditionLabel();
                if ((peakCondition == null) || (!condition.equals(peakCondition))) {
                    continue;
                }
            }
            Float lw = peakDim.getLineWidth();
            if (lw != null) {
                sum += lw;
                n++;
            }
        }
        if (n > 0) {
            result = sum / n;
        }
        return result;
    }

    public Double getPPMDev(String condition) {
        double sum = 0.0;
        double sumsq = 0.0;
        int n = 0;
        Double result = null;
        for (PeakDim peakDim : peakDims) {
            if (peakDim == null) {
                continue;
            }
            if ((condition != null) && (condition.length() > 0)) {
                String peakCondition = peakDim.getPeak().getPeakList().getSampleConditionLabel();
                if ((peakCondition == null) || (!condition.equals(peakCondition))) {
                    continue;
                }
            }
            if (peakDim.getChemShift() != null) {
                sum += peakDim.getChemShift();
                sumsq += peakDim.getChemShift() * peakDim.getChemShift();
                n++;
            }
        }
        if (n > 1) {
            double mean = sum / n;
            double devsq = sumsq / n - mean * mean;
            if (devsq > 0.0) {
                result = Math.sqrt(devsq);
            } else {
                result = 0.0;
            }
        } else if (n == 1) {
            result = 0.0;
        }

        return result;
    }

    public int getPeakCount(String condition) {
        int n = 0;
        for (PeakDim peakDim : peakDims) {
            if ((condition != null) && (condition.length() > 0)) {
                String peakCondition = peakDim.getPeak().getPeakList().getSampleConditionLabel();
                if ((peakCondition == null) || (!condition.equals(peakCondition))) {
                    continue;
                }
            }
            n++;
        }
        return n;
    }
}
