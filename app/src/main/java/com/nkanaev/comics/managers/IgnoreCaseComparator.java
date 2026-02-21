package com.nkanaev.comics.managers;

import java.util.Comparator;
import java.util.function.Function;

public abstract class IgnoreCaseComparator implements Comparator<Object> {

    public abstract String stringValue(Object o);

    /**
     * Creates a comparator for objects that extracts a string using the provided function.
     * This eliminates the need for anonymous inner classes throughout the codebase.
     *
     * @param stringExtractor Function to extract string from object
     * @param <T> Type of objects being compared
     * @return IgnoreCaseComparator instance
     */
    public static <T> IgnoreCaseComparator forFunction(Function<T, String> stringExtractor) {
        return new IgnoreCaseComparator() {
            @Override
            public String stringValue(Object o) {
                return stringExtractor.apply((T) o);
            }
        };
    }

    private String preProcess(Object o){
        if (o == null)
            return "";

        String string = (o instanceof String) ? (String) o : stringValue(o);
        return string;
    }

    public int compare(Object a, Object b) {
        String strA = preProcess(a);
        String strB = preProcess(b);

        return NaturalSort.compareIgnoreCase(strA,strB);
    }

    public static abstract class Reverse extends IgnoreCaseComparator {
        public int compare(Object a, Object b) {
            return super.compare(b, a);
        }
    }

    // remove file extensions if we want to compare *file names* only
    public static abstract class FileNameComparator extends IgnoreCaseComparator{

        private String preProcess(Object o) {
            String string = "";
            if (o == null)
                return string;

            string = (o instanceof String) ? (String) o : stringValue(o);

            string = Utils.removeExtensionIfAny(string);
            return string;
        }

        public int compare(Object a, Object b) {
            String strA = preProcess(a);
            String strB = preProcess(b);

            return super.compare(strA,strB);
        }
    }
}