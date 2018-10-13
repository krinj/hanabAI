import agents.Agent21789272;
import hanabAI.Agent;
import hanabAI.Hanabi;

public class SimulateHanabi {
    public static void main(String[] args)
    {
        System.out.println("Simulating Hanabi Games...");
        int K_SIMULATION_COUNT = 1;
        int count = 0;
        int totalScore = 0;

        while (count < K_SIMULATION_COUNT) {
            count++;
            Agent[] agents = {
                    new Agent21789272(),
                    new Agent21789272(),
                    new Agent21789272(),
                    new Agent21789272()};

            Hanabi game= new Hanabi(agents);
            StringBuffer gameLog = new StringBuffer();
            int result = game.play(gameLog);
            StringBuffer resultLog = new StringBuffer("Game #")
                    .append(count)
                    .append(" Score: ")
                    .append(result)
                    .append("\n");
            System.out.print(gameLog);
            System.out.print(resultLog);
            totalScore += result;
        }

        StringBuffer finalResultLog = new StringBuffer("Simulation Complete. Average Score: ")
                .append(totalScore/ (float) K_SIMULATION_COUNT);
        System.out.println(finalResultLog);
    }
}
