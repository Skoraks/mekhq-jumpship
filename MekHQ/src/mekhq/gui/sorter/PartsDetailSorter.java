package mekhq.gui.sorter;

import java.util.Comparator;

/**
 *
 * @author Dylan Myers
 * Comparator for comparing details in the warehouse and parts store
 */
public class PartsDetailSorter implements Comparator<String> {

    @Override
    public int compare(String s0, String s1) {
        String[] ss0 = s0.replace("<html>", "").replace("</html>", "").split(" ");
        String[] ss1 = s1.replace("<html>", "").replace("</html>", "").split(" ");
        double l0 = Double.parseDouble(ss0[0]);
        double l1 = Double.parseDouble(ss1[0]);
        s0 = ss0[1];
        s1 = ss1[1];
        int sComp = s0.compareTo(s1);
        if (sComp == 0) {
            return ((Comparable<Double>)l0).compareTo(l1);
        } else {
            return sComp;
        }
    }

}