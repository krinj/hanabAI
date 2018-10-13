package agents;
import hanabAI.Colour;

import java.util.HashMap;
import java.util.Map;

public class CardMatrix {

    Map<String, CardStat> stats = new HashMap<>();
    int handIndex = 0;
    float playRatingFactor = 1.0f;

    public void add(CardStat stat)
    {
        String key = CardUtil.getKey(stat.colour, stat.value);
        stats.put(key, stat);
    }

    public float getPlayRating()
    {
        float score = 0.0f;
        for (String key : stats.keySet())
        {
            CardStat stat = stats.get(key);
            score += stat.probability * stat.playRating;
        }

        if (score < 1.0) {
            score *= playRatingFactor;
            score = Math.min(0.99f, score);
        }

        return score;
    }

    public float getDiscardRating()
    {
        float score = 0.0f;
        for (String key : stats.keySet())
        {
            CardStat stat = stats.get(key);
            score += stat.probability * stat.discardRating;
        }
        return score;
    }
}
