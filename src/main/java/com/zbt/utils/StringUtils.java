package com.zbt.utils;

import javafx.util.Pair;


/**
 * Created by Mous on 29.1.2017.
 */
public class StringUtils {
    public static Pair<Integer, Integer> findBrackets(String exp, Integer startBr){
        Integer opened = 0;
        Integer stop = -1;
        Integer start = exp.indexOf('(', startBr);
        Integer nextBr = start + 1;
        for (Integer a = 0; a <= exp.length(); a++){
            Integer openingBr = exp.indexOf('(', nextBr);
            Integer closingBr = exp.indexOf(')', nextBr);
            if (openingBr != -1 && (openingBr < closingBr)){
                opened += 1;
                nextBr = openingBr +1;
            } else {
                if (opened > 0) {
                    opened -= 1;
                    nextBr = closingBr +1;
                } else {
                    stop = closingBr;
                    break;
                }
            }
        }
        return new Pair<Integer, Integer>(start, stop);
    }

    public static Pair<Integer, Integer> findBrackets(String exp){
        return findBrackets(exp, 0);
    }

    public static String placeCharAt(String target, Integer at, char placeThis){
        if (at == target.length()){
            return target + placeThis;
        } else {
            StringBuilder replaced = new StringBuilder(target);
            replaced.setCharAt(at, placeThis);
            return replaced.toString();
        }
    }
}
