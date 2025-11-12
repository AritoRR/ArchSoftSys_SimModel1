package ru.ananev.simmod.ui;

import ru.ananev.simmod.core.SimulationCore;
import ru.ananev.simmod.model.*;

import java.util.Arrays;

public class SimulationApp   {
    public static void main(String[] args) {
        // Создаем компоненты системы
        Source source1 = new Source(1, 1.0); // Источник 1 с интенсивностью 1.0
        Source source2 = new Source(2, 0.5); // Источник 2 с интенсивностью 0.5

        Buffer buffer = new Buffer(1, 3); // Буфер ID=1, размер=3

        Device device1 = new Device(1); // Прибор 1
        Device device2 = new Device(2); // Прибор 2

        // Создаем ядро симуляции
        SimulationCore core = new SimulationCore(
                Arrays.asList(source1, source2),
                buffer,
                Arrays.asList(device1, device2)
        );

        // Инициализируем и запускаем
        core.initialize();
        core.run(10.0); // Запуск на 100 единиц времени

        // Можно также использовать пошагово:
        // core.initialize();
        // while (core.step()) {
        //     // Делать что-то на каждом шаге
        // }
    }
}
