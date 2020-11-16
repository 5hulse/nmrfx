package org.nmrfx.peaks;

import java.util.*;

import org.apache.commons.math3.exception.MaxCountExceededException;
import org.nmrfx.datasets.DatasetBase;
import org.nmrfx.datasets.Nuclei;
import org.nmrfx.project.ProjectBase;

public class PeakListBase {

    static ResonanceFactory resFactory = new ResonanceFactory();
    /**
     *
     */
    public static PeakListBase clusterOrigin = null;
    public int idLast;
    /**
     *
     */
    public String fileName;
    protected String listName;
    protected final int listID;
    protected String details = "";
    protected String sampleLabel = "";
    protected String sampleConditionLabel = "";
    protected List<Peak> peaks;
    protected final Map<Integer, Peak> indexMap = new HashMap<>();
    boolean slideable = false;
    boolean requireSliderCondition = false;
    static boolean globalRequireSliderCondition = false;
    protected List<SearchDim> searchDims = new ArrayList<>();
    Optional<Measures> measures = Optional.empty();
    Map<String, String> properties = new HashMap<>();

    /**
     *
     * @param name
     * @param n
     * @param listNum
     */
    public PeakListBase(String name, int n, Integer listNum) {
        listName = name;
        fileName = "";
        idLast = -1;

        int i;

        for (i = 0; i < nDim; i++) {
            spectralDims[i] = new SpectralDim(this, i);
        }

        peaks = new ArrayList<>();
        indexMap.clear();
        ProjectBase.getActive().addPeakList(this, listName);
        if (listNum == null) {
            listNum = 1;
            while (get(listNum).isPresent()) {
                listNum++;
            }
        }
        listID = listNum;
    }

    /**
     *
     * @param name
     * @param n
     */
    public PeakListBase(String name, int n) {
        this(name, n, null);
    }

    public static ResonanceFactory resFactory() {
        return resFactory;
    }

    /**
     * Returns an Optional containing the PeakList that has the specified id
     * number or empty value if no PeakList with that id exists.
     *
     * @param listID the id of the peak list
     * @return the Optional containing the PeaKlist or an empty value if no
     * PeakList with that id exists
     */
    public static Optional<PeakListBase> get(int listID) {
        return ProjectBase.getActive().getPeakList(listID);
    }
    /**
     *
     */
    public int nDim;
    /**
     *
     */
    public double scale;
    protected SpectralDim[] spectralDims = null;

    /**
     *
     * @param peak
     */
    public static void unLinkPeak(Peak peak) {
        for (int i = 0; i < peak.peakList.nDim; i++) {
            List<PeakDim> peakDims = getLinkedPeakDims(peak, i);
            PeakListBase.unLinkPeak(peak, i);

            for (PeakDim pDim : peakDims) {
                if (pDim.getPeak() != peak) {
                    if (pDim.isCoupled()) {
                        if (peakDims.size() == 2) {
                            pDim.getMultiplet().setSinglet();
                        } else {
                            pDim.getMultiplet().setGenericMultiplet();
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @param peak
     * @param iDim
     */
    public static void unLinkPeak(Peak peak, int iDim) {
        PeakDim peakDim = peak.getPeakDim(iDim);
        if (peakDim != null) {
            peakDim.unLink();
        }
    }

    /**
     *
     * @param peak
     * @return
     */
    public static List getLinks(Peak peak) {
        List peakDims = getLinkedPeakDims(peak, 0);
        ArrayList peaks = new ArrayList(peakDims.size());
        for (int i = 0; i < peakDims.size(); i++) {
            PeakDim peakDim = (PeakDim) peakDims.get(i);
            peaks.add(peakDim.getPeak());
        }
        return peaks;
    }

    /**
     *
     * @param peak
     * @param iDim
     * @return
     */
    public static List<Peak> getLinks(final Peak peak, final int iDim) {
        final List<PeakDim> peakDims = getLinkedPeakDims(peak, iDim);
        final List<Peak> peaks = new ArrayList<>(peakDims.size());
        for (int i = 0; i < peakDims.size(); i++) {
            PeakDim peakDim = (PeakDim) peakDims.get(i);
            peaks.add((Peak) peakDim.getPeak());
        }
        return peaks;
    }

    /**
     *
     * @param peakA
     * @param dimA
     * @param peakB
     * @param dimB
     */
    public static void linkPeaks(Peak peakA, String dimA, Peak peakB, String dimB) {
        PeakDim peakDimA = peakA.getPeakDim(dimA);
        PeakDim peakDimB = peakB.getPeakDim(dimB);
        if ((peakDimA != null) && (peakDimB != null)) {
            PeakListBase.linkPeakDims(peakDimA, peakDimB);
        }
    }

    /**
     *
     * @param peakA
     * @param dimA
     * @param peakB
     * @param dimB
     */
    public static void linkPeaks(Peak peakA, int dimA, Peak peakB, int dimB) {
        PeakDim peakDimA = peakA.getPeakDim(dimA);
        PeakDim peakDimB = peakB.getPeakDim(dimB);
        if ((peakDimA != null) && (peakDimB != null)) {
            PeakListBase.linkPeakDims(peakDimA, peakDimB);
        }
    }

    /**
     *
     * @param peakDimA
     * @param peakDimB
     */
    public static void linkPeakDims(PeakDim peakDimA, PeakDim peakDimB) {
        Resonance resonanceA = peakDimA.getResonance();
        Resonance resonanceB = peakDimB.getResonance();

        Resonance.merge(resonanceA, resonanceB);

        peakDimA.peakDimUpdated();
        peakDimB.peakDimUpdated();
    }

    /**
     *
     * @param peakDimA
     * @param peakDimB
     */
    public static void couplePeakDims(PeakDim peakDimA, PeakDim peakDimB) {
        Resonance resonanceA = peakDimA.getResonance();
        Resonance resonanceB = peakDimB.getResonance();

        Resonance.merge(resonanceA, resonanceB);

        Multiplet.merge(peakDimA, peakDimB);
        peakDimA.peakDimUpdated();
        peakDimB.peakDimUpdated();
    }

    /**
     *
     * @param peak1
     * @param dim1
     * @param peak2
     * @return
     */
    public static boolean isLinked(Peak peak1, int dim1, Peak peak2) {
        boolean result = false;
        List<PeakDim> peakDims = getLinkedPeakDims(peak1, dim1);
        for (PeakDim peakDim : peakDims) {
            if (peakDim.getPeak() == peak2) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     *
     * @return
     */
    public String getDatasetName() {
        return fileName;
    }

    public boolean isSimulated() {
        return getSampleConditionLabel().contains("sim");
    }

    /**
     * Rename the peak list.
     *
     * @param newName
     */
    public void setName(String newName) {
        ProjectBase<PeakListBase> project = ProjectBase.getActive();
        project.removePeakList(listName);
        listName = newName;
        project.addPeakList(this, newName);
    }

    /**
     *
     * @param sampleLabel
     */
    public void setSampleLabel(String sampleLabel) {
        this.sampleLabel = sampleLabel;
    }

    /**
     *
     * @param datasetName
     */
    public void setDatasetName(String datasetName) {
        this.fileName = datasetName;
    }

    public List<SearchDim> getSearchDims() {
        return searchDims;
    }

    public void peakListUpdated(Object object) {
    }

    /**
     *
     */
    public void clearIndex() {
        indexMap.clear();
    }

    /**
     * @return the number of dimensions of the peak list.
     */
    public int getNDim() {
        return nDim;
    }

    /**
     * @return
     */
    public double getScale() {
        return scale;
    }

    /**
     *
     */
    public void setScale(double scale) {
        this.scale = scale;
    }

    /**
     *
     * @param iDim
     * @return
     */
    public SpectralDim getSpectralDim(int iDim) {
        SpectralDim specDim = null;
        if (iDim < spectralDims.length) {
            specDim = spectralDims[iDim];
        }
        return specDim;
    }

    /**
     *
     * @param name
     * @return
     */
    public SpectralDim getSpectralDim(String name) {
        SpectralDim specDim = null;
        for (SpectralDim sDim : spectralDims) {
            if (sDim.getDimName().equals(name)) {
                specDim = sDim;
                break;
            }
        }
        return specDim;
    }

    /**
     *
     * @return
     */
    public String getSampleLabel() {
        return sampleLabel;
    }

    /**
     *
     * @param sampleConditionLabel
     */
    public void setSampleConditionLabel(String sampleConditionLabel) {
        this.sampleConditionLabel = sampleConditionLabel;
    }

    /**
     *
     * @return
     */
    public String getSampleConditionLabel() {
        return sampleConditionLabel;
    }

    /**
     *
     * @param details
     */
    public void setDetails(String details) {
        this.details = details;
    }

    /**
     *
     * @return
     */
    public String getDetails() {
        return details;
    }

    /**
     *
     * @return the ID number of the peak list.
     */
    public int getId() {
        return listID;
    }

    /**
     *
     * @return the name of the peak list.
     */
    public String getName() {
        return listName;
    }

    /**
     *
     * @return a peak list object.
     */
    public List<Peak> peaks() {
        return peaks;
    }

    /**
     *
     * @param i
     * @return
     */
    public Peak getPeak(int i) {
        if (peaks == null) {
            return null;
        }
        if (indexMap.isEmpty()) {
            reIndex();
        }

        if ((i >= 0) && (i < peaks.size())) {
            return (peaks.get(i));
        } else {
            return null;
        }
    }

    /**
     *
     * @param newPeak
     */
    public void addPeakWithoutResonance(Peak newPeak) {
        peaks.add(newPeak);
        clearIndex();
    }

    /**
     *
     * @param newPeak
     */
    public Peak addPeak(Peak newPeak) {
        newPeak.initPeakDimContribs();
        peaks.add(newPeak);
        clearIndex();
        return newPeak;
    }

    /**
     *
     * @param s
     * @return
     */
    public int getListDim(String s) {
        int iDim = -1;

        for (int i = 0; i < nDim; i++) {
            if (getSpectralDim(i).getDimName().equalsIgnoreCase(s)) {
                iDim = i;

                break;
            }
        }

        return iDim;
    }

    public static double foldPPM(double ppm, double fDelta, double min, double max) {
        if (min > max) {
            double hold = min;
            min = max;
            max = hold;
        }
        if (min != max) {
            while (ppm > max) {
                ppm -= fDelta;
            }
            while (ppm < min) {
                ppm += fDelta;
            }
        }
        return ppm;
    }

    /**
     *
     */
    public void reIndex() {
        int i = 0;
        indexMap.clear();
        for (Peak peak : peaks) {
            peak.setIndex(i++);
            indexMap.put(peak.getIdNum(), peak);
        }
        peakListUpdated(this);
    }

    /**
     *
     * @return
     */
    public int size() {
        if (peaks == null) {
            return 0;
        } else {
            return peaks.size();
        }
    }

    /**
     *
     * @param dataset
     * @param looseMode
     * @return
     */
    public int[] getDimsForDataset(DatasetBase dataset, boolean looseMode) {
        int[] pdim = new int[nDim];
        int dataDim = dataset.getNDim();
        boolean[] used = new boolean[dataDim];
        for (int j = 0; j < nDim; j++) {
            boolean ok = false;
            for (int i = 0; i < dataDim; i++) {
                if (!used[i]) {
                    if (getSpectralDim(j).getDimName().equals(dataset.getLabel(i))) {
                        pdim[j] = i;
                        used[i] = true;
                        ok = true;
                        break;
                    }
                }
            }

            if (!ok && looseMode) {
                String pNuc = getSpectralDim(j).getNucleus();
                for (int i = 0; i < dataDim; i++) {
                    if (!used[i]) {
                        String dNuc = dataset.getNucleus(i).getNumberName();
                        if (dNuc.equals(pNuc)) {
                            pdim[j] = i;
                            used[i] = true;
                            ok = true;
                            break;
                        }
                    }
                }
            }
            if (!ok) {
                throw new IllegalArgumentException(
                        "Can't find match for peak dimension \""
                        + getSpectralDim(j).getDimName() + "\"");
            }
        }
        return pdim;
    }

    /**
     *
     * @param dataset
     * @return
     */
    public int[] getDimsForDataset(DatasetBase dataset) {
        return getDimsForDataset(dataset, false);
    }

    /**
     *
     * @param peakSpecifier
     * @return
     */
    public static Peak getAPeak(String peakSpecifier) {
        int dot = peakSpecifier.indexOf('.');

        if (dot == -1) {
            return null;
        }

        int lastDot = peakSpecifier.lastIndexOf('.');

        ProjectBase<PeakListBase> project = ProjectBase.getActive();
        PeakListBase peakList = project.
                getPeakList(peakSpecifier.substring(0, dot));

        if (peakList == null) {
            return null;
        }

        if (peakList.indexMap.isEmpty()) {
            peakList.reIndex();
        }

        int idNum;

        if (lastDot == dot) {
            idNum = Integer.parseInt(peakSpecifier.substring(dot + 1));
        } else {
            idNum = Integer.parseInt(peakSpecifier.substring(dot + 1, lastDot));
        }

        Peak peak = (Peak) peakList.indexMap.get(idNum);
        return peak;
    }

    /**
     *
     * @param peakSpecifier
     * @param iDimInt
     * @return
     * @throws IllegalArgumentException
     */
    public static Peak getAPeak(String peakSpecifier,
            Integer iDimInt) throws IllegalArgumentException {
        int dot = peakSpecifier.indexOf('.');

        if (dot == -1) {
            return null;
        }

        int lastDot = peakSpecifier.lastIndexOf('.');

        ProjectBase<PeakListBase> project = ProjectBase.getActive();
        PeakListBase peakList = project.
                getPeakList(peakSpecifier.substring(0, dot));

        if (peakList == null) {
            return null;
        }

        int idNum;

        try {
            if (lastDot == dot) {
                idNum = Integer.parseInt(peakSpecifier.substring(dot + 1));
            } else {
                idNum = Integer.parseInt(peakSpecifier.substring(dot + 1,
                        lastDot));
            }
        } catch (NumberFormatException numE) {
            throw new IllegalArgumentException(
                    "error parsing peak " + peakSpecifier + ": " + numE.toString());
        }

        return peakList.getPeakByID(idNum);
    }

    /**
     *
     * @param peakSpecifier
     * @return
     * @throws IllegalArgumentException
     */
    public static PeakDim getPeakDimObject(String peakSpecifier)
            throws IllegalArgumentException {
        int dot = peakSpecifier.indexOf('.');

        if (dot == -1) {
            return null;
        }

        int lastDot = peakSpecifier.lastIndexOf('.');

        ProjectBase<PeakListBase> project = ProjectBase.getActive();
        PeakListBase peakList = project.
                getPeakList(peakSpecifier.substring(0, dot));

        if (peakList == null) {
            return null;
        }

        int idNum;

        try {
            if (lastDot == dot) {
                idNum = Integer.parseInt(peakSpecifier.substring(dot + 1));
            } else {
                idNum = Integer.parseInt(peakSpecifier.substring(dot + 1,
                        lastDot));
            }
        } catch (NumberFormatException numE) {
            throw new IllegalArgumentException(
                    "error parsing peak " + peakSpecifier + ": " + numE.toString());
        }

        Peak peak = peakList.getPeakByID(idNum);
        if (peak == null) {
            return null;
        }
        int iDim = peakList.getPeakDim(peakSpecifier);

        return peak.peakDims[iDim];
    }

    /**
     *
     * @param idNum
     * @return
     * @throws IllegalArgumentException
     */
    public Peak getPeakByID(int idNum) throws IllegalArgumentException {
        if (indexMap.isEmpty()) {
            reIndex();
        }
        Peak peak = indexMap.get(idNum);
        return peak;
    }

    /**
     *
     * @param peakSpecifier
     * @return
     */
    public static int getPeakDimNum(String peakSpecifier) {
        int iDim = 0;
        int dot = peakSpecifier.indexOf('.');

        if (dot != -1) {
            int lastDot = peakSpecifier.lastIndexOf('.');

            if (dot != lastDot) {
                String dimString = peakSpecifier.substring(lastDot + 1);
                iDim = Integer.parseInt(dimString) - 1;
            }
        }

        return iDim;
    }

    /**
     *
     * @param peakSpecifier
     * @return
     * @throws IllegalArgumentException
     */
    public int getPeakDim(String peakSpecifier)
            throws IllegalArgumentException {
        int iDim = 0;
        int dot = peakSpecifier.indexOf('.');

        if (dot != -1) {
            int lastDot = peakSpecifier.lastIndexOf('.');

            if (dot != lastDot) {
                String dimString = peakSpecifier.substring(lastDot + 1);
                iDim = getListDim(dimString);

                if (iDim == -1) {
                    try {
                        iDim = Integer.parseInt(dimString) - 1;
                    } catch (NumberFormatException nFE) {
                        iDim = -1;
                    }
                }
            }
        }

        if ((iDim < 0) || (iDim >= nDim)) {
            throw new IllegalArgumentException(
                    "Invalid peak dimension in \"" + peakSpecifier + "\"");
        }

        return iDim;
    }

    static Peak getAPeak2(String peakSpecifier) {
        int dot = peakSpecifier.indexOf('.');

        if (dot == -1) {
            return null;
        }

        int lastDot = peakSpecifier.lastIndexOf('.');

        ProjectBase<PeakListBase> project = ProjectBase.getActive();
        PeakListBase peakList = project.
                getPeakList(peakSpecifier.substring(0, dot));

        if (peakList == null) {
            return null;
        }

        int idNum;

        if (dot == lastDot) {
            idNum = Integer.parseInt(peakSpecifier.substring(dot + 1));
        } else {
            idNum = Integer.parseInt(peakSpecifier.substring(dot + 1, lastDot));
        }

        Peak peak = peakList.getPeak(idNum);

        return (peak);
    }

    /**
     *
     * @return
     */
    public boolean hasSearchDims() {
        return !searchDims.isEmpty();
    }

    /**
     *
     */
    public void clearSearchDims() {
        searchDims.clear();
    }

    /**
     *
     * @param s
     * @throws IllegalArgumentException
     */
    public void setSearchDims(String s) throws IllegalArgumentException {
        String[] elements = s.split(" ");
        if ((elements.length % 2) != 0) {
            throw new IllegalArgumentException("Invalid search dim string: " + s);
        }
        clearSearchDims();
        for (int i = 0; i < elements.length; i += 2) {
            double tol = Double.parseDouble(elements[i + 1]);
            addSearchDim(elements[i], tol);
        }

    }

    /**
     *
     * @param dimName
     * @param tol
     */
    public void addSearchDim(String dimName, double tol) {
        int iDim = getListDim(dimName);
        addSearchDim(iDim, tol);
    }

    /**
     *
     * @param iDim
     * @param tol
     */
    public void addSearchDim(int iDim, double tol) {
        Iterator<SearchDim> iter = searchDims.iterator();
        while (iter.hasNext()) {
            SearchDim sDim = iter.next();
            if (sDim.iDim == iDim) {
                iter.remove();
            }
        }
        SearchDim sDim = new SearchDim(iDim, tol);
        searchDims.add(sDim);
    }

    /**
     *
     * @param noiseLevel
     */
    public void setFOM(double noiseLevel) {
        for (int i = 0; i < peaks.size(); i++) {
            Peak peak = peaks.get(i);
            double devMul = Math.abs(peak.getIntensity() / noiseLevel);
            if (devMul > 20.0) {
                devMul = 20.0;
            }
            double erf;
            try {
                erf = org.apache.commons.math3.special.Erf.erf(devMul / Math.sqrt(2.0));
            } catch (MaxCountExceededException mathE) {
                erf = 1.0;
            }
            float fom = (float) (0.5 * (1.0 + erf));
            peak.setFigureOfMerit(fom);
        }
    }

    /**
     *
     */
    public void reNumber() {
        for (int i = 0; i < peaks.size(); i++) {
            Peak peak = peaks.get(i);
            peak.setIdNum(i);
        }
        idLast = peaks.size() - 1;
        reIndex();
    }

    /**
     *
     * @return
     */
    public boolean hasMeasures() {
        return measures.isPresent();
    }

    /**
     *
     * @param measure
     */
    public void setMeasures(Measures measure) {
        measures = Optional.of(measure);
    }

    /**
     *
     * @return
     */
    public double[] getMeasureValues() {
        double[] values = null;
        if (hasMeasures()) {
            values = measures.get().getValues();
        }
        return values;
    }

    /**
     *
     * @param name
     * @return
     */
    public String getProperty(String name) {
        String result = "";
        if (properties.containsKey(name)) {
            result = properties.get(name);
        }
        return result;
    }

    /**
     *
     * @param name
     * @return
     */
    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    /**
     *
     * @param name
     * @param value
     */
    public void setProperty(String name, String value) {
        properties.put(name, value);
    }

    /**
     *
     * @return
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     *
     * @return
     */
    public Peak getNewPeak() {
        Peak peak = new Peak(this, nDim);
        addPeak(peak);
        return peak;
    }

    /**
     *
     * @return
     */
    public int addPeak() {
        Peak peak = new Peak(this, nDim);
        addPeak(peak);
        return (peak.getIdNum());
    }

    /**
     *
     * @param peak
     */
    public void removePeak(Peak peak) {
        if (peaks.get(peaks.size() - 1) == peak) {
            idLast--;
        }
        peaks.remove(peak);
        reIndex();
    }

    /**
     *
     * @param peak
     * @param requireSameList
     * @return
     */
    public static List<Peak> getLinks(Peak peak, boolean requireSameList) {
        List<PeakDim> peakDims = getLinkedPeakDims(peak, 0);
        ArrayList<Peak> peaks = new ArrayList(peakDims.size());
        for (PeakDim peakDim : peakDims) {
            if (!requireSameList || (peakDim.getPeak().peakList == peak.peakList)) {
                peaks.add((Peak) peakDim.getPeak());
            }
        }
        return peaks;
    }

    /**
     *
     * @param peak
     * @return
     */
    public static List<PeakDim> getLinkedPeakDims(Peak peak) {
        return getLinkedPeakDims(peak, 0);
    }

    /**
     *
     * @param peak
     * @param iDim
     * @return
     */
    public static List<PeakDim> getLinkedPeakDims(Peak peak, int iDim) {
        PeakDim peakDim = peak.getPeakDim(iDim);
        return peakDim.getLinkedPeakDims();
    }

    /**
     *
     * @return
     */
    public boolean isSlideable() {
        return slideable;
    }

    /**
     *
     * @param state
     */
    public void setSlideable(boolean state) {
        slideable = state;
    }

    /**
     *
     * @return
     */
    public boolean requireSliderCondition() {
        return requireSliderCondition;
    }

    /**
     *
     * @return
     */
    public Nuclei[] guessNuclei() {
        double[] sf = new double[nDim];
        for (int i = 0; i < nDim; i++) {
            SpectralDim sDim = getSpectralDim(i);
            sf[i] = sDim.getSf();
        }
        Nuclei[] nuclei = Nuclei.findNuclei(sf);
        return nuclei;
    }

    /**
     *
     * @param iDim
     * @return
     */
    public DoubleSummaryStatistics widthStatsPPM(int iDim) {
        DoubleSummaryStatistics stats = peaks.stream().filter(p -> p.getStatus() >= 0).mapToDouble(p -> p.peakDims[iDim].getLineWidth()).summaryStatistics();
        return stats;
    }

    /**
     *
     * @return
     */
    public int compress() {
        int nRemoved = 0;
        for (int i = (peaks.size() - 1); i >= 0; i--) {
            if ((peaks.get(i)).getStatus() < 0) {
                PeakListBase.unLinkPeak(peaks.get(i));
                (peaks.get(i)).markDeleted();
                peaks.remove(i);
                nRemoved++;
            }
        }
        reIndex();
        return nRemoved;
    }

    /**
     *
     */
    public void unLinkPeaks() {
        int nPeaks = peaks.size();

        for (int i = 0; i < nPeaks; i++) {
            PeakListBase.unLinkPeak(peaks.get(i));
        }
    }

    public class SearchDim {

        final int iDim;
        final double tol;

        public SearchDim(int iDim, double tol) {
            this.iDim = iDim;
            this.tol = tol;
        }

        public int getDim() {
            return iDim;
        }

        public double getTol() {
            return tol;
        }
    }
}