package tests;

import agents.CardCounter;

import java.util.Map;

public class TestCardMatrix {
    public static void main(String[] args)
    {
        CardCounter counter = new CardCounter();
        Map<String, Integer> deckMap = CardCounter.getDeckMap();
        Map<String, Integer> emptyMap = CardCounter.getEmptyMap();
        System.out.println(deckMap);
        System.out.println(emptyMap);
    }
}
