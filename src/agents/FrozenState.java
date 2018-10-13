package agents;
import hanabAI.*;
import java.util.*;

class FrozenCard {

    Colour colour = null;
    int value = -1;

    public boolean sealed = false;

    private int valueHintCounter = 0;
    private int colourHintCounter = 0;

    ArrayList<Colour> notColour = new ArrayList<>();
    ArrayList<Integer> notValue = new ArrayList<>();

    public void receiveColourHint(Colour colour)
    {
        colourHintCounter++;
        this.colour = colour;
    }

    public void receiveValueHint(int value)
    {
        valueHintCounter++;
        this.value = value;
    }

    public void denyColour(Colour colour)
    {
        notColour.add(colour);
    }

    public void denyValue(int value)
    {
        notValue.add(value);
    }

    public boolean hasReceivedColourHint()
    {
        return colourHintCounter > 0;
    }

    public boolean hasReceivedValueHint()
    {
        return valueHintCounter > 0;
    }

    public String key()
    {
        return CardUtil.getKey(colour, value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FrozenCard))
            return false;

        FrozenCard card = (FrozenCard) o;
        return card.colour == colour && card.value == value;
    }

    @Override
    public String toString() {
        String colourString = colour == null ? "??" : colour.toString();
        String valueString = value == -1 ? "??" : String.valueOf(value);
        StringBuilder hintToken = new StringBuilder();
        if (hasReceivedColourHint())
            hintToken.append("C");
        if (hasReceivedValueHint())
            hintToken.append("V");
        return "[" + colourString + " " + valueString + " " + hintToken + "]";
    }
}

class FrozenState {

    FrozenCard[][] hands;

    // A mapping for card and board state analysis.
    ArrayList<FrozenCard> playableCards = new ArrayList<>();
    Map<String, Boolean> playableCardMap = new HashMap<>();
    Map<Colour, Integer> playableNumberMap = new HashMap<>();
    Map<Colour, Boolean> blockedColourMap = new HashMap<>();
    Map<String, Integer> visibleMap = CardCounter.getEmptyMap();
    State originalState;

    FrozenState()
    {

    }

    void reconstruct(State state) throws IllegalActionException {
        // Rebuild the frozen, information rich state from the game state.
        System.out.println("Reconstructing State");
        System.out.println(state.getPreviousAction());

        originalState = state;
        ArrayList<State> stateList = createStateChain(state);

        hands = createHand(state);
        updateActions(stateList, hands);
        calibrateMaps(state);

        for (FrozenCard[] hand : hands) {
            System.out.println(Arrays.toString(hand));
        }

        System.out.println();
    }
    
    // ======================================================================================================
    // Core Utility Functions.
    // ======================================================================================================

    public boolean isCardPlayable(FrozenCard card)
    {
        return playableCardMap.containsKey(card.key());
    }

    public boolean isCardPlayable(Colour colour, int value)
    {
        return playableCardMap.containsKey(CardUtil.getKey(colour, value));
    }

    public float getDiscardScore(FrozenCard card) {
        return getDiscardScore(card.colour, card.value);
    }

    public float getDiscardScore(Colour colour, int value)
    {
        if (blockedColourMap.get(colour))
            return 1.0f;

        if (value == 5)
            return 0.0f;

        if (isCardPlayable(colour, value))
            return 0.2f;

        if (playableNumberMap.containsKey(colour) && value < playableNumberMap.get(colour))
            return 1.0f;

        if (visibleMap.get(CardUtil.getKey(colour, value)) >= 2)
            return 0.5f;

        return 0.3f;
    }

    // ======================================================================================================
    // Functions to build the analysis maps for our agent to use.
    // ======================================================================================================

    private void calibrateMaps(State state)
    {
        calculatePlayableCards(state);
        for (FrozenCard card : playableCards) {
            playableCardMap.put(card.key(), true);
            playableNumberMap.put(card.colour, card.value);
        }
        calculateBlockedMap(state);
        calculateVisibleMap(state);
    }

    private void calculatePlayableCards(State state)
    {
        // Populate the list of all cards playable this turn.
        for(Colour colour: Colour.values()) {
            Stack<Card> firework = state.getFirework(colour);
            if (firework.size() < 5) {
                FrozenCard card = new FrozenCard();
                card.colour = colour;
                card.value = firework.size() + 1;
                playableCards.add(card);
            }
        }
    }

    private void calculateVisibleMap(State state)
    {
        int numberOfPlayers = state.getPlayers().length;

        // Populate the hand with the current known value.
        for (int i = 0; i < numberOfPlayers; i ++)
        {
            Card[] hand = state.getHand(i);
            for (Card card : hand) {
                if (card != null) {
                    String key = CardUtil.getKey(card);
                    visibleMap.put(key, visibleMap.get(key) + 1);
                }
            }
        }
    }

    private void calculateBlockedMap(State state)
    {
        // Returns a mapping of Colour to boolean, indicating whether or not this Colour is blocked from playing.

        // Initially no Colours are blocked.
        for(Colour colour: Colour.values())
            blockedColourMap.put(colour, false);

        // For each playable card...
        // Get the total number in the deck.
        // And check if the discard pile has depleted it.

        Map<String, Integer> deckMap = CardCounter.getDeckMap();
        for (FrozenCard card : playableCards) {
            int totalNumber = deckMap.get(card.key());

            for (Card discardedCard : state.getDiscards())
            {
                if (CardUtil.getKey(discardedCard).equals(card.key())) {
                    totalNumber--;
                    System.out.println(totalNumber);
                }

                if (totalNumber <= 0)
                    blockedColourMap.put(card.colour, true);
            }
        }
    }

    // ======================================================================================================
    // Functions to recreate the hand and hint state given all previous actions.
    // ======================================================================================================

    private ArrayList<State> createStateChain(State state)
    {
        ArrayList<State> stateList = new ArrayList<>();

        // Add the current state to the list.
        stateList.add(state);

        // Add all previous states to the list.
        while (state.getOrder() > 0) {
            state = state.getPreviousState();
            stateList.add(state);
        }

        return stateList;
    }

    private FrozenCard[][] createHand(State state)
    {
        int numberOfPlayers = state.getPlayers().length;
        hands = new FrozenCard[numberOfPlayers][numberOfPlayers > 3 ? 4 : 5];

        // Populate the hand with the current known value.
        for (int i = 0; i < numberOfPlayers; i ++)
        {
            Card[] stateHand = state.getHand(i);
            FrozenCard[] hand = hands[i];

            for (int j = 0; j < stateHand.length; j ++)
            {
                FrozenCard frozenCard = new FrozenCard();
                Card stateCard = stateHand[j];
                if (stateCard != null) {
                    // We can see the details of this card.
                    frozenCard.colour = stateCard.getColour();
                    frozenCard.value = stateCard.getValue();

                } else if (state.getNextPlayer() != i)
                {
                    // This is a card that has been discarded.
                    frozenCard = null;
                }

                // Copy the card into our frozen hand.
                hand[j] = frozenCard;
            }
        }
        return hands;
    }

    private void updateActions(ArrayList<State> stateList, FrozenCard[][] hands) throws IllegalActionException {
        // Step through the action chain to update the cards hint state.
        for (State s : stateList) {
            Action action = s.getPreviousAction();
            if (action == null)
                continue;

            if (action.getType() == ActionType.HINT_COLOUR || action.getType() == ActionType.HINT_VALUE)
                updateHint(hands, action);

            if (action.getType() == ActionType.DISCARD || action.getType() == ActionType.PLAY)
                sealCard(hands, action);
        }
    }

    private void updateHint(FrozenCard[][] hands, Action action) throws IllegalActionException {
        int playerIndex = action.getHintReceiver();
        boolean[] hintList = action.getHintedCards();

        FrozenCard[] hand = hands[playerIndex];
        for (int i = 0; i < hand.length; i++)
        {
            FrozenCard card = hand[i];

            // This hint is irrelevant to us right now.
            if (card == null || card.sealed)
                continue;

            if (hintList[i])
                receiveHint(card, action);
            else
                denyHint(card, action);
        }
    }

    private void receiveHint(FrozenCard card, Action action) throws IllegalActionException {
        if (action.getType() == ActionType.HINT_COLOUR)
            card.receiveColourHint(action.getColour());
        else
            card.receiveValueHint(action.getValue());
    }

    private void denyHint(FrozenCard card, Action action) throws IllegalActionException {
        if (action.getType() == ActionType.HINT_COLOUR)
            card.denyColour(action.getColour());
        else
            card.denyValue(action.getValue());
    }

    private void sealCard(FrozenCard[][] hands, Action action) throws IllegalActionException {
        int playerIndex = action.getPlayer();
        int cardIndex = action.getCard();

        FrozenCard card = hands[playerIndex][cardIndex];
        if (card != null)
            card.sealed = true;
    }
}
