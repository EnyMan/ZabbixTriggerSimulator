import com.zbt.*;
import com.zbt.Exceptions.*;
import javafx.util.Pair;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static com.zbt.utils.StringUtils.*;
/**
 * Created by Mous on 22.1.2017.
 */
public class exprtest {

    @Test
    public void testBasicEprClean() {
        String cleaned = null;
        cleaned = Trigger.clean("        not  {www.zabbix.com:system.cpu.load[all,avg1].last()}>5   or   {www.zabbix.com:system.cpu.load[all,avg1].min(10m)}>2      ");
        assertEquals("not({www.zabbix.com:system.cpu.load[all,avg1].last()}>5) || {www.zabbix.com:system.cpu.load[all,avg1].min(10m)}>2", cleaned);
    }

    @Test
    public void testEprClean() {
        String cleaned = null;
        cleaned = Trigger.clean("        not  ({www.zabbix.com:system.cpu.load[all,avg1].last()}>5   or   {www.zabbix.com:system.cpu.load[all,avg1].min(10m)}>2)      ");
        assertEquals("not(({www.zabbix.com:system.cpu.load[all,avg1].last()}>5 || {www.zabbix.com:system.cpu.load[all,avg1].min(10m)}>2))", cleaned);
    }

    @Test
    public void testEprFindBrackets() {
        Pair<Integer, Integer> indexes = findBrackets("(()((()())((((()))))))");
        assertEquals("0=21", indexes.toString());
    }

    @Test
    public void testStringPlaceAtBegin() {
        String placed = placeCharAt("aaaa", 0, 'b');
        assertEquals("baaa", placed);
    }

    @Test
    public void testStringPlaceAtMiddle() {
        String placed = placeCharAt( "aaaa",2,'b');
        assertEquals("aaba", placed);
    }

    @Test
    public void testStringPlaceAtEnd() {
        String placed = placeCharAt( "aaaa",4,'b');
        assertEquals("aaaab", placed);
    }

    @Test
    public void testCreatingDataStore(){
        DataStore store = null;
        try {
            store = new DataStore("[123]*2+{13..20}*3+[10,20,30,2,1]");
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
        assert store != null;
        assertEquals(3, store.getGenerators().size());
        String rules = store.getGeneratorsRules();
        assertEquals("[123]*2+{13..20}*3+[10,20,30,2,1]" ,rules);
    }

    @Test
    public void testCreatingNonSenseDataStore(){
        DataStore store = null;
        try {
            store = new DataStore("2{13..20}*3");
        } catch (NotValidGenerator notValidGenerator) {
            assert true;
        }
        assertNull(store);
        try {
            store = new DataStore(" 2[1,0]*5");
        } catch (NotValidGenerator notValidGenerator) {
            assert true;
        }
        assertNull(store);
    }

    @Test
     public void testRangeGenTick(){
        RangeGen gen = null;
        try {
            gen = new RangeGen("{1..5}*2");
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
        assert 1.0 == gen.tick();
        for(int i = 0; i < 8; i++)gen.tick();
        assertEquals( 5.0, gen.tick(),0);
    }

    @Test
    public void testRangeGenTickEnd(){
        RangeGen gen = null;
        try {
            gen = new RangeGen("{1..3}*1");
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
        assert 1.0 == gen.tick();
        for(int i = 0; i < 10; i++)gen.tick();
        assertNull(gen.tick());
    }

    @Test
    public void testArrayGenTick(){
        ArrayGen gen = null;
        try {
            gen = new ArrayGen("[1,5,8,9,123,45.5,6,8,0]*3");
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
        assert 1.0 == gen.tick();
        for(int i = 0; i < 12; i++)gen.tick();
        assert 123.0 == gen.tick();
    }

    @Test
    public void testExprPrepare(){
        try {
            Trigger trigger = new Trigger("        not  {www.zabbix.com:system.cpu.load[all,avg1].last()}>5   or   {www.zabbix.com:system.cpu.load[all,avg1].min(10m)}>2      ");
            trigger.prepare();
            assertEquals(1, trigger.getGenerators().size());
            assertEquals("        not  {www.zabbix.com:system.cpu.load[all,avg1].last()}>5   or   {www.zabbix.com:system.cpu.load[all,avg1].min(10m)}>2      ", trigger.getExp());
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        } catch (EmptyExpression emptyExpression) {
            emptyExpression.printStackTrace();
            assert false;
        } catch (NotValidExpression notValidExpression) {
            notValidExpression.printStackTrace();
            assert false;
        } catch (NotValidTime notValidTime) {
            notValidTime.printStackTrace();
            assert false;
        }

    }

    @Test
    public void testExprSetStore() {
        try {
            Trigger trigger = new Trigger("        not  {www.zabbix.com:system.cpu.load[all,avg1].last()}>5   or   {www.zabbix.com:system.cpu.load[all,avg1].min(10m)}>2      ");
            trigger.prepare();
            trigger.setItemStore("www.zabbix.com:system.cpu.load[all,avg1]", "[1,2,3,4,5]*2");
            ArrayGen expect = new ArrayGen("[1,2,3,4,5]*2");
            int indexOfGen = trigger.getGenerators().indexOf(Generator.decide("[1,2,3,4,5]*2"));
            assertEquals(expect, trigger.getGenerators().get(indexOfGen));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        } catch (EmptyExpression emptyExpression) {
            emptyExpression.printStackTrace();
            assert false;
        } catch (NotValidExpression notValidExpression) {
            notValidExpression.printStackTrace();
            assert false;
        } catch (NotValidTime notValidTime) {
            notValidTime.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testExprEvaluateOne(){

        BigDecimal expect = new BigDecimal(1);
        try {
            Trigger trigger = new Trigger("        not  {www.zabbix.com:system.cpu.load[all,avg1].last()}>5   or   {www.zabbix.com:system.cpu.load[all,avg1].min(10m)}>2      ");
            trigger.prepare();
            trigger.setItemStore("www.zabbix.com:system.cpu.load[all,avg1]", "[1,2,3,4,5]");
            for(int i = 0; i < 30; i++)trigger.evaluateOne();
            assertEquals(expect, trigger.evaluateOne());
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        } catch (EmptyExpression emptyExpression) {
            emptyExpression.printStackTrace();
            assert false;
        } catch (NotValidExpression notValidExpression) {
            notValidExpression.printStackTrace();
            assert false;
        } catch (NotSupportedMethod notSupportedMethod) {
            notSupportedMethod.printStackTrace();
            assert false;
        } catch (NotValidTime notValidTime) {
            notValidTime.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testExprItemNames(){
        try {
            Trigger trigger = new Trigger("not{www.zabbix.com:system.cpu.load[all,avg1].last()}>5 or {www.zabbix.com:system.cpu.load[all,avg1].min(10m)}>2");
            trigger.prepare();
            Trigger triggerWhiteSpace = new Trigger("not{www.zabbix.com:system.cpu.load[all, avg1].last()}>5 or {www.zabbix.com:system.cpu.load[all,avg1].min(10m)}>2");
            triggerWhiteSpace.prepare();
            assertEquals(trigger.getItems().size(),triggerWhiteSpace.getItems().size());
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        } catch (EmptyExpression emptyExpression) {
            emptyExpression.printStackTrace();
            assert false;
        } catch (NotValidExpression notValidExpression) {
            notValidExpression.printStackTrace();
            assert false;
        } catch (NotValidTime notValidTime) {
            notValidTime.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testExprEvaluateOneMacro(){

        BigDecimal expect = new BigDecimal(1);
        try {
            Trigger trigger = new Trigger("        not  {TRIGGER.VALUE}=1   or   {www.zabbix.com:system.cpu.load[all,avg1].min(10m)}>2      ");
            trigger.prepare();
            trigger.setItemStore("www.zabbix.com:system.cpu.load[all,avg1]", "[1,2,3,4,5]");
            for(int i = 0; i < 30; i++)trigger.evaluateOne();
            assertEquals(expect, trigger.evaluateOne());
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        } catch (EmptyExpression emptyExpression) {
            emptyExpression.printStackTrace();
            assert false;
        } catch (NotValidExpression notValidExpression) {
            notValidExpression.printStackTrace();
            assert false;
        } catch (NotSupportedMethod notSupportedMethod) {
            notSupportedMethod.printStackTrace();
            assert false;
        } catch (NotValidTime notValidTime) {
            notValidTime.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testExprEvaluate(){

        BigDecimal expect = new BigDecimal(0);
        try {
            Trigger trigger = new Trigger("       {www.zabbix.com:system.cpu.load[all,avg1].last()}>5   or   {www.zabbix.com:system.cpu.load[all,avg1].min(10m)}>2      ");
            trigger.prepare();
            trigger.setItemStore("www.zabbix.com:system.cpu.load[all,avg1]", "[1,2,3,4,5]+{1..3}");
            for(int i = 0; i < 6*30; i++)trigger.evaluateOne();
            assertEquals(expect, trigger.evaluateOne());
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        } catch (EmptyExpression emptyExpression) {
            emptyExpression.printStackTrace();
            assert false;
        } catch (NotValidExpression notValidExpression) {
            notValidExpression.printStackTrace();
            assert false;
        } catch (NotSupportedMethod notSupportedMethod) {
            notSupportedMethod.printStackTrace();
            assert false;
        } catch (NotValidTime notValidTime) {
            notValidTime.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testExprGetItemData(){
        ArrayList<Double> expect = new ArrayList<>();
        expect.add(0.0);
        try {
            Trigger trigger = new Trigger("       {www.zabbix.com:system.cpu.load[all,avg1].last()}>5   or   {www.zabbix.com:system.cpu.load[all,avg1].min(10m)}>2      ");
            trigger.prepare();
            for(int i = 0; i < 6*30-1; i++)trigger.evaluateOne();
            assertEquals(expect, trigger.getItemsData());
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        } catch (EmptyExpression emptyExpression) {
            emptyExpression.printStackTrace();
            assert false;
        } catch (NotValidExpression notValidExpression) {
            notValidExpression.printStackTrace();
            assert false;
        } catch (NotSupportedMethod notSupportedMethod) {
            notSupportedMethod.printStackTrace();
            assert false;
        } catch (NotValidTime notValidTime) {
            notValidTime.printStackTrace();
            assert false;
        }
    }

    @Test()
    public void testExprEvaluateEmpty(){
        try {
            assertNull( new Trigger(""));
        } catch (EmptyExpression emptyExpression) {
            assert true;
        }
    }

    @Test()
    public void testExprEvaluateNull(){

        try {
            assertNull(new Trigger(null));
        } catch (EmptyExpression emptyExpression) {
            assert true;
        }
    }
    @Test()
    public void testExprEvaluateNotExpression(){
        try {
            Trigger trigger = new Trigger("not expression");
            trigger.prepare();
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        } catch (EmptyExpression emptyExpression) {
            emptyExpression.printStackTrace();
            assert false;
        } catch (NotValidExpression notValidExpression) {
            assert true;
        } catch (NotValidTime notValidTime) {
            notValidTime.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testItemEquals(){
        try {
            Item testItem = new Item("abc");
            Item testItem2 = new Item("abc");
            Item testItem3 = new Item("cba");
            assertEquals(testItem, testItem2);
            assertFalse(testItem.equals(testItem3));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testItemByName(){
        try {
            Item testItem = new Item("abc");
            assertEquals("1.0", testItem.byName("last","#1"));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        } catch (NotSupportedMethod notSupportedMethod) {
            notSupportedMethod.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testItemByNameNotMethod(){
        try {
            Item testItem = new Item("abc");
            assertEquals("1.0", testItem.byName("nonsense","#1"));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        } catch (NotSupportedMethod notSupportedMethod) {
            assert true;
        }
    }

    @Test
    public void testItemCollectIndex(){
        try {
            Item testItem = new Item("abc");
            assertEquals((Integer)2, testItem.collectIndex("#2"));
            assertNull(testItem.collectIndex("123"));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testItemCollectTime(){
        try {
            Item testItem = new Item("abc");
            assertEquals((Integer)(123/30), testItem.collectTime("123"));
            assertEquals((Integer)(180/30), testItem.collectTime("3m"));
            assertEquals((Integer)(7200/30), testItem.collectTime("2h"));
            assertEquals((Integer)(86400/30), testItem.collectTime("1d"));
            assertNull(testItem.collectTime("#123"));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testItemCollectPattern(){
        Pair<Integer,Integer> exceptOne = new Pair<>(2,null);
        Pair<Integer,Integer> exceptTwo = new Pair<>(2,3);
        try {
            Item testItem = new Item("abc");
            assertEquals(exceptOne, testItem.collectPattern("2"));
            assertEquals(exceptTwo, testItem.collectPattern("2/3"));
            assertNull(testItem.collectPattern("abc"));
            assertNull(testItem.collectPattern(null));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testItemCollectNumber(){
        try {
            Item testItem = new Item("abc");
            assertEquals((Double)2.0, testItem.collectNumber("2"));
            assertNull(testItem.collectNumber("#123"));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testItemCollectOption(){
        try {
            Item testItem = new Item("abc");
            assertEquals("eq", testItem.collectOption(null));
            assertEquals("eq", testItem.collectOption(""));
            assertEquals("eq", testItem.collectOption("\"eq\""));
            assertEquals("ne", testItem.collectOption("\"ne\""));
            assertEquals("gt", testItem.collectOption("\"gt\""));
            assertEquals("ge", testItem.collectOption("\"ge\""));
            assertEquals("lt", testItem.collectOption("\"lt\""));
            assertEquals("le", testItem.collectOption("\"le\""));
            assertEquals("band", testItem.collectOption("\"band\""));
            assertNull(testItem.collectNumber("#123"));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testItemTime(){
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("HHmmss");
        try {
            Item testItem = new Item("abc");
            assertEquals(Double.parseDouble(ft.format(dNow)), testItem.time(""),0);
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testItemCustomTime(){
        try {
            Item testItem = new Item("abc");
            testItem.setDateTime("24/03/2013 21:54:00");
            assertEquals((Double)215400.0, testItem.time(""),0);
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        } catch (NotValidTime notValidTime) {
            notValidTime.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testItemDate(){
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yMMd");
        try {
            Item testItem = new Item("abc");
            assertEquals(Double.parseDouble(ft.format(dNow)),testItem.date(""),0);
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testItemCustomDate(){
        try {
            Item testItem = new Item("abc");
            testItem.setDateTime("24/03/2013 21:54:00");
            assertEquals((Double)20130324.0, testItem.date(""),0);
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
            assert false;
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
            assert false;
        } catch (NotValidTime notValidTime) {
            notValidTime.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testItemDayOfMonth(){
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("d");
        try {
            Item testItem = new Item("abc");
            assertEquals(Double.parseDouble(ft.format(dNow)),testItem.dayofmonth(""),0);
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
    }

    @Test
    public void testItemDayOfWeek(){
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("u");
        try {
            Item testItem = new Item("abc");
            assertEquals(Double.parseDouble(ft.format(dNow)), testItem.dayofweek(""),0);
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
    }

    @Test
    public void testItemNow(){
        Date dNow = new Date();
        Double now = dNow.getTime()/1000.0;
        try {
            Item testItem = new Item("abc");
            assertEquals(now,testItem.now(""),1);
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
    }

    @Test
    public void testItemSum(){
        try {
            Item testItem = new Item("abc");
            for(int i = 0; i < 100; i++)testItem.tick();
            assertEquals((Double) 1.0, testItem.sum("#2"));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidParametr notValidParametr) {
            notValidParametr.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
    }

    @Test
    public void testItemMin(){
        try {
            Item testItem = new Item("abc");
            for(int i = 0; i < 100; i++)testItem.tick();
            assertEquals((Double) 0.0, testItem.min("#2"));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidParametr notValidParametr) {
            notValidParametr.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
    }

    @Test
    public void testItemMax(){
        try {
            Item testItem = new Item("abc");
            for(int i = 0; i < 100; i++)testItem.tick();
            assertEquals((Double) 1.0, testItem.max("#2"));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidParametr notValidParametr) {
            notValidParametr.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
    }

    @Test
    public void testItemLast(){
        try {
            Item testItem = new Item("abc");
            testItem.tick();
            assertEquals((Double) 1.0, testItem.last());
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidParametr notValidParametr) {
            notValidParametr.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
    }

    @Test
    public void testItemAvg(){
        try {
            Item testItem = new Item("abc");
            for(int i = 0; i < 100; i++)testItem.tick();
            assertEquals((Double) 0.5, testItem.avg("#4"));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidParametr notValidParametr) {
            notValidParametr.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
    }

    @Test
    public void testItemBand(){
        try {
            Item testItem = new Item("abc","[100,50]*5");
            for(int i = 0; i < 100; i++)testItem.tick();
            assertEquals((Double) 4.0, testItem.band("#2,132"));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidParametr notValidParametr) {
            notValidParametr.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
    }

    @Test
    public void testItemChange(){
        try {
            Item testItem = new Item("abc","[100,50]*5");
            for(int i = 0; i < 100; i++)testItem.tick();
            assertEquals((Double) (-50.0), testItem.change(""));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidParametr notValidParametr) {
            notValidParametr.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
    }

    @Test
    public void testItemAbsChange(){
        try {
            Item testItem = new Item("abc","[100,50]*5");
            for(int i = 0; i < 100; i++)testItem.tick();
            assertEquals((Double) 50.0, testItem.abschange(""));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidParametr notValidParametr) {
            notValidParametr.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
    }

    @Test
    public void testItemDelta(){
        try {
            Item testItem = new Item("abc","[100,22]*5");
            for(int i = 0; i < 100; i++)testItem.tick();
            assertEquals((Double) 78.0, testItem.delta("#4"));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidParametr notValidParametr) {
            notValidParametr.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
    }

    @Test
    public void testItemDiff(){
        try {
            Item testItem = new Item("abc","[100,50]*5");
            for(int i = 0; i < 100; i++)testItem.tick();
            assertEquals(0, testItem.diff(""));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidParametr notValidParametr) {
            notValidParametr.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
    }

    @Test
    public void testItemCount(){
        try {
            Item testItem = new Item("abc","[1,2,3,2,1]*5");
            for(int i = 0; i < 100; i++)testItem.tick();
            assertEquals((Double) 4.0, testItem.count("#5"));
            assertEquals((Double) 2.0, testItem.count("#5,2"));
            assertEquals((Double) 2.0, testItem.count("#5,2,\"ne\""));
            assertEquals((Double) 3.0, testItem.count("#5,1,\"gt\""));
            assertEquals((Double) 4.0, testItem.count("#5,1,\"ge\""));
            assertEquals((Double) 3.0, testItem.count("#5,3,\"lt\""));
            assertEquals((Double) 4.0, testItem.count("#5,3,\"le\""));
            assertEquals((Double) 2.0, testItem.count("#5,2/3,\"band\""));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidParametr notValidParametr) {
            notValidParametr.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
    }

    @Test
    public void testItemPercentile(){
        try {
            Item testItem = new Item("abc","[15,20,35,40,50]*5");
            for(int i = 0; i < 130; i++)testItem.tick();
            assertEquals((Double) 15.0, testItem.percentile("#5,5"));
            assertEquals((Double) 20.0, testItem.percentile("#5,30"));
            assertEquals((Double) 20.0, testItem.percentile("#5,40"));
            assertEquals((Double) 35.0, testItem.percentile("#5,50"));
            assertEquals((Double) 50.0, testItem.percentile("#5,100"));
        } catch (EndOfData endOfData) {
            endOfData.printStackTrace();
        } catch (NotValidParametr notValidParametr) {
            notValidParametr.printStackTrace();
        } catch (NotValidGenerator notValidGenerator) {
            notValidGenerator.printStackTrace();
        }
    }
}
