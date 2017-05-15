package com.zbt;

import com.zbt.Exceptions.*;
import javafx.util.Pair;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Mous on 18.12.2016.
 */
public class Item implements Serializable{
    private static final String DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";
    DataStore store;
    String name;
    Integer time;
    Integer refresh;
    Pattern timePattern;
    Pattern indexPattern;
    Pattern patternPattern;
    Pattern numberPattern;
    Pattern optionPattern;
    boolean updated;
    boolean fresh;
    Date dNow;

    public Item(String name, String rule, Integer time) throws EndOfData, NotValidGenerator {
        this.name = name.replace(" ", "");
        store = new DataStore(rule);
        refresh = time;
        String indexRegex = "^#(\\d+)$";
        String timeRegex = "^(\\d+d)?(\\d+h)?(\\d+m)?(\\d+)?$";
        String patternRegex = "^(\\d+)(/(\\d+))?$";
        String numberRegex = "^(\\d+(\\.\\d+)?)$";
        String optionRegex = "^\"(eq|ne|gt|ge|lt|le|band)\"$";
        indexPattern = Pattern.compile(indexRegex);
        timePattern = Pattern.compile(timeRegex);
        patternPattern = Pattern.compile(patternRegex);
        numberPattern = Pattern.compile(numberRegex);
        optionPattern = Pattern.compile(optionRegex);
        updated = true;
        fresh = true;
        store.tick();
        this.time = 0;
        dNow = new Date();
    }

    public Item(String name, String rule) throws EndOfData, NotValidGenerator {
        this(name, rule, 30);
    }

    public Item(String name) throws EndOfData, NotValidGenerator {
        this(name, "[1,0]*5", 30);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;
        Item t = (Item)obj;
        return name.equals(t.getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public String getName() {
        return name;
    }

    public boolean isUpdated()  {
        return updated;
    }

    public Integer getRefresh() {
        return refresh;
    }

    public ArrayList<Generator> getGenerators() {
        return store.getGenerators();
    }

    public Double getLastData(){
        return store.getData().get(store.getData().size() - 1);
    }

    public String byName(String fn, String params) throws NotSupportedMethod {
        Object returnedValue = null;
        try {
            Method method = this.getClass().getDeclaredMethod(fn, params.getClass());
            returnedValue = method.invoke(this,params);
        } catch (NoSuchMethodException e) {
            throw new NotSupportedMethod();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new NotSupportedMethod();
        } catch (IllegalAccessException e) {
            throw new NotSupportedMethod();
        }
        return Double.toString((Double) returnedValue);
    }

    public void tick() throws EndOfData {
        if(fresh){
            fresh = false;
            return;
        }
        time ++;
        updated = false;
        if(time >= refresh){
            store.tick();
            time = 0;
            updated = true;
        }
    }

    public Integer collectIndex(String value) {
        if(value.equals("")) return null;
        Matcher M = this.indexPattern.matcher(value);
        if(M.find()){
            return Integer.parseInt(M.group(1));
        } else return null;
    }

    public Integer collectTime(String value) {
        if(value.equals("")) return null;
        Matcher M = this.timePattern.matcher(value);
        if(M.find()){
            String days = M.group(1);
            String hours = M.group(2);
            String minutes = M.group(3);
            String seconds = M.group(4);

            if (days == null) days = "0d";
            if (hours == null) hours = "0h";
            if (minutes == null) minutes = "0m";
            if (seconds== null) seconds = "0";

            Integer start = (int)Duration.parse(String.format("P%sT%s%s%sS", days, hours, minutes, seconds)).getSeconds();
            return (int) Math.ceil(start/refresh);
        } else return null;
    }

    public Pair<Integer, Integer> collectPattern(String value) {
        if(value == null) return null;
        Matcher M = this.patternPattern.matcher(value);
        if(M.find()){
            if (M.group(2) != null) return new Pair(Integer.parseInt(M.group(1)),Integer.parseInt(M.group(3)));
            return new Pair(Integer.parseInt(M.group(1)),null);
        } else return null;
    }

    public Double collectNumber(String value) {
        Matcher M = this.numberPattern.matcher(value);
        if (M.find()) {
            return Double.parseDouble(M.group(1));
        }else return null;
    }

    public String collectOption(String value) {
        if(value == null) return "eq";
        Matcher M = this.optionPattern.matcher(value);
        if(M.find()){
            return M.group(1);
        } else if (value.equals("")) {
            return "eq";
        } else return null;
    }

    public Double abschange(String dummy) throws NotValidParametr {
        return Math.abs(this.change(""));
    }
    public Double avg(String value) throws NotValidParametr {
        String syntax = "^\\s?(?:(#\\d+)|(\\d+[dhm]?))\\s?$";
        Pattern syntaxPattern = Pattern.compile(syntax);
        Matcher syntaxM = syntaxPattern.matcher(value);
        if (!syntaxM.find()) throw new NotValidParametr();
        value = value.replace(" ", "");
        Integer start = collectIndex(value);
        if (start == null) start = collectTime(value);
        start = store.getData().size() - start;
        if (start < 0) start = 0;
        Double sum = 0.;
        List<Double> subStore = store.getData().subList(start, store.getData().size());
        for(Double one : subStore){
            sum += one;
        }
        return sum / subStore.size();
    }
    public Double band(String value) throws NotValidParametr {
        String syntax = "^\\s?(?:(#\\d+)|(\\d+[dhm]?))\\s?,\\s?(\\d+)\\s?$";
        Pattern syntaxPattern = Pattern.compile(syntax);
        Matcher syntaxM = syntaxPattern.matcher(value);
        if (!syntaxM.find()) throw new NotValidParametr();
        value = value.replace(" ", "");
        syntaxM = syntaxPattern.matcher(value);
        syntaxM.find();
        Integer start = collectIndex(syntaxM.group(1));
        if (start == null) start = collectTime(syntaxM.group(1));
        Double mask = collectNumber(syntaxM.group(3));
        if (start >= store.getData().size()) start = store.getData().size();
        Integer itemdata = store.getData().get(store.getData().size()-start).intValue();
        Integer band = itemdata & mask.intValue();
        return band.doubleValue();
    }
    public Double change(String dummy) throws NotValidParametr {

        return last() - prev("");
    }
    public Double count(String value) throws NotValidParametr {
        String syntax = "^\\s?((#\\d+)|(\\d+[dhm]?))(\\s?,\\s?(\\d+/?\\d*)(\\s?,\\s?(\\D+))?)?\\s?$";
        Pattern syntaxPattern = Pattern.compile(syntax);
        Matcher syntaxM = syntaxPattern.matcher(value);
        if (!syntaxM.find()) throw new NotValidParametr();
        value = value.replace(" ", "");
        syntaxM = syntaxPattern.matcher(value);
        syntaxM.find();
        // collect start
        Integer start = collectIndex(syntaxM.group(1));
        if (start == null) start = collectTime(syntaxM.group(1));
        if (start == null) throw new NotValidParametr();
        // collect patter to search
        Pair<Integer, Integer> pattern = collectPattern(syntaxM.group(5));
        // collect option to compare
        String option = collectOption(syntaxM.group(7));
        if (option == null) throw new NotValidParametr();

        if (!option.equals("eq") && pattern == null) throw new NotValidParametr();

        start = store.getData().size() - start;
        if (start < 0) start = 0;

        if( pattern == null) {
            Integer size = store.getData().subList(start, store.getData().size()).size();
            return size.doubleValue();
        }

        Integer count = 0;
        if( option.equals("eq")){
            for (Double entry : store.getData().subList(start, store.getData().size())){
                if (entry.equals((double)pattern.getKey())) count++;
            }
        }
        if( option.equals("ne")){
            for (Double entry : store.getData().subList(start, store.getData().size())){
                if (!entry.equals((double)pattern.getKey())) count++;
            }
        }
        if( option.equals("gt")){
            for (Double entry : store.getData().subList(start, store.getData().size())){
                if (entry > (double)pattern.getKey()) count++;
            }
        }
        if( option.equals("ge")){
            for (Double entry : store.getData().subList(start, store.getData().size())){
                if (entry >= (double)pattern.getKey()) count++;
            }
        }
        if( option.equals("lt")){
            for (Double entry : store.getData().subList(start, store.getData().size())){
                if (entry < (double)pattern.getKey()) count++;
            }
        }
        if( option.equals("le")){
            for (Double entry : store.getData().subList(start, store.getData().size())){
                if (entry <= (double)pattern.getKey()) count++;
            }
        }
        if( option.equals("band")){
            for (Double entry : store.getData().subList(start, store.getData().size())){
                if ((entry.intValue() & pattern.getValue()) == pattern.getKey()) count++;
            }
        }
        return count.doubleValue();
    }
    public Double date(String dummy){
        SimpleDateFormat ft = new SimpleDateFormat("yMMd");

        return Double.parseDouble(ft.format(dNow));
    }
    public Double dayofmonth(String dummy){
        SimpleDateFormat ft = new SimpleDateFormat("d");

        return Double.parseDouble(ft.format(dNow));
    }
    public Double dayofweek(String dummy){
        SimpleDateFormat ft = new SimpleDateFormat("u");

        return Double.parseDouble(ft.format(dNow));
    }
    public Double delta(String value) throws NotValidParametr {
        return this.max(value) - this.min(value);
    }
    public int diff(String dummy) throws NotValidParametr {
        return prev("") == last("") ? 1 : 0;
    }

    public Double last() throws NotValidParametr {return last("#1");}
    public Double last(String value) throws NotValidParametr {
        if(value.equals(" ")) return last("#1");
        String syntax = "^\\s?(?:(#\\d+)|(\\d+[dhm]?))?\\s?$";
        Pattern syntaxPattern = Pattern.compile(syntax);
        Matcher syntaxM = syntaxPattern.matcher(value);
        if (!syntaxM.find()) throw new NotValidParametr();
        value = value.replace(" ", "");
        Integer start = collectIndex(value);
        if (start == null) start = collectTime(value);
        if (start == null) start = 1;
        if (start >= store.getData().size()) start = store.getData().size();
        return store.getData().get(store.getData().size()-start);
    }
    public Double max(String value) throws NotValidParametr {
        String syntax = "^\\s?(?:(#\\d+)|(\\d+[dhm]?))\\s?$";
        Pattern syntaxPattern = Pattern.compile(syntax);
        Matcher syntaxM = syntaxPattern.matcher(value);
        if (!syntaxM.find()) throw new NotValidParametr();
        value = value.replace(" ", "");
        Integer start = collectIndex(value);
        if (start == null) start = collectTime(value);
        start = store.getData().size() - start;
        if (start < 0) start = 0;
        return Collections.max(store.getData().subList(start, store.getData().size()));
    }
    public Double min(String value) throws NotValidParametr {
        String syntax = "^\\s?(?:(#\\d+)|(\\d+[dhm]?))\\s?$";
        Pattern syntaxPattern = Pattern.compile(syntax);
        Matcher syntaxM = syntaxPattern.matcher(value);
        if (!syntaxM.find()) throw new NotValidParametr();
        value = value.replace(" ", "");
        Integer start = collectIndex(value);
        if (start == null) start = collectTime(value);
        start = store.getData().size() - start;
        if (start < 0) start = 0;
        return Collections.min(store.getData().subList(start, store.getData().size()));
    }
    public Double now(String dummy){
        return dNow.getTime()/1000.0;
    }
    public Double percentile(String value) throws NotValidParametr {
        String syntax = "^\\s?(?:(#\\d+)|(\\d+[dhm]?))\\s?,\\s?(\\d+(.\\d{0,4})?)\\s?$";
        Pattern syntaxPattern = Pattern.compile(syntax);
        Matcher syntaxM = syntaxPattern.matcher(value);
        if (!syntaxM.find()) throw new NotValidParametr();
        value = value.replace(" ", "");
        Integer start = collectIndex(syntaxM.group(1));
        if (start == null) start = collectTime(syntaxM.group(2));
        Double percentile = collectNumber(syntaxM.group(3));
        start = store.getData().size() - start;
        if (start < 0) start = 0;
        List<Double> sublist = store.getData().subList(start, store.getData().size());
        Collections.sort(sublist);
        Double rank = Math.ceil((percentile/100)*sublist.size());
        return sublist.get(rank.intValue()-1);

    }
    public Double prev(String dummy) throws NotValidParametr {
        return last("#2");
    }
    public Double sum(String value) throws NotValidParametr {
        String syntax = "^\\s?(?:(#\\d+)|(\\d+[dhm]?))\\s?$";
        Pattern syntaxPattern = Pattern.compile(syntax);
        Matcher syntaxM = syntaxPattern.matcher(value);
        if (!syntaxM.find()) throw new NotValidParametr();
        value = value.replace(" ", "");
        Integer start = collectIndex(value);
        if (start == null) start = collectTime(value);
        start = store.getData().size() - start;
        if (start < 0) start = 0;
        Double sum = 0.;
        List<Double> subStore = store.getData().subList(start, store.getData().size());
        for(Double one : subStore){
            sum += one;
        }
        return sum;
    }
    public Double time(String dummy){
        SimpleDateFormat ft = new SimpleDateFormat("HHmmss");

        return Double.parseDouble(ft.format(dNow));
    }

    public String getGeneratorsText() {
        return store.getGeneratorsRules();
    }

    public void setDateTime(String datetime) throws NotValidTime {
        if(datetime == null) return;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(this.DATE_FORMAT);
        try{
            dNow = simpleDateFormat.parse(datetime);
        }catch (ParseException e){
            throw new NotValidTime();
        }
    }
}
