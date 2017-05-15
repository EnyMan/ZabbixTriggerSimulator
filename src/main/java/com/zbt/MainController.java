package com.zbt;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


import com.zbt.Exceptions.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import org.gillius.jfxutils.chart.ChartPanManager;
import org.gillius.jfxutils.chart.JFXChartUtil;

public class MainController implements Initializable{
    @FXML
    private  TextField datetime;
    @FXML
    private CategoryAxis status;
    @FXML
    private LineChart<Number, String> resultChart;
    @FXML
    private Tab expTab;
    @FXML
    private Tab dataTab;
    @FXML
    private Button analyse;
    @FXML
    private TextArea expression;
    @FXML
    private VBox items;
    @FXML
    private LineChart<Number, Number> chart;
    private RuntimeManager runtimeManager;
    private boolean expressionChanged;

    @FXML
    void resetZoom() {
        chart.getXAxis().setAutoRanging( true );
        chart.getYAxis().setAutoRanging( true );
    }

    @Override
    public void initialize(URL location, final ResourceBundle resources) {
        expressionChanged = false;
        datetime.setTooltip(new Tooltip("Use this to set static datetime. Format \"dd/MM/yyyy HH:mm:ss\". If empty current datetime will be used."));

        status.setCategories(FXCollections.observableArrayList("down","up"));
        analyse.setOnAction(event -> {
            try {
                chart.getData().clear();
                resultChart.getData().clear();
                if (runtimeManager == null || expressionChanged) {
                    runtimeManager = new RuntimeManager(expression.getText(), datetime.getText());
                }
                expression.setText(runtimeManager.getCleanedExp());
                Pair<ArrayList, XYChart.Series> results = runtimeManager.analyze();
                ArrayList<XYChart.Series> itemsData = results.getKey();
                for (XYChart.Series series : itemsData){
                    chart.getData().add(series);
                }
                resultChart.getData().add(results.getValue());
            } catch (EndOfData endOfData) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Generator Error");
                alert.setHeaderText("Some generator is empty.");
                alert.showAndWait();
                return;
            } catch (NotValidGenerator notValidGenerator) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Generator Error");
                alert.setHeaderText("Some generator has invalid syntax.");
                alert.showAndWait();
                return;
            } catch (EmptyExpression ignored) {
                return;
            } catch (NotValidExpression notValidExpression) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Expression error");
                alert.setHeaderText("Not valid expression.");
                alert.showAndWait();
                return;
            } catch (NotSupportedMethod notSupportedMethod) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Expression error");
                alert.setHeaderText("Invalid function in one of the items.");
                alert.showAndWait();
                return;
            } catch (NotValidTime notValidTime) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("DateTime error");
                alert.setHeaderText("Invalid DateTime.");
                alert.showAndWait();
                return;
            }
            if(items.getChildren().isEmpty())generateDataFields();
            else if(items.getChildren().size() > 0) try {
                setItemsGenerators();
            } catch (NotValidTime notValidTime) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("DateTime error");
                alert.setHeaderText("Invalid DateTime.");
                alert.showAndWait();
            }
        });

        dataTab.setOnSelectionChanged(event -> {
            if(!dataTab.isSelected())return;
            if(expression.getText().equals(""))return;
            if(runtimeManager == null || this.expressionChanged){
                try {
                    runtimeManager = new RuntimeManager(expression.getText());
                    expression.setText(runtimeManager.getCleanedExp());
                } catch (EndOfData | NotValidGenerator | EmptyExpression | NotValidExpression ignored) {
                    return;
                } catch (NotValidTime notValidTime) {
                    Alert alert = new Alert(AlertType.WARNING);
                    alert.setTitle("DateTime error");
                    alert.setHeaderText("Invalid DateTime.");
                    alert.showAndWait();
                }
                expressionChanged = false;
            }
            items.getChildren().clear();
            generateDataFields();
        });

        expTab.setOnSelectionChanged(event -> {
            if(!expTab.isSelected())return;
            if(items.getChildren().isEmpty() || expression.getText().equals(""))return;
            if(runtimeManager == null){
                try {
                    runtimeManager = new RuntimeManager(expression.getText());
                } catch (EndOfData | NotValidGenerator | EmptyExpression | NotValidExpression ignored) {
                    return;
                } catch (NotValidTime notValidTime) {
                    Alert alert = new Alert(AlertType.WARNING);
                    alert.setTitle("DateTime error");
                    alert.setHeaderText("Invalid DateTime.");
                    alert.showAndWait();
                }
            }
            try {
                setItemsGenerators();
            } catch (NotValidTime notValidTime) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("DateTime error");
                alert.setHeaderText("Invalid DateTime.");
                alert.showAndWait();
            }
        });

        //Panning works via either secondary (right) mouse or primary with ctrl held down
        ChartPanManager panner = new ChartPanManager( chart );
        panner.setMouseFilter( new EventHandler<MouseEvent>() {
            @Override
            public void handle( MouseEvent mouseEvent ) {
                if ( mouseEvent.getButton() == MouseButton.SECONDARY ||
                        ( mouseEvent.getButton() == MouseButton.PRIMARY &&
                                mouseEvent.isShortcutDown() ) ) {
                    //let it through
                } else {
                    mouseEvent.consume();
                }
            }
        } );
        panner.start();

        //Zooming works only via primary mouse button without ctrl held down
        JFXChartUtil.setupZooming( chart, new EventHandler<MouseEvent>() {
            @Override
            public void handle( MouseEvent mouseEvent ) {
                if ( mouseEvent.getButton() != MouseButton.PRIMARY ||
                        mouseEvent.isShortcutDown() )
                    mouseEvent.consume();
            }
        } );

        JFXChartUtil.addDoublePrimaryClickAutoRangeHandler( chart );

        expression.setOnKeyReleased(event -> this.expressionChanged = true);
    }

    private void generateDataFields(){
        for (Item item: runtimeManager.trigger.getItems()){
            HBox hbox = new HBox();
            hbox.setAlignment(Pos.CENTER);
            Label itemName = new Label(item.getName());
            itemName.setTooltip(new Tooltip("com.zbt.Item name"));
            TextField itemGenerator = new TextField();
            itemGenerator.setTooltip(new Tooltip("Use [] for array, use {} for range,\nyou can connect them by '+' and\nrepeat them by adding '*' and number,\nexample [1,0]*2+{2..5}*5+[3,2]."));
            TextField itemRefresh = new TextField();
            itemRefresh.setTooltip(new Tooltip("How often items gets data. In seconds."));
            itemRefresh.setPrefColumnCount(5);
            itemRefresh.setText(item.getRefresh().toString());
            itemGenerator.setText(item.getGeneratorsText());
            hbox.getChildren().addAll(itemName, itemGenerator, itemRefresh);
            HBox.setHgrow(itemName,Priority.ALWAYS);
            HBox.setMargin(itemName, new Insets(0,5.0,0,2.0));
            HBox.setHgrow(itemRefresh,Priority.NEVER);
            HBox.setMargin(itemRefresh, new Insets(0,5.0,0,2.0));
            HBox.setHgrow(itemGenerator,Priority.ALWAYS);
            HBox.setMargin(itemGenerator, new Insets(0,5.0,0,5.0));
            items.getChildren().addAll(hbox);
            VBox.setMargin(hbox, new Insets(5.0,0,5.0,0));
        }
    }

    private void setItemsGenerators() throws NotValidTime {
        for(Node hBox : items.getChildren()){
            Label itemName =(Label) ((HBox) hBox).getChildren().get(0);
            TextField generatorRule =(TextField) ((HBox) hBox).getChildren().get(1);
            TextField itemRefresh =(TextField) ((HBox) hBox).getChildren().get(2);
            try {
                runtimeManager.setItemGenerator(itemName.getText(), generatorRule.getText(), itemRefresh.getText());
            } catch (EndOfData ignored) {
            } catch (NotValidGenerator notValidGenerator) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("com.zbt.Generator Error");
                alert.setHeaderText("Some generator has invalid syntax.");
                alert.showAndWait();
            }
        }
    }
}
