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
package org.nmrfx.processor.datasets.vendor;

import com.nanalysis.jcamp.model.JCampBlock;
import com.nanalysis.jcamp.model.JCampDocument;
import com.nanalysis.jcamp.model.JCampRecord;
import com.nanalysis.jcamp.parser.JCampParser;
import com.nanalysis.jcamp.util.JCampUtil;
import org.apache.commons.math3.complex.Complex;
import org.nmrfx.processor.datasets.DatasetType;
import org.nmrfx.processor.datasets.parameters.FPMult;
import org.nmrfx.processor.datasets.parameters.GaussianWt;
import org.nmrfx.processor.datasets.parameters.LPParams;
import org.nmrfx.processor.datasets.parameters.SinebellWt;
import org.nmrfx.processor.math.Vec;
import org.nmrfx.processor.processing.SampleSchedule;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.nanalysis.jcamp.model.Label.*;

class JCAMPData implements NMRData {
    private static final List<String> MATCHING_EXTENSIONS = List.of(".jdx", ".dx");

    private final String path;
    private final JCampDocument document;
    private final JCampBlock block;

    private DatasetType preferredDatasetType = DatasetType.NMRFX;
    private SampleSchedule sampleSchedule = null;


    public JCAMPData(String path) throws IOException {
        this.path = path;
        this.document = new JCampParser().parse(new File(path));

        if (document.getBlockCount() == 0) {
            throw new IOException("Invalid JCamp document, doesn't contain any block.");
        }
        this.block = document.blocks().findFirst()
                .orElseThrow(() -> new IOException("Invalid JCamp document, doesn't contain any block."));

        //realPages = block.getPagesForYSymbol("R");
        //imaginaryPages = block.getPagesForYSymbol("I");

    }

    @Override
    public void close() {
        // Nothing to close
    }

    @Override
    public String getFilePath() {
        return path;
    }

    @Override
    public String getPar(String parname) {
        return block.get(parname).getString();
    }

    @Override
    public Double getParDouble(String parname) {
        return block.get(parname).getDouble();
    }

    @Override
    public Integer getParInt(String parname) {
        return block.get(parname).getInt();
    }

    @Override
    public List<VendorPar> getPars() {
        Set<String> defined = new HashSet<>();
        List<VendorPar> vendorPars = new ArrayList<>();

        // get block-level records first
        for (String key : block.allRecordKeys()) {
            if (defined.add(key)) {
                vendorPars.add(new VendorPar(key, block.get(key).getString()));
            }
        }

        // then add document-level records if they were not defined by the block
        for (String key : document.allRecordKeys()) {
            if (defined.add(key)) {
                vendorPars.add(new VendorPar(key, document.get(key).getString()));
            }
        }

        return vendorPars;
    }

    @Override
    public int getNVectors() {
        //XXX To confirm, looks like it is number of 1D rows in total
        // so 1 if 1D data, and number of 2D if 2D data.
        // no more than 2D data are supported in JCamp
        //XXX should we try to get it from page content instead?
        return block.optional($TD, 1)
                .map(r -> r.getInt() / 2)
                .orElse(1);
    }

    @Override
    public int getNPoints() {
        // XXX always /2, or only if complex data?
        // XXX should we try to get it from page content instead?
        return block.optional($TD, 0)
                .map(r -> r.getInt() / 2)
                .orElse(0);
    }

    @Override
    public int getNDim() {
        return block.getOrDefault(NUMDIM, "1").getInt();
    }

    @Override
    public int getSize(int dim) {
        // XXX should we try to get it from page content instead?
        return block.optional($TD, 0)
                .map(r -> r.getInt() / 2)
                .orElse(0);
    }

    @Override
    public void setSize(int dim, int size) {
        throw new UnsupportedOperationException("This looks like this shouldn't be called.");
    }

    @Override
    public String getSolvent() {
        //TODO implement me
        return null;
    }

    @Override
    public double getTempK() {
        //TODO implement me
        return 0;
    }

    @Override
    public String getSequence() {
        //TODO implement me
        return null;
    }

    @Override
    public double getSF(int dim) {
        //XXX some doubts about the expected unit: Hz or MHz?
        //XXX Base freq, or observed freq? should we try to add offset?
        //Previous implementation was using OBSERVE_FREQUENCY but this is not defined in 2D
        if(dim == 0) {
            return block.optional($BF1, $BFREQ, $SF)
                    .map(r -> r.getDouble() * 1E6)
                    .orElseThrow(() -> new IllegalStateException("Unknown frequency, $BF1, $BFREQ and $SF undefined!"));
        } else if(dim == 1) {
            return block.optional($BF2)
                    .map(r -> r.getDouble() * 1e6)
                    .orElseThrow(() -> new IllegalStateException("Unknown frequency, $BF2 undefined!"));
        } else {
            throw new UnsupportedOperationException("Unsupported dimension " + dim + " in JCamp");
        }
    }

    @Override
    public void setSF(int dim, double value) {
        throw new UnsupportedOperationException("This looks like this shouldn't be called.");
    }

    @Override
    public void resetSF(int dim) {
        //TODO implement me
    }

    @Override
    public double getSW(int dim) {
        //TODO implement me
        return 0;
    }

    @Override
    public void setSW(int dim, double value) {
        //TODO implement me
    }

    @Override
    public void resetSW(int dim) {
        //TODO implement me
    }

    @Override
    public double getRef(int dim) {
        //TODO implement me
        return 0;
    }

    @Override
    public void setRef(int dim, double ref) {
        //TODO implement me
    }

    @Override
    public void resetRef(int dim) {
        //TODO implement me
    }

    @Override
    public double getRefPoint(int dim) {
        //TODO implement me
        return 0;
    }

    @Override
    public String getTN(int dim) {
        if(dim == 0) {
            return block.optional(_OBSERVE_NUCLEUS, $NUC_1, $T2_NUCLEUS)
                    .map(JCampRecord::getString)
                    .map(JCampUtil::toNucleusName)
                    .orElse(NMRDataUtil.guessNucleusFromFreq(getSF(dim)).toString());
        } else if(dim == 1) {
            return block.optional($NUC_2)
                    .map(JCampRecord::getString)
                    .map(JCampUtil::toNucleusName)
                    .orElse(NMRDataUtil.guessNucleusFromFreq(getSF(dim)).toString());
        } else {
            throw new UnsupportedOperationException("Unsupported dimension " + dim + " in JCamp");
        }
    }

    @Override
    public boolean isComplex(int dim) {
        //TODO implement me
        return false;
    }

    @Override
    public String getFTType(int dim) {
        //TODO implement me
        return null;
    }

    @Override
    public double[] getCoefs(int dim) {
        //TODO implement me
        return new double[0];
    }

    @Override
    public String getSymbolicCoefs(int dim) {
        //TODO implement me
        return null;
    }

    @Override
    public String getVendor() {
        //XXX some code expected "bruker" in lowercase
        return block.optional(ORIGIN)
                .map(JCampRecord::getString)
                .orElse("JCamp");
    }

    @Override
    public double getPH0(int dim) {
        //TODO implement me
        return 0;
    }

    @Override
    public double getPH1(int dim) {
        //TODO implement me
        return 0;
    }

    @Override
    public int getLeftShift(int dim) {
        //TODO implement me
        return 0;
    }

    @Override
    public double getExpd(int dim) {
        //TODO implement me

        return 0;
    }

    @Override
    public SinebellWt getSinebellWt(int dim) {
        //TODO implement me
        return null;
    }

    @Override
    public GaussianWt getGaussianWt(int dim) {
        //TODO implement me
        return null;
    }

    @Override
    public FPMult getFPMult(int dim) {
        //TODO implement me
        return null;
    }

    @Override
    public LPParams getLPParams(int dim) {
        //TODO implement me
        return null;
    }

    @Override
    public String[] getSFNames() {
        //XXX check whether this is useful
        //RS2DData returns an array of empty strings
        //Original JCAMPData returned OBSERVEFREQUENCY,dim => doesn't work for 2D files
        String[] names = new String[getNDim()];
        Arrays.fill(names, "");
        return names;
    }

    @Override
    public String[] getSWNames() {
        //XXX check whether this is useful
        // original JCAMPData returned an array of empty strings
        String[] names = new String[getNDim()];
        Arrays.fill(names, "");
        return names;
    }

    @Override
    public String[] getLabelNames() {
        int nDim = getNDim();

        List<String> names = new ArrayList<>();
        for (int i = 0; i < nDim; i++) {
            String name = getTN(i);
            if (names.contains(name)) {
                name = name + "_" + (i + 1);
            }
            names.add(name);
        }
        return names.toArray(new String[0]);
    }

    @Override
    public void readVector(int iVec, Vec dvec) {
        //TODO implement me
    }

    @Override
    public void readVector(int iVec, Complex[] cdata) {
        //TODO implement me
    }

    @Override
    public void readVector(int iVec, double[] rdata, double[] idata) {
        //TODO implement me
    }

    @Override
    public void readVector(int iVec, double[] data) {
        //TODO implement me
    }

    @Override
    public void readVector(int iDim, int iVec, Vec dvec) {
        //TODO implement me
    }

    @Override
    public void resetAcqOrder() {
        //TODO implement me
    }

    @Override
    public String[] getAcqOrder() {
        //TODO implement me
        return new String[0];
    }

    @Override
    public void setAcqOrder(String[] acqOrder) {
        //TODO implement me
    }

    @Override
    public SampleSchedule getSampleSchedule() {
        return sampleSchedule;
    }

    @Override
    public void setSampleSchedule(SampleSchedule sampleSchedule) {
        this.sampleSchedule = sampleSchedule;
    }

    @Override
    public DatasetType getPreferredDatasetType() {
        return preferredDatasetType;
    }

    @Override
    public void setPreferredDatasetType(DatasetType datasetType) {
        this.preferredDatasetType = datasetType;
    }

    @Override
    public String toString() {
        return getFilePath();
    }

    /**
     * Check whether the path contains a JCamp FID file
     *
     * @param bpath the path to check
     * @return true if the path correspond to a JCamp FID file.
     */
    public static boolean findFID(StringBuilder bpath) {
        String lower = bpath.toString().toLowerCase();
        return MATCHING_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }


    /**
     * Check whether the path contains a JCamp dataset file
     *
     * @param bpath the path to check
     * @return true if the path correspond to a JCamp dataset file.
     */
    public static boolean findData(StringBuilder bpath) {
        // FID and Dataset have the same extensions
        return findFID(bpath);
    }
}
