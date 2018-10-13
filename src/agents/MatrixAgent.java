package agents;
import hanabAI.*;
import java.util.HashMap;
import java.util.Map;

public class MatrixAgent implements Agent {

    // Control Constants. This determines agent behaviour.
    static final float K_NORMAL_PLAY_LIMIT = 0.85f;
    static final float K_SAFE_PLAY_LIMIT = 1.00f;
    static final float K_DISCARD_LIMIT = 0.80f;
    static final float K_HINT_PLAY_BOOST = 1.35f;

    // Utility Agent Variables. Good to keep track of.
    private int playerIndex = 0;

    @Override
    public Action doAction(State state) {
        try {

            playerIndex = state.getNextPlayer();
            FrozenState frozenState = new FrozenState();
            frozenState.reconstruct(state);

            if (state.getHintTokens() > 0) {
                return getHintAction(state);
            } else {
                return getDiscardAction();
            }
        } catch(IllegalActionException e){
            e.printStackTrace();
            throw new RuntimeException("Something has gone very wrong");
        }
    }

    public String toString(){
        return "MatrixAgent";
    }

    private Action getDiscardAction() throws IllegalActionException {
        Action action = new Action(playerIndex, toString(), ActionType.DISCARD, 0);
        return action;
    }

    // ======================================================================================================
    // Action Shortcuts.
    // ======================================================================================================

    private Action getHintAction(State state) throws IllegalActionException {
        int hintee = (playerIndex+1) % state.getPlayers().length;
        Card[] hand = state.getHand(hintee);

        java.util.Random rand = new java.util.Random();
        int cardIndex = rand.nextInt(hand.length);
        while(hand[cardIndex]==null) cardIndex = rand.nextInt(hand.length);
        Card c = hand[cardIndex];

        boolean[] col = new boolean[hand.length];
        for(int k = 0; k< col.length; k++){
            col[k]=c.getColour().equals((hand[k]==null?null:hand[k].getColour()));
        }
        return new Action(playerIndex, toString(), ActionType.HINT_COLOUR, hintee, col, c.getColour());
    }

    // ======================================================================================================
    // Analyzer and Matrix Generation Logic.
    // ======================================================================================================

    public Map<String, Integer> generateObservedMatrix(FrozenState state, int playerIndex, int offhandIndex)
    {
        // Use all the information we can see to generate a list of all the possible remaining cards.

        CardCounter counter = CardCounter.newDeckCounter();

        for (Card card : state.originalState.getDiscards())
            counter.add(card.getColour(), card.getValue(), -1);

        for (Colour colour : Colour.values()) {
            for (Card card : state.originalState.getFirework(colour))
                counter.add(card.getColour(), card.getValue(), -1);
        }

        int numberOfPlayers = state.originalState.getPlayers().length;
        for (int i = 0; i < numberOfPlayers; i++)
        {
            if (i != playerIndex)
            {
                FrozenCard[] hand = state.hands[i];
                for (FrozenCard card : hand)
                {
                    if ((offhandIndex == i) && !(card.hasReceivedValueHint() && card.hasReceivedColourHint()))
                        continue;
                    counter.add(card.colour, card.value, -1);
                }
            }
        }

        return new HashMap<>(counter.cardMap);
    }

    public float getProbability(FrozenState state, int playerIndex,
                                Colour colour, int value, Colour knownColour, int knownValue,
                                Colour[] notColour, int[] notValue, Map<String, Integer> observedMap)
    {
        // Get the probability of this Colour and Value combination for a card, given what we know.
        CardCounter counter = new CardCounter();

        if (observedMap == null) {
            observedMap = generateObservedMatrix(state, playerIndex, -1);
        } else {
            observedMap = new HashMap<>(observedMap);
        }

        // Eliminate Cards that we know it cannot be.
        for (Colour ci : Colour.values()) {
            for (int i = 1; i < 6; i ++)
            {
                
            }
        }
    }

    public float getPlayRating(FrozenState state, Colour colour, int value)
    {
        return state.isCardPlayable(colour, value) ? 1.0f : 0.0f;
    }

    public float getDiscardRating(FrozenState state, Colour colour, int value)
    {
        return state.getDiscardScore(colour, value);
    }
    
    // ======================================================================================================
    // Hint Analysis Logic.
    // ======================================================================================================
}