package org.nmrfx.processor.datasets;

import org.apache.commons.math3.util.MultidimensionalCounter;
import org.nmrfx.processor.datasets.vendor.NMRData;

import java.util.*;

public class DatasetGroupIndex {
    final int[] indices;
    final int groupIndex;

    public DatasetGroupIndex(int[] indices, int groupIndex) {
        this.indices = indices.clone();
        this.groupIndex = groupIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatasetGroupIndex that = (DatasetGroupIndex) o;
        return groupIndex == that.groupIndex && Arrays.equals(indices, that.indices);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(groupIndex);
        result = 31 * result + Arrays.hashCode(indices);
        return result;
    }

    public String toIndexString() {
        boolean first = true;
        StringBuilder sBuilder = new StringBuilder();
        for (int index : indices) {
            if (!first) {
                sBuilder.append(",");
            } else {
                first = false;
            }
            if (index >= 0) {
                index++;
            }
            sBuilder.append(index);
        }
        return sBuilder.toString();
    }

    public String toString() {
        return toIndexString() + " " + groupIndex;
    }

    public int[] getIndices() {
        return indices;
    }

    public int getGroupIndex() {
        return groupIndex;
    }

    public static Optional<String> getSkipString(Collection<DatasetGroupIndex> groups) {
        Optional<String> result = Optional.empty();
        boolean scriptMode = true;
        if (!groups.isEmpty()) {
            StringBuilder sBuilder = new StringBuilder();
            if (scriptMode) {
                sBuilder.append("[");
            }
            for (DatasetGroupIndex group : groups) {
                if (scriptMode) {
                    if (sBuilder.length() != 1) {
                        sBuilder.append("],[");
                    }
                } else {
                    sBuilder.append(" ");
                }
                sBuilder.append(group.toIndexString());
            }
            if (scriptMode) {
                sBuilder.append("]");
            }
            result = Optional.of(sBuilder.toString());
        }

        return result;
    }
    public  List<int[]> groupToIndices() {
        List<int[]> result = new ArrayList<>();
        int[] sizes = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            sizes[i] = 2;
        }
        MultidimensionalCounter counter = new MultidimensionalCounter(sizes);
        var iterator = counter.iterator();
        System.out.println("iter");
        while (iterator.hasNext()) {
            iterator.next();
            int[] counts = iterator.getCounts();
            int[] cIndices = new int[indices.length];
            for (int k = 0; k < indices.length; k++) {
                if (indices[k] >= 0) {
                    cIndices[k] = indices[k] * sizes[k] + counts[k];
                    System.out.println(k + " " + indices[k] + " " + sizes[k] + " " + counts[k] + " " + cIndices[k]);
                } else {
                    cIndices[k] = -1;
                }
            }
            result.add(cIndices);
        }
        return result;
    }

}
