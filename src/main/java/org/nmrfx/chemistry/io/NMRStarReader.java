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
package org.nmrfx.chemistry.io;

import org.nmrfx.chemistry.Order;
import java.io.BufferedReader;

import org.nmrfx.datasets.DatasetBase;
import org.nmrfx.chemistry.*;
import org.nmrfx.chemistry.constraints.*;
import org.nmrfx.chemistry.Residue.RES_POSITION;
import org.nmrfx.chemistry.AtomResonance;
import org.nmrfx.star.Loop;
import org.nmrfx.star.ParseException;
import org.nmrfx.star.STAR3;
import org.nmrfx.star.Saveframe;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.*;
import org.nmrfx.peaks.AbsMultipletComponent;
import org.nmrfx.peaks.ComplexCoupling;
import org.nmrfx.peaks.CouplingPattern;
import org.nmrfx.peaks.Multiplet;
import org.nmrfx.peaks.PeakDim;
import org.nmrfx.peaks.PeakList;
import org.nmrfx.peaks.Resonance;
import org.nmrfx.peaks.ResonanceFactory;
import org.nmrfx.peaks.SpectralDim;
import org.nmrfx.peaks.Peak;
import org.nmrfx.utilities.NvUtil;
import org.nmrfx.peaks.io.PeakPathReader;
import org.nmrfx.chemistry.RelaxationData.relaxTypes;

/**
 *
 * @author brucejohnson
 */
public class NMRStarReader {

    static String[] polymerEntityStrings = {"_Entity.Sf_category", "_Entity.Sf_framecode", "_Entity.Entry_ID", "_Entity.ID", "_Entity.Name", "_Entity.Type", "_Entity.Polymer_type", "_Entity.Polymer_strand_ID", "_Entity.Polymer_seq_one_letter_code_can", "_Entity.Polymer_seq_one_letter_code"};

    final STAR3 star3;
    final File starFile;

    Map entities = new HashMap();
    boolean hasResonances = false;
    List<PeakDim> peakDimsWithoutResonance = new ArrayList<>();
    public static boolean DEBUG = false;
    MoleculeBase molecule = null;

    public NMRStarReader(final File starFile, final STAR3 star3) {
        this.star3 = star3;
        this.starFile = starFile;
//        PeakDim.setResonanceFactory(new AtomResonanceFactory());
    }

    public static STAR3 read(String starFileName) throws ParseException {
        File file = new File(starFileName);
        return read(file);
    }

    public static STAR3 read(File starFile) throws ParseException {
        FileReader fileReader;
        try {
            fileReader = new FileReader(starFile);
        } catch (FileNotFoundException ex) {
            return null;
        }
        BufferedReader bfR = new BufferedReader(fileReader);

        STAR3 star = new STAR3(bfR, "star3");

        try {
            star.scanFile();
        } catch (ParseException parseEx) {
            throw new ParseException(parseEx.getMessage() + " " + star.getLastLine());
        }
        NMRStarReader reader = new NMRStarReader(starFile, star);
        reader.process();
        return star;
    }

    static void updateFromSTAR3ChemComp(Saveframe saveframe, Compound compound) throws ParseException {
        Loop loop = saveframe.getLoop("_Chem_comp_atom");
        if (loop == null) {
            throw new ParseException("No \"_Chem_comp_atom\" loop in \"" + saveframe.getName() + "\"");
        }
        List<String> idColumn = loop.getColumnAsList("Atom_ID");
        List<String> typeColumn = loop.getColumnAsList("Type_symbol");
        for (int i = 0; i < idColumn.size(); i++) {
            String aName = (String) idColumn.get(i);
            String aType = (String) typeColumn.get(i);
            Atom atom = Atom.genAtomWithElement(aName, aType);
            compound.addAtom(atom);
        }
        compound.updateNames();
        loop = saveframe.getLoop("_Chem_comp_bond");
        if (loop != null) {
            List<String> id1Column = loop.getColumnAsList("Atom_ID_1");
            List<String> id2Column = loop.getColumnAsList("Atom_ID_2");
            List<String> orderColumn = loop.getColumnAsList("Value_order");
            for (int i = 0; i < id1Column.size(); i++) {
                String aName1 = (String) id1Column.get(i);
                String aName2 = (String) id2Column.get(i);
                String orderString = (String) orderColumn.get(i);
                Atom atom1 = compound.getAtom(aName1);
                Atom atom2 = compound.getAtom(aName2);
                Order order = Order.SINGLE;
                if (orderString.toUpperCase().startsWith("SING")) {
                    order = Order.SINGLE;
                } else if (orderString.toUpperCase().startsWith("DOUB")) {
                    order = Order.DOUBLE;
                } else if (orderString.toUpperCase().startsWith("TRIP")) {
                    order = Order.TRIPLE;
                } else {
                    order = Order.SINGLE;
                }
                int stereo = 0;
                Atom.addBond(atom1, atom2, order, stereo, false);
            }
            for (Atom atom : compound.getAtoms()) {
                if (atom.bonds == null) {
                    System.out.println("no bonds");
                }
            }
        }
    }

    static void addComponents(Saveframe saveframe, List<String> idColumn, List<String> authSeqIDColumn, List<String> compIDColumn, List<String> entityIDColumn, Compound compound) throws ParseException {
        for (int i = 0; i < compIDColumn.size(); i++) {
            String resName = (String) compIDColumn.get(i);
            String seqNumber = (String) authSeqIDColumn.get(i);
            String ccSaveFrameName = "save_chem_comp_" + resName;
            Saveframe ccSaveframe = saveframe.getSTAR3().getSaveframe(ccSaveFrameName);
            if (ccSaveframe == null) {
                ccSaveFrameName = "save_" + resName;
                ccSaveframe = saveframe.getSTAR3().getSaveframe(ccSaveFrameName);
            }
            if (ccSaveframe != null) {
                compound.setNumber(seqNumber);
                updateFromSTAR3ChemComp(ccSaveframe, compound);
            } else {
                System.out.println("No save frame: " + ccSaveFrameName);
            }
        }

    }

    public static void processLoop(STAR3 star3, List tagList) {
    }

    static void finishSaveFrameProcessing(final NMRStarReader nmrStar, Saveframe saveframe, Compound compound, String mapID) throws ParseException {
        Loop loop = saveframe.getLoop("_Entity_comp_index");
        if (loop != null) {
            List<String> idColumn = loop.getColumnAsList("ID");
            List<String> authSeqIDColumn = loop.getColumnAsList("Auth_seq_ID");
            List<String> entityIDColumn = loop.getColumnAsList("Entity_ID");
            List<String> compIDColumn = loop.getColumnAsList("Comp_ID");
            nmrStar.addCompound(mapID, compound);
            addComponents(saveframe, idColumn, authSeqIDColumn, compIDColumn, entityIDColumn, compound);
        } else {
            System.out.println("No \"_Entity_comp_index\" loop");
        }

    }

    public void finishSaveFrameProcessing(final Polymer polymer, final Saveframe saveframe, final String nomenclature, final boolean capped) throws ParseException {
        String lstrandID = saveframe.getOptionalValue("_Entity", "Polymer_strand_ID");
        if (lstrandID != null) {
            if (DEBUG) {
                System.out.println("set strand " + lstrandID);
            }
            polymer.setStrandID(lstrandID);
        }
        String type = saveframe.getOptionalValue("_Entity", "Polymer_type");
        if (type != null) {
            if (DEBUG) {
                System.out.println("set polytype " + type);
            }
            polymer.setPolymerType(type);
        }
        Loop loop = saveframe.getLoop("_Entity_comp_index");
        if (loop == null) {
            System.out.println("No \"_Entity_comp_index\" loop");
        } else {
            List<String> idColumn = loop.getColumnAsList("ID");
            List<String> authSeqIDColumn = loop.getColumnAsList("Auth_seq_ID");
            List<String> entityIDColumn = loop.getColumnAsList("Entity_ID");
            List<String> compIDColumn = loop.getColumnAsList("Comp_ID");
            addResidues(polymer, saveframe, idColumn, authSeqIDColumn, compIDColumn, entityIDColumn, nomenclature);
            polymer.setCapped(capped);
            if (capped) {
                PDBFile.capPolymer(polymer);
            }
        }
        loop = saveframe.getLoop("_Entity_chem_comp_deleted_atom");
        if (loop != null) {
            List<String> idColumn = loop.getColumnAsList("ID");
            List<String> compIndexIDColumn = loop.getColumnAsList("Comp_index_ID");
            List<String> compIDColumn = loop.getColumnAsList("Comp_ID");
            List<String> atomIDColumn = loop.getColumnAsList("Atom_ID");
            List<String> entityIDColumn = loop.getColumnAsList("Entity_ID");
            polymer.removeAtoms(compIndexIDColumn, atomIDColumn);
        }
        loop = saveframe.getLoop("_Entity_bond");
        if (loop != null) {
            List<String> orderColumn = loop.getColumnAsList("Value_order");
            List<String> comp1IndexIDColumn = loop.getColumnAsList("Comp_index_ID_1");
            List<String> atom1IDColumn = loop.getColumnAsList("Atom_ID_1");
            List<String> comp2IndexIDColumn = loop.getColumnAsList("Comp_index_ID_2");
            List<String> atom2IDColumn = loop.getColumnAsList("Atom_ID_2");
            polymer.addBonds(orderColumn, comp1IndexIDColumn, atom1IDColumn, comp2IndexIDColumn, atom2IDColumn);
        }
        polymer.molecule.genCoords(false);
        polymer.molecule.setupRotGroups();
    }

    public void addResidues(Polymer polymer, Saveframe saveframe, List<String> idColumn, List<String> authSeqIDColumn, List<String> compIDColumn, List<String> entityIDColumn, String frameNomenclature) throws ParseException {
        String reslibDir = PDBFile.getReslibDir(frameNomenclature);
        polymer.setNomenclature(frameNomenclature.toUpperCase());
        Sequence sequence = new Sequence();
        for (int i = 0; i < compIDColumn.size(); i++) {
            String resName = (String) compIDColumn.get(i);
            String iEntity = (String) entityIDColumn.get(i);
            String iRes = (String) idColumn.get(i);
            String mapID = polymer.assemblyID + "." + iEntity + "." + iRes;
            if (authSeqIDColumn != null) {
                String iResTemp = (String) authSeqIDColumn.get(i);
                if (!iResTemp.equals(".")) {
                    iRes = iResTemp;
                }
            }
            if (resName.length() == 1) {
                char RD = 'd';
                if (polymer.getPolymerType().equalsIgnoreCase("polyribonucleotide")) {
                    RD = 'r';
                }
                resName = AtomParser.pdbResToPRFName(resName, RD);
            }
            Residue residue = new Residue(iRes, resName.toUpperCase());
            residue.molecule = polymer.molecule;
            addCompound(mapID, residue);
            polymer.addResidue(residue);
            String ccSaveFrameName = "save_chem_comp_" + resName + "." + iRes;
            Saveframe ccSaveframe = saveframe.getSTAR3().getSaveframe(ccSaveFrameName);
            if (ccSaveframe == null) {
                ccSaveFrameName = "save_chem_comp_" + resName;
                ccSaveframe = saveframe.getSTAR3().getSaveframe(ccSaveFrameName);
            }
            if (ccSaveframe == null) {
                ccSaveFrameName = "save_" + resName;
                ccSaveframe = saveframe.getSTAR3().getSaveframe(ccSaveFrameName);
            }
            RES_POSITION resPos = RES_POSITION.MIDDLE;
            if (ccSaveframe != null) {
                updateFromSTAR3ChemComp(ccSaveframe, residue);
            } else {
                try {
                    if (!sequence.addResidue(reslibDir + "/" + Sequence.getAliased(resName.toLowerCase()) + ".prf", residue, resPos, "", false)) {
                        throw new ParseException("Can't find residue \"" + resName + "\" in residue libraries or STAR file");
                    }
                } catch (MoleculeIOException psE) {
                    throw new ParseException(psE.getMessage());
                }
            }
        }
        sequence.removeBadBonds();
    }

    public void addCompound(String id, Compound compound) {
        var compoundMap = MoleculeBase.compoundMap();
        compoundMap.put(id, compound);
    }

    public void buildChemShifts(int fromSet, final int toSet) throws ParseException {
        Iterator iter = star3.getSaveFrames().values().iterator();
        int iSet = 0;
        while (iter.hasNext()) {
            Saveframe saveframe = (Saveframe) iter.next();
            if (saveframe.getCategoryName().equals("assigned_chemical_shifts")) {
                if (DEBUG) {
                    System.err.println("process chem shifts " + saveframe.getName());
                }
                if (fromSet < 0) {
                    processChemicalShifts(saveframe, iSet);
                } else if (fromSet == iSet) {
                    processChemicalShifts(saveframe, toSet);
                    break;
                }
                iSet++;
            }
        }
    }

    public void buildConformers() throws ParseException {
        Iterator iter = star3.getSaveFrames().values().iterator();
        while (iter.hasNext()) {
            Saveframe saveframe = (Saveframe) iter.next();
            if (saveframe.getCategoryName().equals("conformer_family_coord_set")) {
                if (DEBUG) {
                    System.err.println("process conformers " + saveframe.getName());
                }
                processConformer(saveframe);
            }
        }
    }
    
    public void buildNOE() throws ParseException {
        Iterator iter = star3.getSaveFrames().values().iterator();
        while (iter.hasNext()) {
            Saveframe saveframe = (Saveframe) iter.next();
            if (saveframe.getCategoryName().equals("heteronucl_NOEs")) {
                if (DEBUG) {
                    System.err.println("process NOEs " + saveframe.getName());
                }
                processNOE(saveframe);
            }
        }
    }
    
    public void buildRelaxation(relaxTypes expType) throws ParseException {
        Iterator iter = star3.getSaveFrames().values().iterator();
        while (iter.hasNext()) {
            Saveframe saveframe = (Saveframe) iter.next();
            if (saveframe.getCategoryName().equals("heteronucl_" + expType + "_relaxation")) {
                if (DEBUG) {
                    System.err.println("process " + expType + " relaxation " + saveframe.getName());
                }
                processRelaxation(saveframe, expType);
            }
        }
    }

    public void buildDataset(Map tagMap, String datasetName) throws ParseException, IOException {
        String name = STAR3.getTokenFromMap(tagMap, "Name");
        String path = STAR3.getTokenFromMap(tagMap, "Directory_path");
        String type = STAR3.getTokenFromMap(tagMap, "Type");
        File file = new File(path, name);
        if (datasetName.equals("")) {
            datasetName = file.getAbsolutePath();
        }
        try {
            if (DEBUG) {
                System.err.println("open " + file.getAbsolutePath());
            }
            if (!file.exists()) {
                file = FileSystems.getDefault().getPath(starFile.getParentFile().getParent(), "datasets", file.getName()).toFile();
            }
            DatasetBase dataset = new DatasetBase(file.getAbsolutePath(), datasetName, false, false);
        } catch (IOException | IllegalArgumentException tclE) {
            System.err.println(tclE.getMessage());
        }
    }

    public void buildDihedralConstraints() throws ParseException {
        Iterator iter = star3.getSaveFrames().values().iterator();
        while (iter.hasNext()) {
            Saveframe saveframe = (Saveframe) iter.next();
            if (saveframe.getCategoryName().equals("torsion_angle_constraints")) {
                if (DEBUG) {
                    System.err.println("process torsion angle constraints " + saveframe.getName());
                }
                processDihedralConstraints(saveframe);
            }
        }
    }

    public void buildRDCConstraints() throws ParseException {
        Iterator iter = star3.getSaveFrames().values().iterator();
        while (iter.hasNext()) {
            Saveframe saveframe = (Saveframe) iter.next();
            if (saveframe.getCategoryName().equals("RDCs")) {
                if (DEBUG) {
                    System.err.println("process RDC constraints " + saveframe.getName());
                }
                processRDCConstraints(saveframe);
            }
        }
    }

    public void buildEntity(MoleculeBase molecule, Map tagMap, int index) throws ParseException {
        String entityAssemblyIDString = STAR3.getTokenFromMap(tagMap, "ID");
        String entityIDString = STAR3.getTokenFromMap(tagMap, "Entity_ID");
        String entitySaveFrameLabel = STAR3.getTokenFromMap(tagMap, "Entity_label").substring(1);
        String entityAssemblyName = STAR3.getTokenFromMap(tagMap, "Entity_assembly_name");
        String asymLabel = STAR3.getTokenFromMap(tagMap, "Asym_ID", entityAssemblyName);
        if (asymLabel.equals(".")) {
            asymLabel = String.valueOf((char) ('A' + index));
        }
        String pdbLabel = STAR3.getTokenFromMap(tagMap, "PDB_chain_ID", "A");
        if (pdbLabel.equals(".")) {
            pdbLabel = asymLabel;
        }
        int entityID = 1;
        int entityAssemblyID = 1;
        try {
            entityID = Integer.parseInt(entityIDString);
            entityAssemblyID = Integer.parseInt(entityAssemblyIDString);
        } catch (NumberFormatException nFE) {
            throw new ParseException(nFE.getMessage());
        }
        //int entityAssemblyID = Integer.parseInt(entityAssemblyIDString);
        // get entity saveframe
        String saveFrameName = "save_" + entitySaveFrameLabel;
        if (DEBUG) {
            System.err.println("process entity " + saveFrameName);
        }
        Saveframe saveframe = (Saveframe) star3.getSaveFrames().get(saveFrameName);
        if (saveframe != null) {

            String type = saveframe.getValue("_Entity", "Type");
            String name = saveframe.getValue("_Entity", "Name");
            String nomenclature = saveframe.getValue("_Entity", "Nomenclature", "");
            if (nomenclature.equals("")) {
                nomenclature = "IUPAC";
            }
            String cappedString = saveframe.getValue("_Entity", "Capped", "");
            boolean capped = true;
            if (cappedString.equalsIgnoreCase("no")) {
                capped = false;
            }
            if (type != null && type.equals("polymer")) {
                Entity entity = molecule.getEntity(entityAssemblyName);
                if (entity == null) {
                    Polymer polymer = new Polymer(entitySaveFrameLabel, entityAssemblyName);
                    polymer.setIDNum(entityID);
                    polymer.assemblyID = entityAssemblyID;
                    polymer.setPDBChain(pdbLabel);
                    entities.put(entityAssemblyIDString + "." + entityIDString, polymer);
                    molecule.addEntity(polymer, asymLabel, entityAssemblyID);
                    finishSaveFrameProcessing(polymer, saveframe, nomenclature, capped);
                } else {
                    molecule.addCoordSet(asymLabel, entityAssemblyID, entity);
                }
            } else {
                Entity entity = molecule.getEntity(name);
                if (entity == null) {
                    Compound compound = new Compound("1", entityAssemblyName, name);
                    compound.setIDNum(1);
                    compound.assemblyID = entityAssemblyID;
                    compound.setPDBChain(pdbLabel);
                    entities.put(entityAssemblyIDString + "." + entityIDString, compound);
                    molecule.addEntity(compound, asymLabel, entityAssemblyID);
                    String mapID = entityAssemblyID + "." + entityID + "." + 1;
                    finishSaveFrameProcessing(this, saveframe, compound, mapID);
                }
            }
        } else {
            System.out.println("Saveframe \"" + saveFrameName + "\" doesn't exist");
        }
    }

    public void buildExperiments() throws ParseException {
        Iterator iter = star3.getSaveFrames().values().iterator();
        while (iter.hasNext()) {
            Saveframe saveframe = (Saveframe) iter.next();
            if (saveframe.getCategoryName().equals("experiment_list")) {
                if (DEBUG) {
                    System.err.println("process experiments " + saveframe.getName());
                }
                int nExperiments = 0;
                try {
                    nExperiments = saveframe.loopCount("_Experiment_file");
                } catch (ParseException tclE) {
                    nExperiments = 0;
                }
                for (int i = 0; i < nExperiments; i++) {
                    Map map = saveframe.getLoopRowMap("_Experiment_file", i);
                    String datasetName;
                    try {
                        Map nameMap = saveframe.getLoopRowMap("_Experiment", i);
                        datasetName = STAR3.getTokenFromMap(nameMap, "Name");
                    } catch (ParseException tclE) {
                        datasetName = "";
                    }
                    try {
                        buildDataset(map, datasetName);
                    } catch (IOException ioE) {
                        throw new ParseException(ioE.getMessage());
                    }
                }
            }
        }
    }

    public void buildGenDistConstraints() throws ParseException {
        Iterator iter = star3.getSaveFrames().values().iterator();
        while (iter.hasNext()) {
            Saveframe saveframe = (Saveframe) iter.next();
            if (saveframe.getCategoryName().equals("general_distance_constraints")) {
                if (DEBUG) {
                    System.err.println("process general distance constraints " + saveframe.getName());
                }
                processGenDistConstraints(saveframe);
            }
        }
    }

    public void buildMolecule() throws ParseException {
        Iterator iter = star3.getSaveFrames().values().iterator();
        while (iter.hasNext()) {
            Saveframe saveframe = (Saveframe) iter.next();
            if (DEBUG) {
                System.err.println(saveframe.getCategoryName());
            }
            if (saveframe.getCategoryName().equals("assembly")) {
                if (DEBUG) {
                    System.err.println("process molecule >>" + saveframe.getName() + "<<");
                }
                String molName = saveframe.getValue("_Assembly", "Name");
                if (molName.equals("?")) {
                    molName = "noname";
                }
                molecule = MoleculeFactory.newMolecule(molName);
                int nEntities = saveframe.loopCount("_Entity_assembly");
                if (DEBUG) {
                    System.err.println("mol name " + molName + " nEntities " + nEntities);
                }
                for (int i = 0; i < nEntities; i++) {
                    Map map = saveframe.getLoopRowMap("_Entity_assembly", i);
                    buildEntity(molecule, map, i);
                }
                molecule.updateSpatialSets();
                molecule.genCoords(false);
                List<String> tags = saveframe.getTags("_Assembly");
                for (String tag : tags) {
                    if (tag.startsWith("NvJ_prop")) {
                        String propValue = saveframe.getValue("_Assembly", tag);
                        molecule.setProperty(tag.substring(9), propValue);
                    }
                }

            }
        }
    }

    public void buildPeakLists() throws ParseException {
        peakDimsWithoutResonance.clear();
        for (Saveframe saveframe : star3.getSaveFrames().values()) {
            if (saveframe.getCategoryName().equals("spectral_peak_list")) {
                if (DEBUG) {
                    System.err.println("process peaklists " + saveframe.getName());
                }
                processSTAR3PeakList(saveframe);
            }
        }
        addMissingResonances();
    }

    public void addMissingResonances() {
        ResonanceFactory resFactory = PeakList.resFactory();
        for (PeakDim peakDim : peakDimsWithoutResonance) {
            Resonance resonance = resFactory.build();
            resonance.add(peakDim);
        }
    }

    public void buildPeakPaths() throws ParseException {
        for (Saveframe saveframe : star3.getSaveFrames().values()) {
            if (saveframe.getCategoryName().equals("nmrfx_peak_path")) {
                if (DEBUG) {
                    System.err.println("process nmrfx_peak_path " + saveframe.getName());
                }
                PeakPathReader peakPathReader = new PeakPathReader();
                peakPathReader.processPeakPaths(saveframe);
            }
        }
    }

    public void buildResonanceLists() throws ParseException {
        var compoundMap = MoleculeBase.compoundMap();
        for (Saveframe saveframe : star3.getSaveFrames().values()) {
            if (saveframe.getCategoryName().equals("resonance_linker")) {
                hasResonances = true;
                if (DEBUG) {
                    System.err.println("process resonances " + saveframe.getName());
                }
                AtomResonance.processSTAR3ResonanceList(this, saveframe, compoundMap);
            }
        }
    }

    public void buildRunAbout() throws ParseException {
        Iterator iter = star3.getSaveFrames().values().iterator();
        while (iter.hasNext()) {
            Saveframe saveframe = (Saveframe) iter.next();
            if (saveframe.getCategoryName().startsWith("nmrview_")) {
                String toolName = saveframe.getCategoryName().substring(8);
                if (DEBUG) {
                    System.err.println("process tool " + saveframe.getName());
                }
                // interp.eval("::star3::setupTool " + toolName);
            }
        }
    }

    public Entity getEntity(String entityAssemblyIDString, String entityIDString) {
        return (Entity) entities.get(entityAssemblyIDString + "." + entityIDString);
    }

    public SpatialSetGroup getSpatialSet(List<String> entityAssemblyIDColumn, List<String> entityIDColumn, List<String> compIdxIDColumn, List<String> atomColumn, List<String> resonanceColumn, int i) throws ParseException {
        SpatialSetGroup spg = null;
        String iEntity = (String) entityIDColumn.get(i);
        String entityAssemblyID = (String) entityAssemblyIDColumn.get(i);
        var compoundMap = MoleculeBase.compoundMap();
        if (!iEntity.equals("?")) {
            String iRes = (String) compIdxIDColumn.get(i);
            String atomName = (String) atomColumn.get(i);
            Atom atom = null;
            if (entityAssemblyID.equals(".")) {
                entityAssemblyID = "1";
            }
            String mapID = entityAssemblyID + "." + iEntity + "." + iRes;
            Compound compound1 = compoundMap.get(mapID);
            if (compound1 != null) {
                //throw new ParseException("invalid compound in assignments saveframe \""+mapID+"\"");
                if ((atomName.charAt(0) == 'Q') || (atomName.charAt(0) == 'M')) {
                    Atom[] pseudoAtoms = ((Residue) compound1).getPseudo(atomName);
                    spg = new SpatialSetGroup(pseudoAtoms);
                } else {
                    atom = compound1.getAtomLoose(atomName);
                    if (atom != null) {
                        spg = new SpatialSetGroup(atom.spatialSet);
                    }
                }
                if (spg == null) {
                    System.out.println("invalid spatial set in assignments saveframe \"" + mapID + "." + atomName + "\"");
                }
            } else {
                System.err.println("invalid compound in assignments saveframe \"" + mapID + "\"");
            }
        }
        return spg;
    }

//    private void addResonance(long resID, PeakDim peakDim) {
//        List<PeakDim> peakDims = resMap.get(resID);
//        if (peakDims == null) {
//            peakDims = new ArrayList<>();
//            resMap.put(resID, peakDims);
//        }
//        peakDims.add(peakDim);
//    }
//
//    public void linkResonances() {
//        ResonanceFactory resFactory = PeakDim.resFactory();
//        for (Long resID : resMap.keySet()) {
//            List<PeakDim> peakDims = resMap.get(resID);
//            PeakDim firstPeakDim = peakDims.get(0);
//            Resonance resonance = resFactory.build(resID);
//            firstPeakDim.setResonance(resonance);
//            resonance.add(firstPeakDim);
//            if (peakDims.size() > 1) {
//                for (PeakDim peakDim : peakDims) {
//                    if (peakDim != firstPeakDim) {
//                        PeakList.linkPeakDims(firstPeakDim, peakDim);
//                    }
//                }
//            }
//        }
//    }
    public void processSTAR3PeakList(Saveframe saveframe) throws ParseException {
        ResonanceFactory resFactory = PeakList.resFactory();
        String listName = saveframe.getValue("_Spectral_peak_list", "Sf_framecode");
        String id = saveframe.getValue("_Spectral_peak_list", "ID");
        String sampleLabel = saveframe.getLabelValue("_Spectral_peak_list", "Sample_label");
        String sampleConditionLabel = saveframe.getOptionalLabelValue("_Spectral_peak_list", "Sample_condition_list_label");
        String datasetName = saveframe.getLabelValue("_Spectral_peak_list", "Experiment_name");
        String nDimString = saveframe.getValue("_Spectral_peak_list", "Number_of_spectral_dimensions");
        String dataFormat = saveframe.getOptionalValue("_Spectral_peak_list", "Text_data_format");
        String details = saveframe.getOptionalValue("_Spectral_peak_list", "Details");
        String slidable = saveframe.getOptionalValue("_Spectral_peak_list", "Slidable");
        String scaleStr = saveframe.getOptionalValue("_Spectral_peak_list", "Scale");

        if (dataFormat.equals("text")) {
            System.out.println("Aaaack, peak list is in text format, skipping list");
            System.out.println(details);
            return;
        }
        if (nDimString.equals("?")) {
            return;
        }
        if (nDimString.equals(".")) {
            return;
        }
        int nDim = NvUtil.toInt(nDimString);

        PeakList peakList = new PeakList(listName, nDim, NvUtil.toInt(id));

        int nSpectralDim = saveframe.loopCount("_Spectral_dim");
        if (nSpectralDim > nDim) {
            throw new IllegalArgumentException("Too many _Spectral_dim values " + listName + " " + nSpectralDim + " " + nDim);
        }

        peakList.setSampleLabel(sampleLabel);
        peakList.setSampleConditionLabel(sampleConditionLabel);
        peakList.setDatasetName(datasetName);
        peakList.setDetails(details);
        peakList.setSlideable(slidable.equals("yes"));
        if (scaleStr.length() > 0) {
            peakList.setScale(NvUtil.toDouble(scaleStr));
        }

        for (int i = 0; i < nSpectralDim; i++) {
            SpectralDim sDim = peakList.getSpectralDim(i);

            String value = null;
            value = saveframe.getValueIfPresent("_Spectral_dim", "Atom_type", i);
            if (value != null) {
                sDim.setAtomType(value);
            }
            value = saveframe.getValueIfPresent("_Spectral_dim", "Atom_isotope_number", i);
            if (value != null) {
                sDim.setAtomIsotopeValue(NvUtil.toInt(value));
            }
            value = saveframe.getValueIfPresent("_Spectral_dim", "Spectral_region", i);
            if (value != null) {
                sDim.setSpectralRegion(value);
            }
            value = saveframe.getValueIfPresent("_Spectral_dim", "Magnetization_linkage", i);
            if (value != null) {
                sDim.setMagLinkage(NvUtil.toInt(value) - 1);
            }
            value = saveframe.getValueIfPresent("_Spectral_dim", "Sweep_width", i);
            if (value != null) {
                sDim.setSw(NvUtil.toDouble(value));
            }
            value = saveframe.getValueIfPresent("_Spectral_dim", "Spectrometer_frequency", i);
            if (value != null) {
                sDim.setSf(NvUtil.toDouble(value));
            }
            value = saveframe.getValueIfPresent("_Spectral_dim", "Encoding_code", i);
            if (value != null) {
                sDim.setEncodingCode(value);
            }
            value = saveframe.getValueIfPresent("_Spectral_dim", "Encoded_source_dimension", i);
            if (value != null) {
                sDim.setEncodedSourceDim(NvUtil.toInt(value) - 1);
            }
            value = saveframe.getValueIfPresent("_Spectral_dim", "Dataset_dimension", i);
            if (value != null) {
                sDim.setDataDim(NvUtil.toInt(value) - 1);
            }
            value = saveframe.getValueIfPresent("_Spectral_dim", "Dimension_name", i);
            if (value != null) {
                sDim.setDimName(value);
            }
            value = saveframe.getValueIfPresent("_Spectral_dim", "ID_tolerance", i);
            if (value != null) {
                sDim.setIdTol(NvUtil.toDouble(value));
            }
            value = saveframe.getValueIfPresent("_Spectral_dim", "Pattern", i);
            if (value != null) {
                sDim.setPattern(value);
            }
            value = saveframe.getValueIfPresent("_Spectral_dim", "Relation", i);
            if (value != null) {
                sDim.setRelation(value);
            }
            value = saveframe.getValueIfPresent("_Spectral_dim", "Aliasing", i);
            if (value != null) {
                sDim.setAliasing(value);
            }
            value = saveframe.getValueIfPresent("_Spectral_dim", "Precision", i);
            if (value != null) {
                sDim.setPrecision(NvUtil.toInt(value));
            }
        }

        Loop loop = saveframe.getLoop("_Peak");
        if (loop != null) {
            List<String> idColumn = loop.getColumnAsList("ID");
            List<String> detailColumn = loop.getColumnAsListIfExists("Details");
            List<String> fomColumn = loop.getColumnAsListIfExists("Figure_of_merit");
            List<String> typeColumn = loop.getColumnAsListIfExists("Type");
            List<String> statusColumn = loop.getColumnAsListIfExists("Status");
            List<String> colorColumn = loop.getColumnAsListIfExists("Color");
            List<String> flagColumn = loop.getColumnAsListIfExists("Flag");
            List<String> cornerColumn = loop.getColumnAsListIfExists("Label_corner");

            for (int i = 0, n = idColumn.size(); i < n; i++) {
                int idNum = Integer.parseInt((String) idColumn.get(i));
                Peak peak = new Peak(peakList, nDim);
                peak.setIdNum(idNum);
                String value;
                if ((value = NvUtil.getColumnValue(fomColumn, i)) != null) {
                    float fom = NvUtil.toFloat(value);
                    peak.setFigureOfMerit(fom);
                }
                if ((value = NvUtil.getColumnValue(detailColumn, i)) != null) {
                    peak.setComment(value);
                }
                if ((value = NvUtil.getColumnValue(typeColumn, i)) != null) {
                    int type = Peak.getType(value);
                    peak.setType(type);
                }
                if ((value = NvUtil.getColumnValue(statusColumn, i)) != null) {
                    int status = NvUtil.toInt(value);
                    peak.setStatus(status);
                }
                if ((value = NvUtil.getColumnValue(colorColumn, i)) != null) {
                    value = value.equals(".") ? null : value;
                    peak.setColor(value);
                }
                if ((value = NvUtil.getColumnValue(flagColumn, i)) != null) {
                    for (int iFlag = 0; iFlag < Peak.NFLAGS; iFlag++) {
                        if (value.length() > iFlag) {
                            peak.setFlag(iFlag, (value.charAt(iFlag) == '1'));
                        } else {
                            peak.setFlag(iFlag, false);
                        }
                    }
                }
                if ((value = NvUtil.getColumnValue(cornerColumn, i)) != null) {
                    peak.setCorner(value);
                }
                peakList.addPeakWithoutResonance(peak);
            }

            loop = saveframe.getLoop("_Peak_general_char");
            if (loop != null) {
                List<String> peakidColumn = loop.getColumnAsList("Peak_ID");
                List<String> methodColumn = loop.getColumnAsList("Measurement_method");
                List<String> intensityColumn = loop.getColumnAsList("Intensity_val");
                List<String> errorColumn = loop.getColumnAsList("Intensity_val_err");
                for (int i = 0, n = peakidColumn.size(); i < n; i++) {
                    String value = null;
                    int idNum = 0;
                    if ((value = NvUtil.getColumnValue(peakidColumn, i)) != null) {
                        idNum = NvUtil.toInt(value);
                    } else {
                        //throw new TclException("Invalid peak id value at row \""+i+"\"");
                        continue;
                    }
                    Peak peak = peakList.getPeakByID(idNum);
                    String method = "height";
                    if ((value = NvUtil.getColumnValue(methodColumn, i)) != null) {
                        method = value;
                    }
                    if ((value = NvUtil.getColumnValue(intensityColumn, i)) != null) {
                        float iValue = NvUtil.toFloat(value);
                        if (method.equals("height")) {
                            peak.setIntensity(iValue);
                        } else if (method.equals("volume")) {
                            // FIXME should set volume/evolume 
                            peak.setVolume1(iValue);
                        } else {
                            // FIXME throw error if don't know type, or add new type dynamically?
                            peak.setIntensity(iValue);
                        }
                    }
                    if ((value = NvUtil.getColumnValue(errorColumn, i)) != null) {
                        if (!value.equals(".")) {
                            float iValue = NvUtil.toFloat(value);
                            if (method.equals("height")) {
                                peak.setIntensityErr(iValue);
                            } else if (method.equals("volume")) {
                                // FIXME should set volume/evolume 
                                peak.setVolume1Err(iValue);
                            } else {
                                // FIXME throw error if don't know type, or add new type dynamically?
                                peak.setIntensityErr(iValue);
                            }
                        }
                    }
                    // FIXME set error value
                }
            }

            loop = saveframe.getLoop("_Peak_char");
            if (loop == null) {
                throw new ParseException("No \"_Peak_char\" loop");
            }
            if (loop != null) {
                List<String> peakIdColumn = loop.getColumnAsList("Peak_ID");
                List<String> sdimColumn = loop.getColumnAsList("Spectral_dim_ID");
                String[] peakCharStrings = Peak.getSTAR3CharStrings();
                for (int j = 0; j < peakCharStrings.length; j++) {
                    String tag = peakCharStrings[j].substring(peakCharStrings[j].indexOf(".") + 1);
                    if (tag.equals("Sf_ID") || tag.equals("Entry_ID") || tag.equals("Spectral_peak_list_ID")) {
                        continue;
                    }
                    if (tag.equals("Resonance_ID") || tag.equals("Resonance_count")) {
                        continue;
                    }
                    List<String> column = loop.getColumnAsListIfExists(tag);
                    if (column != null) {
                        for (int i = 0, n = column.size(); i < n; i++) {
                            int idNum = Integer.parseInt((String) peakIdColumn.get(i));
                            int sDim = Integer.parseInt((String) sdimColumn.get(i)) - 1;
                            String value = (String) column.get(i);
                            if (!value.equals(".") && !value.equals("?")) {
                                Peak peak = peakList.getPeakByID(idNum);
                                PeakDim peakDim = peak.getPeakDim(sDim);
                                if (peakDim != null) {
                                    peakDim.setAttribute(tag, value);
                                }
                            }
                        }
                    }
                }
                loop = saveframe.getLoop("_Assigned_peak_chem_shift");

                if (loop != null) {
                    List<String> peakidColumn = loop.getColumnAsList("Peak_ID");
                    List<String> spectralDimColumn = loop.getColumnAsList("Spectral_dim_ID");
                    List<String> valColumn = loop.getColumnAsList("Val");
                    List<String> resonanceColumn = loop.getColumnAsList("Resonance_ID");
                    for (int i = 0, n = peakidColumn.size(); i < n; i++) {
                        String value = null;
                        int idNum = 0;
                        if ((value = NvUtil.getColumnValue(peakidColumn, i)) != null) {
                            idNum = NvUtil.toInt(value);
                        } else {
                            //throw new TclException("Invalid peak id value at row \""+i+"\"");
                            continue;
                        }
                        int sDim = 0;
                        long resonanceID = -1;
                        if ((value = NvUtil.getColumnValue(spectralDimColumn, i)) != null) {
                            sDim = NvUtil.toInt(value) - 1;
                        } else {
                            throw new ParseException("Invalid spectral dim value at row \"" + i + "\"");
                        }
                        if ((value = NvUtil.getColumnValue(valColumn, i)) != null) {
                            NvUtil.toFloat(value);  // fixme shouldn't we use this
                        }
                        if ((value = NvUtil.getColumnValue(resonanceColumn, i)) != null) {
                            resonanceID = NvUtil.toLong(value);
                        }
                        Peak peak = peakList.getPeakByID(idNum);
                        PeakDim peakDim = peak.getPeakDim(sDim);
                        if (resonanceID != -1L) {
                            Resonance resonance = resFactory.build(resonanceID);
                            resonance.add(peakDim);
                        } else {
                            peakDimsWithoutResonance.add(peakDim);
                        }
                    }
                } else {
                    System.out.println("No \"Assigned Peak Chem Shift\" loop");
                }
            }
            loop = saveframe.getLoop("_Peak_coupling");
            if (loop != null) {
                List<Integer> peakIdColumn = loop.getColumnAsIntegerList("Peak_ID", null);
                List<Integer> sdimColumn = loop.getColumnAsIntegerList("Spectral_dim_ID", null);
                List<Integer> compIDColumn = loop.getColumnAsIntegerList("Multiplet_component_ID", null);
                List<Double> couplingColumn = loop.getColumnAsDoubleList("Coupling_val", null);
                List<Double> strongCouplingColumn = loop.getColumnAsDoubleList("Strong_coupling_effect_val", null);
                List<Double> intensityColumn = loop.getColumnAsDoubleList("Intensity_val", null);
                List<String> couplingTypeColumn = loop.getColumnAsList("Type");
                int from = 0;
                int to = 0;
                for (int i = 0; i < peakIdColumn.size(); i++) {
                    int currentID = peakIdColumn.get(from);
                    int currentDim = sdimColumn.get(i) - 1;
                    if ((i == (peakIdColumn.size() - 1))
                            || (peakIdColumn.get(i + 1) != currentID)
                            || (sdimColumn.get(i + 1) - 1 != currentDim)) {
                        Peak peak = peakList.getPeakByID(currentID);
                        to = i + 1;
                        Multiplet multiplet = peak.getPeakDim(currentDim).getMultiplet();
                        CouplingPattern couplingPattern = new CouplingPattern(multiplet,
                                couplingColumn.subList(from, to),
                                couplingTypeColumn.subList(from, to),
                                strongCouplingColumn.subList(from, to),
                                intensityColumn.get(from)
                        );
                        multiplet.setCoupling(couplingPattern);
                        from = to;
                    }
                }
            }
            processTransitions(saveframe, peakList);
        }
    }

    void processTransitions(Saveframe saveframe, PeakList peakList) throws ParseException {
        Loop loop = saveframe.getLoop("_Spectral_transition");
        if (loop != null) {
            List<Integer> idColumn = loop.getColumnAsIntegerList("ID", null);
            List<Integer> peakIdColumn = loop.getColumnAsIntegerList("Peak_ID", null);
            List<Double> fomColumn = loop.getColumnAsDoubleList("Figure_of_merit", null);

            for (int i = 0, n = idColumn.size(); i < n; i++) {
                int idNum = idColumn.get(i);
                int peakIdNum = peakIdColumn.get(i);
                Peak peak = peakList.getPeakByID(peakIdNum);
                peak.setIdNum(idNum);
            }
            loop = saveframe.getLoop("_Spectral_transition_general_char");

            if (loop != null) {
                Map<Integer, Double> intMap = new HashMap<>();
                Map<Integer, Double> volMap = new HashMap<>();
                idColumn = loop.getColumnAsIntegerList("Spectral_transition_ID", null);
                peakIdColumn = loop.getColumnAsIntegerList("Peak_ID", null);
                List<Double> intensityColumn = loop.getColumnAsDoubleList("Intensity_val", null);
                List<String> methodColumn = loop.getColumnAsList("Measurement_method");

                for (int i = 0, n = idColumn.size(); i < n; i++) {
                    int idNum = idColumn.get(i);
                    Double value = intensityColumn.get(i);
                    if (value != null) {
                        String mode = methodColumn.get(i);
                        if (mode.equals("height")) {
                            intMap.put(idNum, value);
                        } else {
                            volMap.put(idNum, value);
                        }
                    }

                }

                loop = saveframe.getLoop("_Spectral_transition_char");
                if (loop != null) {
                    idColumn = loop.getColumnAsIntegerList("Spectral_transition_ID", null);
                    peakIdColumn = loop.getColumnAsIntegerList("Peak_ID", null);
                    List<Double> shiftColumn = loop.getColumnAsDoubleList("Chem_shift_val", null);
                    List<Double> lwColumn = loop.getColumnAsDoubleList("Line_width_val", null);
                    List<Integer> sdimColumn = loop.getColumnAsIntegerList("Spectral_dim_ID", null);

                    List<AbsMultipletComponent> comps = new ArrayList<>();
                    for (int i = 0; i < peakIdColumn.size(); i++) {
                        int currentID = peakIdColumn.get(i);
                        int transID = idColumn.get(i);
                        int currentDim = sdimColumn.get(i) - 1;
                        double sf = peakList.getSpectralDim(currentDim).getSf();
                        Peak peak = peakList.getPeakByID(currentID);
                        Multiplet multiplet = peak.getPeakDim(currentDim).getMultiplet();
                        AbsMultipletComponent comp = new AbsMultipletComponent(
                                multiplet, shiftColumn.get(i), intMap.get(transID), volMap.get(transID), lwColumn.get(i) / sf);
                        comps.add(comp);
                        if ((i == (peakIdColumn.size() - 1))
                                || (peakIdColumn.get(i + 1) != currentID)
                                || (sdimColumn.get(i + 1) - 1 != currentDim)) {
                            ComplexCoupling complexCoupling = new ComplexCoupling(multiplet, comps);
                            multiplet.setCoupling(complexCoupling);
                            comps.clear();
                        }
                    }
                }
            }
        }

    }

    public void processChemicalShifts(Saveframe saveframe, int ppmSet) throws ParseException {
        Loop loop = saveframe.getLoop("_Atom_chem_shift");
        if (loop != null) {
            var compoundMap = MoleculeBase.compoundMap();
            List<String> entityAssemblyIDColumn = loop.getColumnAsList("Entity_assembly_ID");
            List<String> entityIDColumn = loop.getColumnAsList("Entity_ID");
            List<String> compIdxIDColumn = loop.getColumnAsList("Comp_index_ID");
            List<String> atomColumn = loop.getColumnAsList("Atom_ID");
            List<String> typeColumn = loop.getColumnAsList("Atom_type");
            List<String> valColumn = loop.getColumnAsList("Val");
            List<String> valErrColumn = loop.getColumnAsList("Val_err");
            List<String> resColumn = loop.getColumnAsList("Resonance_ID");
            List<Integer> ambigColumn = loop.getColumnAsIntegerList("Ambiguity_code", -1);
            ResonanceFactory resFactory = PeakList.resFactory();
            for (int i = 0; i < entityAssemblyIDColumn.size(); i++) {
                String iEntity = (String) entityIDColumn.get(i);
                String entityAssemblyID = (String) entityAssemblyIDColumn.get(i);
                if (iEntity.equals("?")) {
                    continue;
                }
                String iRes = (String) compIdxIDColumn.get(i);
                String atomName = (String) atomColumn.get(i);
                String atomType = (String) typeColumn.get(i);
                String value = (String) valColumn.get(i);
                String valueErr = (String) valErrColumn.get(i);
                String resIDStr = ".";
                if (resColumn != null) {
                    resIDStr = (String) resColumn.get(i);
                }
                if (entityAssemblyID.equals(".")) {
                    entityAssemblyID = "1";
                }
                String mapID = entityAssemblyID + "." + iEntity + "." + iRes;
                Compound compound = compoundMap.get(mapID);
                if (compound == null) {
                    //throw new ParseException("invalid compound in assignments saveframe \""+mapID+"\"");
                    System.err.println("invalid compound in assignments saveframe \"" + mapID + "\"");
                    continue;
                }
                Atom atom = compound.getAtomLoose(atomName);
                if (atom == null) {
                    if (atomName.startsWith("H")) {
                        atom = compound.getAtom(atomName + "1");
                    }
                }
                if (atom == null) {
                    atom = Atom.genAtomWithElement(atomName, atomType);
                    compound.addAtom(atom);
                }
                if (atom == null) {
                    throw new ParseException("invalid atom in assignments saveframe \"" + mapID + "." + atomName + "\"");
                }
                SpatialSet spSet = atom.spatialSet;
                if (ppmSet < 0) {
                    ppmSet = 0;
                }
                int structureNum = ppmSet;
                if (spSet == null) {
                    throw new ParseException("invalid spatial set in assignments saveframe \"" + mapID + "." + atomName + "\"");
                }
                try {
                    spSet.setPPM(structureNum, Double.parseDouble(value), false);
                    spSet.getPPM(structureNum).setAmbigCode(ambigColumn.get(i));
                    if (!valueErr.equals(".")) {
                        spSet.setPPM(structureNum, Double.parseDouble(valueErr), true);
                    }
                } catch (NumberFormatException nFE) {
                    throw new ParseException("Invalid chemical shift value (not double) \"" + value + "\" error \"" + valueErr + "\"");
                }
                if (hasResonances && !resIDStr.equals(".")) {
                    long resID = Long.parseLong(resIDStr);
                    if (resID >= 0) {
                        AtomResonance resonance = (AtomResonance) resFactory.get(resID);
                        if (resonance == null) {
                            throw new ParseException("atom elem resonance " + resIDStr + ": invalid resonance");
                        }
//                    ResonanceSet resonanceSet = resonance.getResonanceSet();
//                    if (resonanceSet == null) {
//                        resonanceSet = new ResonanceSet(resonance);
//                    }
                        atom.setResonance(resonance);
                        resonance.setAtom(atom);
                    }
                }
            }
        }
    }

    public void processConformer(Saveframe saveframe) throws ParseException {
        Loop loop = saveframe.getLoop("_Atom_site");
        if (loop == null) {
            System.err.println("No \"_Atom_site\" loop");
            return;
        }
        var compoundMap = MoleculeBase.compoundMap();
        List<String> entityAssemblyIDColumn = loop.getColumnAsList("Label_entity_assembly_ID");
        List<String> entityIDColumn = loop.getColumnAsList("Label_entity_ID");
        List<String> compIdxIDColumn = loop.getColumnAsList("Label_comp_index_ID");
        List<String> atomColumn = loop.getColumnAsList("Label_atom_ID");
        List<String> xColumn = loop.getColumnAsList("Cartn_x");
        List<String> yColumn = loop.getColumnAsList("Cartn_y");
        List<String> zColumn = loop.getColumnAsList("Cartn_z");
        List<String> resColumn = loop.getColumnAsListIfExists("Resonance_ID");
        List<String> modelColumn = loop.getColumnAsList("Model_ID");
        TreeSet<Integer> selSet = new TreeSet<>();
        MoleculeBase molecule = null;
        int lastStructure = -1;
        for (int i = 0; i < entityAssemblyIDColumn.size(); i++) {
            String iEntity = (String) entityIDColumn.get(i);
            String entityAssemblyID = (String) entityAssemblyIDColumn.get(i);
            if (iEntity.equals("?")) {
                continue;
            }
            String iRes = (String) compIdxIDColumn.get(i);
            String atomName = (String) atomColumn.get(i);
            String xStr = (String) xColumn.get(i);
            String yStr = (String) yColumn.get(i);
            String zStr = (String) zColumn.get(i);
            String modelStr = (String) modelColumn.get(i);
            String resIDStr = ".";
            if (resColumn != null) {
                resIDStr = (String) resColumn.get(i);
            }
            if (entityAssemblyID.equals(".")) {
                entityAssemblyID = "1";
            }
            String mapID = entityAssemblyID + "." + iEntity + "." + iRes;
            Compound compound = compoundMap.get(mapID);
            if (compound == null) {
                //throw new ParseException("invalid compound in conformer saveframe \""+mapID+"\"");
                System.err.println("invalid compound in conformer saveframe \"" + mapID + "\"");
                continue;
            }
            if (molecule == null) {
                molecule = compound.molecule;
            }
            Atom atom = compound.getAtomLoose(atomName);
            if (atom == null) {
                System.err.println("No atom \"" + mapID + "." + atomName + "\"");
                continue;
                //throw new ParseException("invalid atom in conformer saveframe \""+mapID+"."+atomName+"\"");
            }
            int structureNumber = Integer.parseInt(modelStr);
            Integer intStructure = structureNumber;
            if (intStructure != lastStructure) {
                molecule.nullCoords(structureNumber);
                selSet.add(intStructure);
                molecule.structures.add(intStructure);
            }
            lastStructure = intStructure;
            double x = Double.parseDouble(xStr);
            double y = Double.parseDouble(yStr);
            double z = Double.parseDouble(zStr);
            String coordSetName = compound.molecule.getFirstCoordSet().getName();
            atom.setPointValidity(structureNumber, true);
            Point3 pt = new Point3(x, y, z);
            atom.setPoint(structureNumber, pt);
            //  atom.setOccupancy((float) atomParse.occupancy);
            //  atom.setBFactor((float) atomParse.bfactor);
        }
        if (molecule != null) {
            molecule.setActiveStructures(selSet);
            for (Integer iStructure : selSet) {
                molecule.genCoords(iStructure, true);
            }
        }
    }
    
    public void processNOE(Saveframe saveframe) throws ParseException {
        String frameName = saveframe.getCategory("_Heteronucl_NOE_list").get("Sf_framecode");
        String field = saveframe.getCategory("_Heteronucl_NOE_list").get("Spectrometer_frequency_1H");
        Map<String, String> extras = new HashMap<>();
        String refVal = saveframe.getCategory("_Heteronucl_NOE_list").get("ref_val");
        String refDescription = saveframe.getCategory("_Heteronucl_NOE_list").get("ref_description");
        extras.put("refVal", refVal);
        extras.put("refDescription", refDescription);
       
        MoleculeBase mol = MoleculeFactory.getActive();
        var compoundMap = MoleculeBase.compoundMap();
        Loop loop = saveframe.getLoop("_Heteronucl_NOE");
        if (loop == null) {
            System.err.println("No \"NOE\" loop");
            return;
        }
        List<String> entityAssemblyIDColumn = loop.getColumnAsList("Entity_assembly_ID_1");
        List<String> entityIDColumn = loop.getColumnAsList("Entity_ID_1");
        List<String> compIdxIDColumn = loop.getColumnAsList("Comp_index_ID_1");
        List<String> atomColumn = loop.getColumnAsList("Atom_ID_1");
        List<String> entityAssemblyID2Column = loop.getColumnAsList("Entity_assembly_ID_2");
        List<String> entityID2Column = loop.getColumnAsList("Entity_ID_2");
        List<String> compIdxID2Column = loop.getColumnAsList("Comp_index_ID_2");
        List<String> atom2Column = loop.getColumnAsList("Atom_ID_2");
        List<String> valColumn = loop.getColumnAsList("Val");
        List<String> errColumn = loop.getColumnAsList("Val_err");
                
        for (int i = 0; i < entityAssemblyIDColumn.size(); i++) {
            String iEntity = (String) entityIDColumn.get(i);
            String entityAssemblyID = (String) entityAssemblyIDColumn.get(i);
            if (iEntity.equals("?")) {
                continue;
            }
            String iRes = (String) compIdxIDColumn.get(i);
            String atomName = (String) atomColumn.get(i);
            String iEntity2 = (String) entityID2Column.get(i);
            String entityAssemblyID2 = (String) entityAssemblyID2Column.get(i);
            if (iEntity2.equals("?")) {
                continue;
            }
            String iRes2 = (String) compIdxID2Column.get(i);
            String atomName2 = (String) atom2Column.get(i);
            Double value = 0.0;
            Double error = 0.0;
            if (!valColumn.get(i).equals(".")) {
                value = Double.parseDouble(valColumn.get(i));
            }
            if (!errColumn.get(i).equals(".")) {
                error = Double.parseDouble(errColumn.get(i));
            }
            
            double temperature = 25.0;
            
            if (entityAssemblyID.equals(".")) {
                entityAssemblyID = "1";
            }
            String mapID = entityAssemblyID + "." + iEntity + "." + iRes;
            Compound compound = compoundMap.get(mapID);
            if (compound == null) {
                //throw new ParseException("invalid compound in conformer saveframe \""+mapID+"\"");
                System.err.println("invalid compound in NOE saveframe \"" + mapID + "\"");
                continue;
            }
            if (mol == null) {
                mol = compound.molecule;
            }            
            Atom atom = compound.getAtomLoose(atomName);
            if (atom == null) {
                System.err.println("No atom \"" + mapID + "." + atomName + "\"");
                continue;
                //throw new ParseException("invalid atom in conformer saveframe \""+mapID+"."+atomName+"\"");
            }
            
            if (entityAssemblyID2.equals(".")) {
                entityAssemblyID2 = "1";
            }
            String mapID2 = entityAssemblyID2 + "." + iEntity2 + "." + iRes2;
    
            Atom atom2 = compound.getAtomLoose(atomName2);
            if (atom2 == null) {
                System.err.println("No atom \"" + mapID2 + "." + atomName2 + "\"");
                continue;
                //throw new ParseException("invalid atom in conformer saveframe \""+mapID+"."+atomName+"\"");
            }
            
            NOEData noeData = new NOEData(frameName, atom2, Double.parseDouble(field), temperature, value, error, extras);
//            System.out.println("reader " + noeData);
            atom.noeData.put(frameName, noeData);
            NOEData noeData2 = new NOEData(frameName, atom, Double.parseDouble(field), temperature, value, error, extras);
            atom2.noeData.put(frameName, noeData2);
        }
    }
    
    public void processRelaxation(Saveframe saveframe, relaxTypes expType) throws ParseException {
        String frameName = saveframe.getCategory("_Heteronucl_" + expType + "_list").get("Sf_framecode");
        String field = saveframe.getCategory("_Heteronucl_" + expType + "_list").get("Spectrometer_frequency_1H");
        String coherenceType = saveframe.getCategory("_Heteronucl_" + expType + "_list").get(expType + "_coherence_type");
        String units = saveframe.getCategory("_Heteronucl_" + expType + "_list").get(expType + "_val_units");
        Map<String, String> extras = new HashMap<>();
        extras.put("coherenceType", coherenceType);
        extras.put("units", units);
       
        MoleculeBase mol = MoleculeFactory.getActive();
        var compoundMap = MoleculeBase.compoundMap();
        Loop loop = saveframe.getLoop("_" + expType);
        if (loop == null) {
            System.err.println("No \"" + expType + "\" loop");
            return;
        }
        List<String> entityAssemblyIDColumn = loop.getColumnAsList("Entity_assembly_ID");
        List<String> entityIDColumn = loop.getColumnAsList("Entity_ID");
        List<String> compIdxIDColumn = loop.getColumnAsList("Comp_index_ID");
        List<String> atomColumn = loop.getColumnAsList("Atom_ID");
        List<String> valColumn = loop.getColumnAsListIfExists("Val");
        List<String> errColumn = loop.getColumnAsListIfExists("Val_err");
        List<String> RexValColumn = loop.getColumnAsListIfExists("Rex_val");
        List<String> RexErrColumn = loop.getColumnAsListIfExists("Rex_err");
        if (expType.equals(relaxTypes.T2) || expType.equals(relaxTypes.T1RHO)) {
            valColumn = loop.getColumnAsList(expType + "_val");
            errColumn = loop.getColumnAsList(expType + "_val_err");
        } 
                
        for (int i = 0; i < entityAssemblyIDColumn.size(); i++) {
            String iEntity = (String) entityIDColumn.get(i);
            String entityAssemblyID = (String) entityAssemblyIDColumn.get(i);
            if (iEntity.equals("?")) {
                continue;
            }
            String iRes = (String) compIdxIDColumn.get(i);
            String atomName = (String) atomColumn.get(i);
            Map<String, Double> values = new LinkedHashMap<>();
            Map<String, Double> errors = new LinkedHashMap<>();
            values.put(expType.getName(), null);
            errors.put(expType.getName(), null);
            if (!valColumn.get(i).equals(".")) {
                values.replace(expType.getName(), Double.parseDouble(valColumn.get(i)));
            }
            if (!errColumn.get(i).equals(".")) {
                errors.replace(expType.getName(), Double.parseDouble(errColumn.get(i)));
            }
            if (expType.equals(relaxTypes.T2)) {
                values.put("Rex", null);
                errors.put("Rex", null);
                if (!RexValColumn.get(i).equals(".")) {
                    values.replace("Rex", Double.parseDouble(RexValColumn.get(i)));
                }
                if (!RexErrColumn.get(i).equals(".")) {
                    errors.replace("Rex", Double.parseDouble(RexErrColumn.get(i)));
                }
            }
            double temperature = 25.0;
            
            if (entityAssemblyID.equals(".")) {
                entityAssemblyID = "1";
            }
            String mapID = entityAssemblyID + "." + iEntity + "." + iRes;
            Compound compound = compoundMap.get(mapID);
            if (compound == null) {
                //throw new ParseException("invalid compound in conformer saveframe \""+mapID+"\"");
                System.err.println("invalid compound in " + expType + " saveframe \"" + mapID + "\"");
                continue;
            }
            if (mol == null) {
                mol = compound.molecule;
            }
            Atom atom = compound.getAtomLoose(atomName);
            if (atom == null) {
                System.err.println("No atom \"" + mapID + "." + atomName + "\"");
                continue;
                //throw new ParseException("invalid atom in conformer saveframe \""+mapID+"."+atomName+"\"");
            }
            
            RelaxationData relaxData = new RelaxationData(frameName, expType, Double.parseDouble(field), temperature, values, errors, extras);
//            System.out.println("reader " + relaxData);
            atom.relaxData.put(frameName, relaxData);
//            System.out.println("reader atom.relaxData = " + atom + " " + atom.relaxData);
        }
    }

    public void processDihedralConstraints(Saveframe saveframe) throws ParseException {
        Loop loop = saveframe.getLoop("_Torsion_angle_constraint");
        if (loop == null) {
            throw new ParseException("No \"_Torsion_angle_constraint\" loop");
        }
        List<String>[] entityAssemblyIDColumns = new ArrayList[4];
        List<String>[] entityIDColumns = new ArrayList[4];
        List<String>[] compIdxIDColumns = new ArrayList[4];
        List<String>[] atomColumns = new ArrayList[4];
        List<String>[] resonanceColumns = new ArrayList[4];
        for (int i = 1; i <= 4; i++) {
            entityAssemblyIDColumns[i - 1] = loop.getColumnAsList("Entity_assembly_ID_" + i);
            entityIDColumns[i - 1] = loop.getColumnAsList("Entity_ID_" + i);
            compIdxIDColumns[i - 1] = loop.getColumnAsList("Comp_index_ID_" + i);
            atomColumns[i - 1] = loop.getColumnAsList("Atom_ID_" + i);
            resonanceColumns[i - 1] = loop.getColumnAsList("Resonance_ID_" + i);
        }
        List<String> angleNameColumn = loop.getColumnAsList("Torsion_angle_name");
        List<String> lowerColumn = loop.getColumnAsList("Angle_lower_bound_val");
        List<String> upperColumn = loop.getColumnAsList("Angle_upper_bound_val");
        AngleConstraintSet angleSet = molecule.getMolecularConstraints().
                newAngleSet(saveframe.getName().substring(5));
        for (int i = 0; i < entityAssemblyIDColumns[0].size(); i++) {
            Atom[] atoms = new Atom[4];
            for (int iAtom = 0; iAtom < 4; iAtom++) {
                atoms[iAtom] = getSpatialSet(entityAssemblyIDColumns[iAtom],
                        entityIDColumns[iAtom], compIdxIDColumns[iAtom],
                        atomColumns[iAtom], resonanceColumns[iAtom], i).
                        getFirstSet().atom;
            }
            String upperValue = (String) upperColumn.get(i);
            String lowerValue = (String) lowerColumn.get(i);
            String name = (String) angleNameColumn.get(i);
            double upper = Double.parseDouble(upperValue);
            double lower = 1.8;
            if (!lowerValue.equals(".")) {
                lower = Double.parseDouble(lowerValue);
            }
            try {
                AngleConstraint aCon = new AngleConstraint(atoms, lower, upper, name);
                angleSet.add(aCon);
            } catch (InvalidMoleculeException ex) {
                throw new ParseException(ex.getLocalizedMessage());
            }
        }
    }

    public void processRDCConstraints(Saveframe saveframe) throws ParseException {
        Loop loop = saveframe.getLoop("_RDC");
        if (loop == null) {
            throw new ParseException("No \"_RDC\" loop");
        }
        //saveframe.getTagsIgnoreMissing(tagCategory);
        List<String>[] entityAssemblyIDColumns = new ArrayList[2];
        List<String>[] entityIDColumns = new ArrayList[2];
        List<String>[] compIdxIDColumns = new ArrayList[2];
        List<String>[] atomColumns = new ArrayList[2];
        List<String>[] resonanceColumns = new ArrayList[2];
        for (int i = 1; i <= 2; i++) {
            entityAssemblyIDColumns[i - 1] = loop.getColumnAsList("Entity_assembly_ID_" + i);
            entityIDColumns[i - 1] = loop.getColumnAsList("Entity_ID_" + i);
            compIdxIDColumns[i - 1] = loop.getColumnAsList("Comp_index_ID_" + i);
            atomColumns[i - 1] = loop.getColumnAsList("Atom_ID_" + i);
            resonanceColumns[i - 1] = loop.getColumnAsList("Resonance_ID_" + i);
        }
        List<Double> valColumn = loop.getColumnAsDoubleList("Val", null);
        List<Double> errColumn = loop.getColumnAsDoubleList("Val_err", null);
        List<Double> lengthColumn = loop.getColumnAsDoubleList("Val_bond_length", null);
        RDCConstraintSet rdcSet = molecule.getMolecularConstraints().newRDCSet(saveframe.getName().substring(5));
        for (int i = 0; i < entityAssemblyIDColumns[0].size(); i++) {
            SpatialSet[] spSets = new SpatialSet[4];
            for (int iAtom = 0; iAtom < 2; iAtom++) {
                SpatialSetGroup spG = getSpatialSet(entityAssemblyIDColumns[iAtom], entityIDColumns[iAtom], compIdxIDColumns[iAtom], atomColumns[iAtom], resonanceColumns[iAtom], i);
                if (spG != null) {
                    spSets[iAtom] = spG.getFirstSet();
                    if (errColumn.get(i) != null) {
                        RDC aCon = new RDC(rdcSet, spSets[0], spSets[1], valColumn.get(i), errColumn.get(i));
                        rdcSet.add(aCon);
                    }
                }
            }
        }
    }

    public void processGenDistConstraints(Saveframe saveframe) throws ParseException {
        Loop loop = saveframe.getLoop("_Gen_dist_constraint");
        if (loop == null) {
            throw new ParseException("No \"_Gen_dist_constraint\" loop");
        }
        var compoundMap = MoleculeBase.compoundMap();
        List<String>[] entityAssemblyIDColumns = new ArrayList[2];
        List<String>[] entityIDColumns = new ArrayList[2];
        List<String>[] compIdxIDColumns = new ArrayList[2];
        List<String>[] atomColumns = new ArrayList[2];
        List<String>[] resonanceColumns = new ArrayList[2];
        entityAssemblyIDColumns[0] = loop.getColumnAsList("Entity_assembly_ID_1");
        entityIDColumns[0] = loop.getColumnAsList("Entity_ID_1");
        compIdxIDColumns[0] = loop.getColumnAsList("Comp_index_ID_1");
        atomColumns[0] = loop.getColumnAsList("Atom_ID_1");
        resonanceColumns[0] = loop.getColumnAsList("Resonance_ID_1");
        entityAssemblyIDColumns[1] = loop.getColumnAsList("Entity_assembly_ID_2");
        entityIDColumns[1] = loop.getColumnAsList("Entity_ID_2");
        compIdxIDColumns[1] = loop.getColumnAsList("Comp_index_ID_2");
        atomColumns[1] = loop.getColumnAsList("Atom_ID_2");
        resonanceColumns[0] = loop.getColumnAsList("Resonance_ID_2");
        List<String> constraintIDColumn = loop.getColumnAsList("ID");
        List<String> lowerColumn = loop.getColumnAsList("Distance_lower_bound_val");
        List<String> upperColumn = loop.getColumnAsList("Distance_upper_bound_val");
        List<String> peakListIDColumn = loop.getColumnAsList("Spectral_peak_list_ID");
        List<String> peakIDColumn = loop.getColumnAsList("Spectral_peak_ID");
        Atom[] atoms = new Atom[2];
        SpatialSetGroup[] spSets = new SpatialSetGroup[2];
        String[] resIDStr = new String[2];
        PeakList peakList = null;
        String lastPeakListIDStr = "";
        NoeSet noeSet = molecule.getMolecularConstraints().newNOESet(saveframe.getName().substring(5));

        for (int i = 0; i < entityAssemblyIDColumns[0].size(); i++) {
            boolean okAtoms = true;
            for (int iAtom = 0; iAtom < 2; iAtom++) {
                spSets[iAtom] = null;
                String iEntity = (String) entityIDColumns[iAtom].get(i);
                String entityAssemblyID = (String) entityAssemblyIDColumns[iAtom].get(i);
                if (iEntity.equals("?")) {
                    continue;
                }
                String iRes = (String) compIdxIDColumns[iAtom].get(i);
                String atomName = (String) atomColumns[iAtom].get(i);
                resIDStr[iAtom] = ".";
                if (resonanceColumns[iAtom] != null) {
                    resIDStr[iAtom] = (String) resonanceColumns[iAtom].get(i);
                }
                if (entityAssemblyID.equals(".")) {
                    entityAssemblyID = "1";
                }
                String mapID = entityAssemblyID + "." + iEntity + "." + iRes;
                Compound compound1 = compoundMap.get(mapID);
                if (compound1 == null) {
                    //throw new ParseException("invalid compound in distance constraints saveframe \""+mapID+"\"");
                    System.err.println("invalid compound in distance constraints saveframe \"" + mapID + "\"");
                } else if ((atomName.charAt(0) == 'Q') || (atomName.charAt(0) == 'M')) {
                    Residue residue = (Residue) compound1;
                    Atom[] pseudoAtoms = ((Residue) compound1).getPseudo(atomName);
                    if (pseudoAtoms == null) {
                        System.err.println(residue.getIDNum() + " " + residue.getNumber() + " " + residue.getName());
                        System.err.println("invalid pseudo in distance constraints saveframe \"" + mapID + "\" " + atomName);
                        okAtoms = false;
                    } else {
                        spSets[iAtom] = new SpatialSetGroup(pseudoAtoms);
                    }
                } else {
                    atoms[iAtom] = compound1.getAtomLoose(atomName);
                    if (atoms[iAtom] == null) {
                        throw new ParseException("invalid atom in distance constraints saveframe \"" + mapID + "." + atomName + "\"");
                    }
                    spSets[iAtom] = new SpatialSetGroup(atoms[iAtom].spatialSet);
                }
                if (spSets[iAtom] == null) {
                    throw new ParseException("invalid spatial set in distance constraints saveframe \"" + mapID + "." + atomName + "\"");
                }
            }
            String upperValue = (String) upperColumn.get(i);
            String lowerValue = (String) lowerColumn.get(i);
            String peakListIDStr = (String) peakListIDColumn.get(i);
            String peakID = (String) peakIDColumn.get(i);
            String constraintID = (String) constraintIDColumn.get(i);
            if (!peakListIDStr.equals(lastPeakListIDStr)) {
                if (peakListIDStr.equals(".")) {
                    if (peakList == null) {
                        peakList = new PeakList("gendist", 2);
                    }
                } else {
                    try {
                        int peakListID = Integer.parseInt(peakListIDStr);
                        Optional<PeakList> peakListOpt = PeakList.get(peakListID);
                        if (peakListOpt.isPresent()) {
                            peakList = peakListOpt.get();
                        }
                    } catch (NumberFormatException nFE) {
                        throw new ParseException("Invalid peak list id (not int) \"" + peakListIDStr + "\"");
                    }
                }
            }
            lastPeakListIDStr = peakListIDStr;
            Peak peak = null;
            if (peakList != null) {
                if (peakID.equals(".")) {
                    peakID = constraintID;
                    int idNum = Integer.parseInt(peakID);
                    while ((peak = peakList.getPeak(idNum)) == null) {
                        peakList.addPeak();
                    }
                } else {
                    int idNum = Integer.parseInt(peakID);
                    peak = peakList.getPeakByID(idNum);
                }
                Noe noe = new Noe(peak, spSets[0], spSets[1], 1.0);
                double upper = 1000000.0;
                if (upperValue.equals(".")) {
                    System.err.println("Upper value is a \".\" at line " + i);
                } else {
                    upper = Double.parseDouble(upperValue);
                }
                noe.setUpper(upper);
                double lower = 1.8;
                if (!lowerValue.equals(".")) {
                    lower = Double.parseDouble(lowerValue);
                }
                noe.setLower(lower);
                noe.setPpmError(1.0);
                noe.setIntensity(Math.pow(upper, -6.0) * 10000.0);
                noe.setVolume(Math.pow(upper, -6.0) * 10000.0);
                noeSet.add(noe);
            }
        }
        //  noeSet.updateNPossible(null);
        noeSet.setCalibratable(false);
    }

    public void process() throws ParseException, IllegalArgumentException {
        String[] argv = {};
        process(argv);
    }

    public void process(String[] argv) throws ParseException, IllegalArgumentException {
        if ((argv.length != 0) && (argv.length != 3)) {
            throw new IllegalArgumentException("?shifts fromSet toSet?");
        }
        if (DEBUG) {
            System.out.println("nSave " + star3.getSaveFrameNames());
        }
        ResonanceFactory resFactory = PeakList.resFactory();
        if (argv.length == 0) {
            hasResonances = false;
            var compoundMap = MoleculeBase.compoundMap();
            compoundMap.clear();
            buildExperiments();
            if (DEBUG) {
                System.err.println("process molecule");
            }
            buildMolecule();
            if (DEBUG) {
                System.err.println("process peak lists");
            }
            buildPeakLists();
            if (DEBUG) {
                System.err.println("process resonance lists");
            }
            buildResonanceLists();
            if (DEBUG) {
                System.err.println("process chem shifts");
            }
            buildChemShifts(-1, 0);
            if (DEBUG) {
                System.err.println("process conformers");
            }
            buildConformers();
            if (DEBUG) {
                System.err.println("process dist constraints");
            }
            buildGenDistConstraints();
            if (DEBUG) {
                System.err.println("process angle constraints");
            }
            buildDihedralConstraints();
            if (DEBUG) {
                System.err.println("process rdc constraints");
            }
            buildRDCConstraints();
            if (DEBUG) {
                System.err.println("process NOE");
            }
            buildNOE();
            if (DEBUG) {
                System.err.println("process T1");
            }
            buildRelaxation(relaxTypes.T1);
            if (DEBUG) {
                System.err.println("process T1rho");
            }
            buildRelaxation(relaxTypes.T1RHO);
            if (DEBUG) {
                System.err.println("process T2");
            }
            buildRelaxation(relaxTypes.T2);
            if (DEBUG) {
                System.err.println("process runabout");
            }
            buildRunAbout();
            if (DEBUG) {
                System.err.println("process paths");
            }
            buildPeakPaths();
            if (DEBUG) {
                System.err.println("clean resonances");
            }
            resFactory.clean();
            if (DEBUG) {
                System.err.println("process done");
            }
        } else if ("shifts".startsWith(argv[2].toString())) {
            int fromSet = Integer.parseInt(argv[3]);
            int toSet = Integer.parseInt(argv[4]);
            buildChemShifts(fromSet, toSet);
        }
    }
}
