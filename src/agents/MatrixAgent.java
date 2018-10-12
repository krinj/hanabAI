package agents;
import hanabAI.*;

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
        playerIndex = state.getNextPlayer();
        try {
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
}