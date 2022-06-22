import java.io.*;
import java.util.*;
import java.lang.*;

public class Prog3 {
    
    int nonterminal = 0, terminal = 0, round = 0, frequency = 0, M = 0;
    int top = Integer.MIN_VALUE;
    int bottom = Integer.MAX_VALUE;
    int maxAction = Integer.MIN_VALUE;
    List<State> list = new ArrayList<>();
    Terminalstate[] terminalSet;
    
    public static void main (String[] args) throws IOException {

        if(args.length < 1) {
            System.out.println("Error, can't find input file name");
            System.exit(1);
        }
        
        Prog3 prog = new Prog3();
        prog.read(args);
        prog.learn();
        
    }
    
    // get the input
    void read(String[] args) throws IOException {
        try {
            Scanner sc = new Scanner(new File(args[0]));

            // get first line
            nonterminal = sc.nextInt();
            terminal = sc.nextInt();
            round = sc.nextInt();
            frequency = sc.nextInt();
            M = sc.nextInt();
            
            sc.nextLine();
            //get second line
            String[] t = sc.nextLine().split("\\s");
            terminalSet = new Terminalstate[t.length];
            for (int i = 0; i < t.length / 2; i++) {
                terminalSet[i] = new Terminalstate(Integer.parseInt(t[i*2]), Integer.parseInt(t[i*2 + 1]));
                top = Math.max(top, Integer.parseInt(t[i*2 + 1]));
                bottom = Math.min(bottom, Integer.parseInt(t[i*2 + 1]));
            }
            //get the rest
            int last = 0;
            int tmpnum = 0;
            String[] tmp;
            tmp = sc.nextLine().split("\\s");
            int k = 0;
            while (tmp[0].charAt(k) != ':')
                k++;
            tmpnum = Integer.parseInt(tmp[0].substring(0, k));
            while (sc.hasNextLine()) {
                State s = new State(0);
                List<Map<Integer, Double>> p = new ArrayList<Map<Integer, Double>>();
                s.number = tmpnum;
                while (tmpnum == last) {
                    Map<Integer, Double> m = new HashMap<Integer, Double>();
                    for (int i = 1; i < tmp.length; i += 2) {
                        m.put(Integer.parseInt(tmp[i]), Double.parseDouble(tmp[i + 1]));
                    }
                    p.add(m);
                    if (sc.hasNextLine()) {
                        tmp = sc.nextLine().split("\\s");
                        int q = 0;
                        while (tmp[0].charAt(q) != ':')
                            q++;
                        tmpnum = Integer.parseInt(tmp[0].substring(0, q));
                    } else {
                        break;
                    }
                }
                list.add(s);
                s.action = p;
                s.actionNumber = p.size();
                maxAction = Math.max(maxAction, s.actionNumber);
                last = tmpnum;
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("No file found");
        }
    }
       
    // learning process
    void learn() {
        int[][] count = new int[nonterminal][maxAction];
        int[][] total = new int[nonterminal][maxAction];
        int currentRound = 1;
        while (currentRound <= round) {
            
            // get a random starting state
            Random ran = new Random();
            int nextState = ran.nextInt(nonterminal);
            State start = new State(0);
            int nextAction = 0;
            int finalReward = 0;
            boolean[][] dup = new boolean[nonterminal][maxAction];
            
            do {
                start = list.get(nextState);
                nextAction = chooseAction(start, count, total, M, top, bottom);
                dup[nextState][nextAction] = true;
                Map<Integer, Double> tmpMap = start.action.get(nextAction);
                nextState = chooseState(tmpMap);
            } while (nextState < nonterminal);
            
            for (int i = 0; i < terminal; i++) {
                if (terminalSet[i].number == nextState) {
                    finalReward = terminalSet[i].reward;
                    break;
                }
            }
            
            for (int i = 0; i < nonterminal; i++) {
                for (int j = 0; j < maxAction; j++) {
                    if (dup[i][j]) {
                        count[i][j]++;
                        total[i][j] += finalReward;
                    }
                }
            }
            
            // print every frequency's round
            if (currentRound % frequency == 0) {
                System.out.println("After " + currentRound + " rounds");
                
                // print count
                System.out.println("Count:");
                for (int i = 0; i < count.length; i++) {
                    for (int j = 0; j < list.get(i).actionNumber; j++) {
                        System.out.print("[" + i + "," + j + "]=" + count[i][j] + ". ");
                    }
                    System.out.println();
                }
                
                // print total
                System.out.println();
                System.out.println("Total:");
                for (int i = 0; i < total.length; i++) {
                    for (int j = 0; j < list.get(i).actionNumber; j++) {
                        System.out.print("[" + i + "," + j + "]=" + total[i][j] + ". ");
                    }
                    System.out.println();
                }
                
                // print the best action
                System.out.println();
                System.out.print("Best action: ");
                for (int i = 0; i < total.length; i++) {
                    boolean isU = false;
                    double max = Double.MIN_VALUE;
                    int bestAction = 0;
                    System.out.print(i + ":");
                    for (int j = 0; j < list.get(i).actionNumber; j++) {
                        if (count[i][j] == 0) {
                            isU = true;
                            break;
                        }
                        if (Double.compare(max, (double)total[i][j] / count[i][j]) < 0) {
                            bestAction = j;
                            max = (double)total[i][j] / count[i][j];
                        }
                    }
                    if (isU) {
                        System.out.print("U. ");
                    } else {
                        System.out.print(bestAction + ". ");
                    }
                }
                System.out.println();
                System.out.println();
            }
            currentRound++;
        }
    }

    // choose an action
    int chooseAction (State s, int[][] count, int[][] total, int M, int top, int bottom) {
        int n = s.actionNumber;
        int c = 0;
        double norm = 0.0;
        double[] avg = new double[n];
        double[] savg = new double[n];
        double[] up = new double[n];
        double[] p = new double[n];
        double[] u = new double[n];
        for (int i = 0; i < n; i++) {
            if (count[s.number][i] == 0)
                return i;
        }
        for (int i = 0; i < n; i++) {
            avg[i] = (double) total[s.number][i] / count[s.number][i];
            savg[i] = 0.25 + 0.75 * (avg[i] - bottom) / (top - bottom);
            c += count[s.number][i];
        }
        for (int i = 0; i < n; i++) {
            up[i] = Math.pow(savg[i], (double)c / M);
            norm += up[i];
        }
        for (int i = 0; i < n; i++) {
            p[i] = up[i] / norm;
        }
        
        // choose a random action
        u[0] = p[0];
        for (int i = 1; i < n; i++) {
            u[i] = u[i - 1] + p[i];
        }
        double x = Math.random();
        for (int i = 0; i < n - 1; i++) {
            if (Double.compare(x, u[i]) < 0)
                return i;
        }
        return (n - 1);
    }
                     
    // choose the nextState by the probability
    int chooseState (Map<Integer, Double> tmpMap) {
        double sum = 0.0;
        int i = 0;
        double x = Math.random();
        for (Integer d : tmpMap.keySet()) {
            i++;
            if (i == tmpMap.size())
                return d;
            sum += tmpMap.get(d);
            if (Double.compare(x, sum) < 0)
                return d;
        }
        return 0;
    }
}

class State {
    int number;
    int actionNumber;
    List<Map<Integer, Double>> action = new ArrayList<Map<Integer, Double>>();
    public State (int number) {
        this.number = number;
    }
}

class Terminalstate {
    int number;
    int reward;
    public Terminalstate (int number, int reward) {
        this.number = number;
        this.reward = reward;
    }
}
