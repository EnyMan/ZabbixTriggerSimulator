package com.zbt;

import com.zbt.Exceptions.NotValidGenerator;

import java.util.ArrayList;

/**
 * Created by Mous on 20.3.2017.
 */
public class ArrayGen extends Generator {

    private Integer repeats;
    private Integer step;
    private ArrayList<Double> array;

    public ArrayGen(String rule) throws NotValidGenerator {
        super(rule);
        step = 0;
        array = new ArrayList<Double>();
        String[] temp = rule.split("\\*");
        if(temp.length >= 2) {
            repeats = Integer.parseInt(temp[1]) -1;
        } else {
            repeats = 0;
        }
        temp[0] = temp[0].replace("[", "");
        temp[0] = temp[0].replace("]","");
        for (String number : temp[0].split(",")){
            this.array.add(Double.parseDouble(number));
        }
    }

    @Override
    public Double tick() {
        if (step >= array.size()){
            repeats--;
            step=0;
        }
        if (repeats == -1){
            return null;
        }
        Double number = array.get(step);
        step++;
        return number;

    }
}
