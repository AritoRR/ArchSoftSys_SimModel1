package ru.ananev.simmod.ui;

import ru.ananev.simmod.core.SimulationCore;
import ru.ananev.simmod.model.*;

import java.util.Arrays;

public class SimulationApp   {
    public static void main(String[] args) {
        Source source1 = new Source(1, 1.0);
        Source source2 = new Source(2, 0.5);

        Buffer buffer = new Buffer(1, 3);

        Device device1 = new Device(1);
        Device device2 = new Device(2);

        SimulationCore core = new SimulationCore(
                Arrays.asList(source1, source2),
                buffer,
                Arrays.asList(device1, device2)
        );

        core.initialize();
        core.runAutomatic(10.0);
    }
}
