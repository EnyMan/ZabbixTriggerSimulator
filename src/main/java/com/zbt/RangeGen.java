package com.zbt;

import com.zbt.Exceptions.NotValidGenerator;

/**
 * Created by Mous on 20.3.2017.
 */
public class RangeGen extends Generator {

    Integer repeats;
    Double from;
    Double to;
    Integer step;

    public RangeGen(String rule) throws NotValidGenerator {
        super(rule);
        step = 0;
        String[] temp = rule.split("\\*");
        if(temp.length >= 2) {
            repeats = Integer.parseInt(temp[1]) -1;
        } else {
            repeats = 0;
        }
        from = Double.parseDouble(temp[0].substring(1, temp[0].indexOf("..")));
        to = Double.parseDouble(temp[0].substring(temp[0].indexOf("..")+2, temp[0].length()-1));
    }

    @Override
    public Double tick() {
        if (step > (to-from)){
            repeats--;
            step=0;
        }
        if (repeats == -1){
            return null;
        }
        Double tmp = from + step;
        step++;
        return tmp;
    }
}
