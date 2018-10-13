package agents;
import hanabAI.Card;
import hanabAI.Colour;
import java.util.HashMap;
import java.util.Map;

public class CardCounter {
    
    // ======================================================================================================
    // Static Methods. Helps to get reference collections.
    // ======================================================================================================

    private static Map<String, Integer> _deckMap;
    private static Map<String, Integer> _emptyMap;

    static public Map<String, Integer> getDeckMap()
    {
        // Create the deck map if it didn't exist yet.
        if (_deckMap == null) {
            _deckMap = new HashMap<>();
            Card[] deck = Card.getDeck();
            for (Card card : deck)
            {
                String key = CardUtil.getKey(card);
                _deckMap.put(key, _deckMap.getOrDefault(key, 0) + 1);
            }
        }

        // Return a copy of the deck map, if it already exists.
        return new HashMap<>(_deckMap);
    }

    static public Map<String, Integer> getEmptyMap()
    {
        // Create the empty map if it didn't exist yet.
        if (_emptyMap == null) {
            _emptyMap = new HashMap<>();
            for (Colour colour : Colour.values())
            {
                for (int v = 1; v < 6; v++)
                {
                    String key = CardUtil.getKey(colour, v);
                    _emptyMap.put(key, 0);
                }
            }
        }

        // Return a copy of the empty map, if it already exists.
        return new HashMap<>(_emptyMap);
    }
    
    // ======================================================================================================
    // Static Utility Functions.
    // ======================================================================================================

    public static CardCounter newDeckCounter() {
        CardCounter counter = new CardCounter();
        counter.cardMap = getDeckMap();
        return counter;
    }

    // ======================================================================================================
    // Class Implementation.
    // ======================================================================================================
    public Map<String, Integer> cardMap;

    public CardCounter() {
        cardMap = getEmptyMap();
    }

    public void add(Colour colour, int value, int amount)
    {
        add(CardUtil.getKey(colour, value), amount);
    }

    public void add(String key, int amount)
    {
        cardMap.put(key, cardMap.get(key) + amount);
    }

    public int count(Colour colour, int value)
    {
        return count(CardUtil.getKey(colour, value));
    }

    public int count(String key)
    {
        return cardMap.get(key);
    }

    public int totalCount()
    {
        // Count the total number of cards tracked in this counter.
        int count = 0;
        for (String key : cardMap.keySet()) {
            count += cardMap.get(key);
        }
        return count;
    }
}
