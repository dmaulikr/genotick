package com.alphatica.genotick.ui;

import com.alphatica.genotick.data.MainAppData;
import com.alphatica.genotick.genotick.Main;
import com.alphatica.genotick.genotick.MainSettings;
import com.alphatica.genotick.genotick.RandomGenerator;
import com.alphatica.genotick.timepoint.TimePoint;
import com.alphatica.genotick.breeder.InheritedWeightMode;
import com.alphatica.genotick.chart.GenoChartMode;

import java.util.Random;

@SuppressWarnings("unused")
class RandomParametersInput extends BasicUserInput {
    private Random random = RandomGenerator.get();
    private final UserOutput output = UserInputOutputFactory.getUserOutput();

    @Override
    public MainSettings getSettings() {
        MainSettings defaults = MainSettings.getSettings();
        defaults.populationDAO = "";
        defaults.requireSymmetricalRobots = true;
        defaults.killNonPredictingRobots = true;
        defaults.performTraining = true;
        defaults.chartMode = GenoChartMode.NONE;
        MainAppData data = getData(Main.DEFAULT_DATA_DIR);
        assignTimePoints(defaults, data);
        return assignRandom(defaults);
    }

    private void assignTimePoints(MainSettings defaults, MainAppData data) {
        TimePoint first = data.getFirstTimePoint();
        TimePoint last = data.getLastTimePoint();
        long diff = last.getValue() - first.getValue();
        long count = Math.abs(random.nextLong() % diff);
        defaults.startTimePoint = new TimePoint(last.getValue() - count);
        defaults.endTimePoint = last;
    }
    
    private <E extends Enum<E>> E nextEnum(Class<E> enumType) {
        final E[] enumValues = enumType.getEnumConstants();
        final int index = random.nextInt(enumValues.length);
        return enumValues[index];
    }

    private MainSettings assignRandom(MainSettings settings) {

        settings.dataMaximumOffset = random.nextInt(256) + 1;
        settings.processorInstructionLimit = random.nextInt(256) + 1;
        settings.maximumDeathByAge = random.nextDouble();
        settings.maximumDeathByWeight = random.nextDouble();
        settings.probabilityOfDeathByAge = random.nextDouble();
        settings.probabilityOfDeathByWeight = random.nextDouble();
        settings.inheritedChildWeight = random.nextDouble();
        settings.inheritedChildWeightMode = nextEnum(InheritedWeightMode.class);
        settings.protectRobotsUntilOutcomes = random.nextInt(100);
        settings.protectBestRobots = random.nextDouble();
        settings.newInstructionProbability = random.nextDouble();
        settings.instructionMutationProbability = random.nextDouble();
        settings.skipInstructionProbability = settings.newInstructionProbability;
        settings.minimumOutcomesToAllowBreeding = random.nextInt(50);
        settings.minimumOutcomesBetweenBreeding = random.nextInt(50);
        settings.randomRobotsAtEachUpdate = random.nextDouble();
        settings.resultThreshold = 1 + (random.nextDouble() * 9);
        return settings;

    }

}
