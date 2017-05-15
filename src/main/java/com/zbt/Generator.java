package com.zbt;

import com.zbt.Exceptions.NotValidGenerator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Mous on 20.3.2017.
 */
public abstract class Generator {

    String rule;

    public Generator(String rule) {
        this.rule = rule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Generator)) return false;

        Generator generator = (Generator) o;

        return rule.equals(generator.rule);
    }

    @Override
    public int hashCode() {
        return rule.hashCode();
    }

    public String getRule() {
        return rule;
    }

    public static Generator decide(String rule) throws NotValidGenerator {
        String pattern = "^\\{(?:\\d*\\.\\.)*\\d*\\}\\s*(?:\\*\\s*\\d+)?$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(rule);

        if(m.find()){
            return new RangeGen(rule);
        }

        pattern= "^\\[(?:\\d*,)*\\d*\\]\\s*(?:\\*\\s*\\d+)?$";
        r = Pattern.compile(pattern);
        m = r.matcher(rule);

        if(m.find()){
            return new ArrayGen(rule);
        }

        throw new NotValidGenerator();
    }

    public abstract Double tick();
}

