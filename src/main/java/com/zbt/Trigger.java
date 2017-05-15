package com.zbt;

import com.udojava.evalex.Expression;
import com.zbt.Exceptions.*;
import javafx.util.Pair;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zbt.utils.StringUtils.findBrackets;
import static com.zbt.utils.StringUtils.placeCharAt;

/**
 * Created by Mous on 18.12.2016.
 */

public class Trigger {
    String exp;
    String expOriginal;
    Map<String,Item> items;
    Integer status;
    Integer time;
    String datetime;

    public Trigger(String rule) throws EmptyExpression {
        if(rule == null || rule.isEmpty()){
            throw new EmptyExpression();
        }
        exp = clean(rule);
        expOriginal = rule;
        items = new HashMap<>();
        time = 0;
        status = 0;
    }

    public Trigger(String rule, String datetime) throws EmptyExpression {
        if(rule == null || rule.isEmpty()){
            throw new EmptyExpression();
        }
        exp = clean(rule);
        expOriginal = rule;
        items = new HashMap<>();
        this.datetime = datetime;
        time = 0;
        status = 0;
    }

    public ArrayList<Generator> getGenerators() {
        ArrayList<Generator> generators = new ArrayList<Generator>();
        for (Map.Entry<String, Item> item : items.entrySet()){
            generators.addAll(item.getValue().getGenerators());
        }
        return generators;
    }

    public Collection<Item> getItems() {
        return items.values();
    }

    public String getExp() {
        return expOriginal;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public void setItemStore(String name, String store, Integer refresh) throws EndOfData, NotValidGenerator, NotValidTime {
        Item newStore = new Item(name, store, refresh);
        newStore.setDateTime(datetime);
        items.replace(name, newStore);
    }

    public void setItemStore(String name, String store) throws EndOfData, NotValidGenerator, NotValidTime {
        this.setItemStore(name, store, 30);
    }

    public static String clean(String exp) {
        String pattern;
        Pattern r;
        Matcher m;

        //replace ors and ands
        exp = exp.replace("or", "||");
        exp = exp.replace("and", "&&");

        // removing unnecessary spaces
        exp = exp.replaceAll("\\s+", " ");
        exp = exp.replaceAll("\\s+$", "");
        exp = exp.replaceAll("^\\s+", "");

        //adding brackets for not
        pattern= "not\\s+\\(";
        r = Pattern.compile(pattern);
        m = r.matcher(exp);
        if(m.find()){
            Pair startEnd = findBrackets(exp, m.end() - 1);
            exp = placeCharAt(exp,(Integer) startEnd.getKey() -1, '(');
            exp = placeCharAt(exp,(Integer) startEnd.getValue() + 1, ')');
        } else {
            pattern = "not\\s+(\\{.*?}(?:\\*|/|\\+|<>|<=|>=|<|>|=|-)\\d+\\.?\\d*)";
            r = Pattern.compile(pattern);
            m = r.matcher(exp);
            exp = m.replaceAll("not($1)");
        }

        return exp;
    }

    public void prepare() throws EndOfData, NotValidGenerator, NotValidExpression, NotValidTime {
        String evaluated = exp;
        String pattern = "(\\{.*?\\})";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(evaluated);
        while (m.find()) {
            String found = m.group(0);
            if (found.equals("{TRIGGER.VALUE}")){
                continue;
            }
            create_item(found);
        }
        if(items.isEmpty()){
            throw new NotValidExpression();
        }
    }

    public void create_item(String definition) throws EndOfData, NotValidGenerator, NotValidTime {

        String itemName = definition.substring(1,definition.lastIndexOf("."));
        String itemNameCleaned = itemName.replace(" ", "");
        this.exp = this.exp.replace(itemName, itemNameCleaned);
        this.expOriginal = this.expOriginal.replace(itemName, itemNameCleaned);
        Item  t = new Item(itemNameCleaned);
        t.setDateTime(datetime);
        items.put(itemNameCleaned,t);
    }

    public void tickAll() throws EndOfData {
        for(Item item: items.values()){
            item.tick();
        }
        time++;
    }

    public BigDecimal evaluateOne() throws EndOfData, NotSupportedMethod {
        String evaluated = exp;
        String pattern = "(\\{.*?\\})";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(evaluated);
        tickAll();
        boolean updatedItem = false;
        for (Item item: items.values()){
            if (item.isUpdated()) updatedItem = true; break;
        }
        if(!updatedItem)return null;
        while (m.find()) {
            String found = m.group(0);
            if (found.equals("{TRIGGER.VALUE}")){
                evaluated = evaluated.replace( found, this.status.toString());
                continue;
            }
            String fn = found.substring(found.lastIndexOf(".")+1, found.length()-1);
            String params = fn.substring(fn.indexOf("(") + 1, fn.length() - 1);
            fn = fn.substring(0,fn.indexOf("("));
            String item = found.substring(1, found.lastIndexOf("."));
            String returnedValue = items.get(item).byName(fn,params);
            evaluated = evaluated.replace( found, returnedValue);
        }
        Expression expr = new Expression(evaluated);
        return expr.eval();

    }

    public ArrayList<Double> getItemsData() {
        ArrayList<Double> data = new ArrayList<>();
        for (Item item : items.values()){
            data.add(item.getLastData());
        }
        return data;
    }
}
