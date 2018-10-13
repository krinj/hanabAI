package agents;
import hanabAI.*;

import java.util.*;

class HintStat {

    // Hint: Action Commands.
    int targetIndex;
    int playerIndex;
    int value;
    Colour colour;

    // Hint: Value Analysis.
    int distance;
    int enablesPlay = 0;
    int enablesDiscard = 0;
    float totalPlayGain = 0.0f;
    float totalDiscardGain = 0.0f;
    float maxPlayGain = 0.0f;
    float maxDiscardGain = 0.0f;
    int vitalReveal = 0;
    int nCardsAffected = 0;
    int truePlayableCards = 0;

    HintStat(int playerIndex, int targetIndex, Colour colour, int value, FrozenState state)
    {
        this.playerIndex = playerIndex;
        this.targetIndex = targetIndex;
        this.colour = colour;
        this.value = value;
        distance = 0;

        int i = playerIndex;
        while (i != targetIndex)
        {
            distance ++;
            i ++;

            if (i >= state.originalState.getPlayers().length)
                i = 0;
        }
        distance = state.originalState.getPlayers().length - distance;
    }

    void registerEffect(FrozenCard card, FrozenState state)
    {
        // This hint can affect the board state.
        nCardsAffected ++;
        if (state.isCardPlayable(card))
            truePlayableCards ++;
    }

    float getPlayEnablingScore()
    {
        return distance * (enablesPlay > 0 ? 1f : 0);
    }

    float getTruePlayScore()
    {
        return (truePlayableCards > 0 ? 1f : 0);
    }

    float getDiscardEnablingScore()
    {
        return distance * (enablesDiscard > 1 ? 1f : 0);
    }

    float getMaxPlayGainScore()
    {
        return distance * totalPlayGain;
    }

    @Override
    public String toString() {
        String hintSubject = (colour != null) ? colour.toString() : String.valueOf(value);
        return "Hint: [" + hintSubject + " to Player " + targetIndex + "]: " +
                "{" + getPlayEnablingScore() +", " + getTruePlayScore() +", " + getDiscardEnablingScore() + ", "+getMaxPlayGainScore()+ "}";
    }
}

class HintComparator implements Comparator<HintStat> {
    public int compare(HintStat o1, HintStat o2) {
        int rPlayEnabling = Float.compare(o1.getPlayEnablingScore(), o2.getPlayEnablingScore());
        if (rPlayEnabling != 0)
            return rPlayEnabling;

        int rTruePlayable = Float.compare(o1.getTruePlayScore(), o2.getTruePlayScore());
        if (rTruePlayable != 0)
            return rTruePlayable;

        int rDiscardEnabling = Float.compare(o1.getDiscardEnablingScore(), o2.getDiscardEnablingScore());
        if (rDiscardEnabling != 0)
            return rDiscardEnabling;

        return Float.compare(o1.getMaxPlayGainScore(), o2.getMaxPlayGainScore());
    }
}

public class MatrixAgent implements Agent {

    // Control Constants. This determines agent behaviour.
    static final float K_NORMAL_PLAY_LIMIT = 0.80f;
    static final float K_SAFE_PLAY_LIMIT = 1.00f;
    static final float K_DISCARD_LIMIT = 0.80f;
    static final float K_HINT_PLAY_BOOST = 1.00f;

    // Utility Agent Variables. Good to keep track of.
    private int playerIndex = 0;

    public String toString(){
        return "MatrixAgent";
    }

    @Override
    public Action doAction(State state) {
        try {

            playerIndex = state.getNextPlayer();
            FrozenState frozenState = new FrozenState();
            frozenState.reconstruct(state);
            List<CardMatrix> matrices = generateHandMatrix(frozenState, playerIndex);

//            System.out.println();
//            for (FrozenCard[] hand : frozenState.hands)
//                System.out.println(Arrays.toString(hand));
//
//            System.out.println();

            CardMatrix pMatrix = null;  // The Matrix for the most playable card in our hand.
            CardMatrix dMatrix = null;  // The Matrix for the most useless card in our hand.
            for (CardMatrix m : matrices) {
                pMatrix = (pMatrix == null || pMatrix.getPlayRating() < m.getPlayRating()) ? m : pMatrix;
                dMatrix = (dMatrix == null || dMatrix.getDiscardRating() < m.getDiscardRating()) ? m : dMatrix;
            }

            // Make sure the Matrix is never null.
            assert pMatrix != null;

            // Voluntarily play a card.
            float playLimit = state.getFuseTokens() == 1 ? K_SAFE_PLAY_LIMIT : K_NORMAL_PLAY_LIMIT;
            if (pMatrix.getPlayRating() >= playLimit) {
//                System.out.println("Play: " + frozenState.hands[playerIndex][pMatrix.handIndex]);
                return new Action(playerIndex, toString(), ActionType.PLAY, pMatrix.handIndex);
            }

            // Voluntarily discard.
            if (state.getHintTokens() < 8 && dMatrix.getDiscardRating() >= K_DISCARD_LIMIT) {
//                System.out.println("Discard: " + frozenState.hands[playerIndex][pMatrix.handIndex]);
                return new Action(playerIndex, toString(), ActionType.DISCARD, dMatrix.handIndex);
            }

            // Give hint to another player.
            if (state.getHintTokens() > 0) {
                List<HintStat> hints = getValidHintCommands(frozenState, playerIndex);
                HintStat hint = getBestHint(hints);
//                System.out.println("Best Hint: " + hint);
//                System.out.println();

                // Generate the action for this hint.
                Card[] targetHand = state.getHand(hint.targetIndex);
                boolean[] hintCards = new boolean[targetHand.length];
                for (int i = 0; i < targetHand.length; i++)
                {
                    Card card = targetHand[i];
                    if (card == null)
                        continue;

                    hintCards[i] = false;

                    if (hint.colour != null && hint.colour == card.getColour())
                        hintCards[i] = true;

                    if (hint.value > 0 && hint.value == card.getValue())
                        hintCards[i] = true;
                }

                ActionType hintType = hint.colour == null ? ActionType.HINT_VALUE : ActionType.HINT_COLOUR;
                if (hint.colour == null)
                    return new Action(playerIndex, toString(), hintType, hint.targetIndex, hintCards, hint.value);
                else
                    return new Action(playerIndex, toString(), hintType, hint.targetIndex, hintCards, hint.colour);
            }

            // Forced to discard.
//            System.out.println("Discard: " + frozenState.hands[playerIndex][dMatrix.handIndex]);
            return new Action(playerIndex, toString(), ActionType.DISCARD, dMatrix.handIndex);

        } catch(IllegalActionException e){
            e.printStackTrace();
            throw new RuntimeException("Something has gone very wrong");
        }
    }

    // ======================================================================================================
    // Analyzer and Matrix Generation Logic.
    // ======================================================================================================

    public List<CardMatrix> generateHandMatrix(FrozenState state, int playerIndex)
    {
        // Generate a CardMatrix for each card in hand, based on what we know.
        ArrayList<CardMatrix> matrices = new ArrayList<>();
        FrozenCard[] hand = state.hands[playerIndex];
        Map<String, Integer> observedMap = generateObservationMap(state, playerIndex, -1);

        for (int i = 0; i < hand.length; i++)
        {
            FrozenCard card = hand[i];
            if (card == null)
                continue;

            // Create a new matrix based on all the information that we know.
            CardMatrix matrix = getCardMatrix(
                    state, playerIndex, card.colour, card.value, card.notColour, card.notValue, observedMap);
            matrix.age = card.age;

            // Bind it to this hand.
            matrix.handIndex = i;

            // Favour the hinted cards.
            if (card.hasReceivedValueHint() || card.hasReceivedColourHint())
            {
                if (matrix.getPlayRating() > matrix.getDiscardRating())
                    matrix.playRatingFactor = K_HINT_PLAY_BOOST;
            }

            // Add this to all all known card matrices.
            matrices.add(matrix);
        }
        return matrices;
    }

    public Map<String, Integer> generateObservationMap(FrozenState state, int playerIndex, int offhandIndex)
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
                    if (card == null ||
                            ((offhandIndex == i) && !(card.hasReceivedValueHint() && card.hasReceivedColourHint())))
                        continue;
                    counter.add(card.colour, card.value, -1);
                }
            }
        }

        return new HashMap<>(counter.cardMap);
    }

    public float getProbability(FrozenState state, int playerIndex,
                                Colour colour, int value, Colour knownColour, int knownValue,
                                Set<Colour> notColour, Set<Integer> notValue, Map<String, Integer> observedMap)
    {
        // Get the probability of this Colour and Value combination for a card, given what we know.
        CardCounter counter = new CardCounter();

        if (observedMap == null) {
            counter.cardMap = generateObservationMap(state, playerIndex, -1);
        } else {
            counter.cardMap  = new HashMap<>(observedMap);
        }

        // Eliminate Cards that we know it cannot be.
        for (Colour ci : Colour.values()) {
            for (int i = 1; i < 6; i ++)
            {
                if ((knownColour != null && knownColour != ci) || (knownValue > 0 && knownValue != i))
                    counter.set(ci, i, 0);

                if ((notColour != null && notColour.contains(ci)) || (notValue != null && notValue.contains(i)))
                    counter.set(ci, i, 0);
            }
        }

        int cardCount = counter.count(colour, value);
        int totalCount = counter.totalCount();
        if (totalCount == 0)
            return 0.0f;

        return (float) cardCount / (float) totalCount;
    }

    public float getPlayRating(FrozenState state, Colour colour, int value)
    {
        return state.isCardPlayable(colour, value) ? 1.0f : 0.0f;
    }

    public float getDiscardRating(FrozenState state, Colour colour, int value)
    {
        return state.getDiscardScore(colour, value);
    }

    public CardMatrix getCardMatrix(FrozenState state, int playerIndex, Colour knownColour, int knownValue,
                                    Set<Colour> notColour, Set<Integer> notValue, Map<String, Integer> observedMap)
    {
        CardMatrix matrix = new CardMatrix();

        // For each possible card, work out its probability, play and discard rating.
        for (Colour c : Colour.values())
        {
            for (int v = 1; v < 6; v ++)
            {
                CardStat stat = new CardStat();
                stat.colour = c;
                stat.value = v;
                stat.playRating = getPlayRating(state, c, v);
                stat.discardRating = getDiscardRating(state, c, v);
                stat.probability = getProbability(
                        state, playerIndex, c, v, knownColour, knownValue, notColour, notValue, observedMap);
                matrix.add(stat);
            }
        }

        return matrix;
    }
    
    // ======================================================================================================
    // Hint Analysis Logic.
    // ======================================================================================================

    public ArrayList<HintStat> getValidHintCommands(FrozenState state, int playerIndex)
    {
        ArrayList<HintStat> hints = new ArrayList<>();

        int numberOfPlayers = state.originalState.getPlayers().length;
        for (int i = 0; i < numberOfPlayers; i++)
        {
            if (i == playerIndex)
                continue;

            HashSet<Colour> colourMap = new HashSet<>();
            HashSet<Integer> valueMap = new HashSet<>();

            FrozenCard[] hand = state.hands[i];
            for (FrozenCard card : hand)
            {
                if (card == null)
                    continue;

                if (!card.hasReceivedColourHint())
                    colourMap.add(card.colour);

                if (!card.hasReceivedValueHint())
                    valueMap.add(card.value);
            }

            for (Colour c : colourMap)
                hints.add(new HintStat(playerIndex, i, c, -1, state));

            for (int v : valueMap)
                hints.add(new HintStat(playerIndex, i, null, v, state));
        }

        for (HintStat hint : hints)
            populateHintRating(state, hint);

        return hints;
    }

    public void populateHintRating(FrozenState state, HintStat hint)
    {
        FrozenCard[] hand = state.hands[hint.targetIndex];
        Map<String, Integer> observedMap = generateObservationMap(state, hint.targetIndex, hint.playerIndex);

        for (FrozenCard card : hand)
        {
            if (card == null)
                continue;

            CardMatrix originalMatrix = getCardMatrix(
                    state, hint.targetIndex, card.getObservedColour(), card.getObservedValue(), card.notColour, card.notValue, observedMap);
            CardMatrix postMatrix = originalMatrix;

            // Simulate Giving a Colour Hint.
            if (hint.colour != null && hint.colour == card.colour)
            {
                if (!card.hasReceivedColourHint())
                    hint.registerEffect(card, state);

                postMatrix = getCardMatrix(
                        state, hint.targetIndex, hint.colour, card.getObservedValue(), card.notColour, card.notValue, observedMap);
            }

            // Simulate Giving a Value Hint.
            if (hint.value > 0 && hint.value == card.value)
            {
                if (!card.hasReceivedValueHint()) {
                    hint.registerEffect(card, state);
                    if (hint.value == 5)
                        hint.vitalReveal ++;
                }

                postMatrix = getCardMatrix(
                        state, hint.targetIndex, card.getObservedColour(), hint.value, card.notColour, card.notValue, observedMap);
            }

            float playGain = postMatrix.getPlayRating() - originalMatrix.getPlayRating();
            float discardGain = postMatrix.getDiscardRating() - originalMatrix.getDiscardRating();

            if (postMatrix.getPlayRating() > 0.99f && playGain > 0f)
                hint.enablesPlay ++;

            if (postMatrix.getDiscardRating() > 0.99f && discardGain > 0f)
                hint.enablesDiscard ++;

            hint.totalPlayGain += playGain;
            hint.totalDiscardGain += discardGain;
            hint.maxPlayGain = Math.max(hint.maxPlayGain, playGain);
            hint.maxDiscardGain = Math.max(hint.maxDiscardGain, discardGain);
        }
    }

    public HintStat getBestHint(List<HintStat> hints)
    {
        hints.sort(new HintComparator());
        return hints.get(hints.size() - 1);
    }
}