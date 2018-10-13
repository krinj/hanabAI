package agents;
import hanabAI.Card;
import hanabAI.Colour;

class CardUtil {
    static String getKey(Card card)
    {
        // Get the hash-key for a card for quick map lookups.
        return card.getColour().toString() + card.getValue();
    }

    static String getKey(Colour color, int value)
    {
        // Get the hash-key for a card for quick map lookups.
        return color.toString() + value;
    }
}
