package de.uniaugsburg.isse.csp.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class StringLists {

    public static String toString(List<String> list) {
        StringBuilder builder = new StringBuilder();
        for (String entry : list) {
            builder.append(entry);
            builder.append("\n");
        }
        return builder.toString();
    }

    public static List<String> toList(String string) {
        List<String> resultList = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(string, "\n");
        while (tokenizer.hasMoreTokens()) {
            resultList.add(tokenizer.nextToken());
        }
        return resultList;
    }

}
