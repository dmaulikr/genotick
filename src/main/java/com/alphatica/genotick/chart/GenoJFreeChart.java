package com.alphatica.genotick.chart;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import com.alphatica.genotick.ui.UserInputOutputFactory;
import com.alphatica.genotick.ui.UserOutput;

import java.util.Map;
import java.util.HashMap;
import static java.lang.String.format;

class GenoJFreeChart implements GenoChart {
    
    private class XYChartDefinition {
        final Map<String, XYSeries> xySeriesMap = new HashMap<String, XYSeries>();
        String xLabel;
        String yLabel;
    }
    
    private final Map<String, XYChartDefinition> xyChartMap = new HashMap<String, XYChartDefinition>();
    private final boolean drawChart;
    private final boolean saveChart;
    private int numOpenedChartFrames;
    
    GenoJFreeChart(final GenoChartMode mode) {
        drawChart = mode.contains(GenoChartMode.DRAW);
        saveChart = mode.contains(GenoChartMode.SAVE);
        numOpenedChartFrames = 0;
        assert(drawChart || saveChart);
    }
    
    @Override
    public void addXYLineChart(
            final String chartTitle, final String xLabel, final String yLabel,
            final String seriesTitle, final double xValue, final double yValue)
    {
        XYChartDefinition chart = xyChartMap.get(chartTitle);
        if (null == chart) {
            chart = new XYChartDefinition();
            chart.xLabel = xLabel;
            chart.yLabel = yLabel;
            xyChartMap.put(chartTitle, chart);
        }
        XYSeries series = chart.xySeriesMap.get(seriesTitle);
        if (null == series) {
            series = new XYSeries(seriesTitle);
            chart.xySeriesMap.put(seriesTitle, series);
        }
        series.add(xValue, yValue);
    }
    
    @Override
    public void plot(final String chartTitle) {
        plotXYChart(chartTitle);
    }
    
    @Override
    public void plotAll() {
        plotXYCharts();
    }
    
    private void plotXYChart(final String title) {
        final XYChartDefinition definition = xyChartMap.remove(title);
        if (null != definition) {
            plotXYChart(title, definition);
        }
    }
    
    private void plotXYCharts() {
        xyChartMap.forEach((title, definition) -> {
            plotXYChart(title, definition);
        });
        xyChartMap.clear();
    }
    
    private void plotXYChart(final String title, final XYChartDefinition definition) {
        // A JFreeChart object cannot be reused for drawing and saving
        // Thus individual objects must be created for each operation to avoid a memory corruption
        if (drawChart) {
            final JFreeChart chartObject = createChartObject(title, definition);
            drawChart(title, chartObject);
        }
        if (saveChart) {
            final JFreeChart chartObject = createChartObject(title, definition);
            saveChart(title, chartObject);
        }
    }
        
    private JFreeChart createChartObject(final String title, final XYChartDefinition definition) {
        final boolean legend = true;
        final boolean tooltips = true;
        final boolean urls = false;
        final XYSeriesCollection collection = new XYSeriesCollection();
        definition.xySeriesMap.forEach((seriesTitle, series) -> {
            collection.addSeries(series);
        });
        final JFreeChart chartObject = ChartFactory.createXYLineChart(
                title, definition.xLabel, definition.yLabel, collection,
                PlotOrientation.VERTICAL, legend, tooltips, urls);
        return chartObject;
    }
    
    private void drawChart(final String title, final JFreeChart chartObject) {
        final ChartFrame frame = new ChartFrame(title, chartObject);
        frame.pack();
        frame.setVisible(true);
        frame.toFront();
        alignChartFrame(frame);
        numOpenedChartFrames++;
    }
    
    private void saveChart(final String title, final JFreeChart chartObject) {
        final String filename = makeFileName(title);
        final File file = new File(filename);
        try {
            ChartUtilities.saveChartAsPNG(file, chartObject, 512, 512);
        }
        catch (IOException exception) {
            final UserOutput userOutput = UserInputOutputFactory.getUserOutput();
            userOutput.infoMessage(format("JFreeChart: Unable to save chart image %s", filename));
        }
    }
    
    private String makeFileName(final String title) {
        return "genotick-" + title.toLowerCase().replace(" ", "-") + ".png";
    }
    
    private void alignChartFrame(final ChartFrame frame) {
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final double pixels = 40.0 * numOpenedChartFrames;
        final double widthPercent = pixels / screenSize.getWidth();
        final double heightPercent = pixels / screenSize.getHeight();
        RefineryUtilities.positionFrameOnScreen(frame, widthPercent, heightPercent);
    }
}
