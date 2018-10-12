import hanabAI.Agent;
import hanabAI.Hanabi;

public class SimulateHanabi {
    public static void main(String[] args)
    {
        System.out.println("Simulating Hanabi Games...");
        int K_SIMULATION_COUNT = 100;
        int count = 0;
        int totalScore = 0;

        while (count < K_SIMULATION_COUNT) {
            count++;
            Agent[] agents = {
                    new agents.BasicAgent(),
                    new agents.BasicAgent(),
                    new agents.BasicAgent(),
                    new agents.BasicAgent()};

            Hanabi game= new Hanabi(agents);
            StringBuffer gameLog = new StringBuffer("A simple game for three basic agents:\n");
            int result = game.play(gameLog);
            StringBuffer resultLog = new StringBuffer("Game #")
                    .append(count)
                    .append(" Score: ")
                    .append(result)
                    .append("\n");
            System.out.print(resultLog);
            totalScore += result;
        }

        StringBuffer finalResultLog = new StringBuffer("Simulation Complete. Average Score: ")
                .append(totalScore/K_SIMULATION_COUNT);
        System.out.println(finalResultLog);
    }
}
