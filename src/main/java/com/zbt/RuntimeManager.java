package com.zbt;

import com.zbt.Exceptions.*;
import javafx.scene.chart.XYChart;
import javafx.util.Pair;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Created by Mous on 2.4.2017.
 */
public class RuntimeManager {
    // TODO create run function with data from controller
    // TODO create my own trigger class and generate generators
    // TODO evaluate trigger for each step

    Trigger trigger;

    public RuntimeManager(String exp) throws EndOfData, NotValidGenerator, EmptyExpression, NotValidExpression, NotValidTime {
        this.trigger = new Trigger(exp);
        this.trigger.prepare();
    }

    public RuntimeManager(String exp, String datetime) throws EndOfData, NotValidGenerator, EmptyExpression, NotValidExpression, NotValidTime {
        if (datetime == null){
            this.trigger = new Trigger(exp, datetime);
        }else {
            this.trigger = new Trigger(exp);
        }
        this.trigger.prepare();
    }

    public Pair<ArrayList, XYChart.Series> analyze() throws NotSupportedMethod {
        Pair<ArrayList, XYChart.Series> result;
        ArrayList<XYChart.Series> itemData = new ArrayList<>();
        XYChart.Series evaluated = new XYChart.Series<Double, Integer>();
        for (int i = 0; i < this.trigger.getItems().size(); i++) {
            itemData.add(new XYChart.Series<Integer, Double>());
            itemData.get(i).setName(trigger.getItems().toArray()[i].toString());
        }
        trigger.setTime(0);
        int count = 0;
        while (true) {
            try {
                BigDecimal evaulatedExp = this.trigger.evaluateOne();
                if (evaulatedExp == null) continue;
                XYChart.Data<Integer, String> evaulatedData = new XYChart.Data<>(count*trigger.getTime(), translateToString(evaulatedExp.intValue()));
                evaluated.getData().add(evaulatedData);
                for (int j = 0; j < this.trigger.getItemsData().size(); j++) {
                    Double data = this.trigger.getItemsData().get(j);
                    XYChart.Data<Integer, Double> chartData = new XYChart.Data<>(count*trigger.getTime(), data);
                    itemData.get(j).getData().add(chartData);
                }
            } catch (EndOfData endOfData) {
                break;
            }
            count++;
            trigger.setTime(0);
        }

        result = new Pair<>(itemData, evaluated);
        return result;

    }
    public void setItemGenerator(String item, String definition, String refresh) throws EndOfData, NotValidGenerator, NotValidTime {
        if(!refresh.matches("^\\d{1,10}")) throw new NotValidGenerator();
        this.trigger.setItemStore(item, definition, Integer.parseInt(refresh));
    }

    public String getCleanedExp(){
        return trigger.getExp();
    }

    public String translateToString(Integer triggerStatus){
        if (triggerStatus == 0)  return "down";
        else return "up";
    }
}
