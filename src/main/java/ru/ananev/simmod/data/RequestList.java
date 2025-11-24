package ru.ananev.simmod.data;

import java.util.Random;

public class RequestList {
    private static final Random random = new Random();

    public static final String[] FOOD_ITEMS = {
            "Пицца Маргарита",
            "Пицца Пепперони",
            "Бургер Классический",
            "Бургер Чизбургер",
            "Стейк Рибай",
            "Стейк Филе-миньон",
            "Суши Филадельфия",
            "Суши Калифорния",
            "Паста Карбонара",
            "Паста Болоньезе",
            "Салат Цезарь",
            "Салат Греческий",
            "Тайская лапша",
            "Курица терияки",
            "Такос",
            "Буррито",
            "Суп Том-ям",
            "Суп Минестроне",
            "Шашлык",
            "Лаваш",
            "Спринг-роллы",
            "Фондю",
            "Рамен",
            "Пельмени",
            "Хинкали",
            "Гамбургер",
            "Чизкейк",
            "Тирамису",
            "Мороженое",
            "Кока-кола"
    };

    public static String getRandomFood() {
        return FOOD_ITEMS[random.nextInt(FOOD_ITEMS.length)];
    }
}
