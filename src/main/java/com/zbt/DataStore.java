package com.zbt;

import com.zbt.Exceptions.EndOfData;
import com.zbt.Exceptions.NotValidGenerator;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Mous on 14.2.2017.
 */
public class DataStore {

    ArrayList<Double> data;
    ArrayList<Generator> generators;
    Integer currentGenerator;
    public DataStore(String definition) throws NotValidGenerator {
        generators = new ArrayList<>();
        data = new ArrayList<>();
        createGen(definition);
        currentGenerator = 0;
    }

    public ArrayList<Double> getData() {
        return data;
    }

    public void createGen(String definition) throws NotValidGenerator {
        ArrayList<String> definitions = new ArrayList<>();
        definitions.addAll(Arrays.asList(definition.split("\\s*\\+\\s*")));
        for (String def : definitions ){
            generators.add(Generator.decide(def));
        }
    }

    public void tick() throws EndOfData {
        if (currentGenerator >= generators.size()) throw new EndOfData();
        Double nextData = generators.get(currentGenerator).tick();
        if (nextData != null) data.add(nextData);
        else {currentGenerator++;
            if (currentGenerator >= generators.size()) throw new EndOfData();
            nextData = generators.get(currentGenerator).tick();
            if (nextData != null) data.add(nextData);
        }
    }

    public ArrayList<Generator> getGenerators() {
        return generators;
    }
    public String getGeneratorsRules()
    {
        String separator = "";
        String rules = "";
        for(Generator gen : generators){
            rules += separator;
            rules+= gen.getRule();
            separator = "+";
        }
        return rules;
    }
}