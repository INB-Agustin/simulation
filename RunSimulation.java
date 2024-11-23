import java.util.Random;
import java.util.Scanner;

public class RunSimulation {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Step 1: Get observed data from user input
        System.out.print("Enter the number of rows in the contingency table: ");
        int numRows = scanner.nextInt();

        System.out.print("Enter the number of columns in the contingency table: ");
        int numCols = scanner.nextInt();

        int[][] observed = new int[numRows][numCols];
        System.out.println("Enter the observed data row by row (space-separated):");
        for (int i = 0; i < numRows; i++) {
            System.out.print("Enter row " + (i + 1) + ": ");
            for (int j = 0; j < numCols; j++) {
                observed[i][j] = scanner.nextInt();
            }
        }

        // Step 2: Perform the Monte Carlo simulation
        int numSimulations = 10000; // Number of simulations
        runMonteCarloSimulation(observed, numSimulations);

        scanner.close();
    }

    // Method to calculate expected frequencies
    public static double[][] calculateExpectedFrequencies(int[][] observed) {
        int numRows = observed.length;
        int numCols = observed[0].length;

        int[] rowTotals = new int[numRows];
        int[] colTotals = new int[numCols];
        int grandTotal = 0;

        // Calculate row and column totals
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                rowTotals[i] += observed[i][j];
                colTotals[j] += observed[i][j];
                grandTotal += observed[i][j];
            }
        }

        // Calculate expected frequencies
        double[][] expected = new double[numRows][numCols];
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                expected[i][j] = (double) rowTotals[i] * colTotals[j] / grandTotal;
            }
        }

        return expected;
    }

    // Method to calculate the Chi-Square statistic
    public static double calculateChiSquare(int[][] observed, double[][] expected) {
        double chiSquare = 0.0;
        for (int i = 0; i < observed.length; i++) {
            for (int j = 0; j < observed[0].length; j++) {
                double expectedValue = expected[i][j];
                if (expectedValue > 0) {
                    chiSquare += Math.pow(observed[i][j] - expectedValue, 2) / expectedValue;
                }
            }
        }
        return chiSquare;
    }

    // Method to simulate a contingency table under the null hypothesis
    public static int[][] simulateTable(double[][] expected, int grandTotal) {
        int[][] simulated = new int[expected.length][expected[0].length];
        Random random = new Random();

        // Flatten expected frequencies for multinomial sampling
        double[] flatExpected = new double[expected.length * expected[0].length];
        int[] flatSimulated = new int[flatExpected.length];
        int index = 0;

        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < expected[0].length; j++) {
                flatExpected[index++] = expected[i][j];
            }
        }

        // Simulate counts using multinomial sampling
        for (int i = 0; i < grandTotal; i++) {
            double rand = random.nextDouble() * grandTotal;
            double cumulative = 0.0;

            for (int k = 0; k < flatExpected.length; k++) {
                cumulative += flatExpected[k];
                if (rand < cumulative) {
                    flatSimulated[k]++;
                    break;
                }
            }
        }

        // Reconstruct simulated table
        index = 0;
        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < expected[0].length; j++) {
                simulated[i][j] = flatSimulated[index++];
            }
        }

        return simulated;
    }

    // Method to calculate degrees of freedom
    public static int calculateDegreesOfFreedom(int numRows, int numCols) {
        return (numRows - 1) * (numCols - 1);
    }

    // Method to calculate Cramer's V
    public static double calculateCramersV(double chiSquare, int grandTotal, int numRows, int numCols) {
        int minDimension = Math.min(numRows - 1, numCols - 1);
        return Math.sqrt(chiSquare / (grandTotal * minDimension));
    }

    // Method to run the Monte Carlo simulation
    public static void runMonteCarloSimulation(int[][] observed, int numSimulations) {
        double[][] expected = calculateExpectedFrequencies(observed);
        double observedChiSquare = calculateChiSquare(observed, expected);

        int numRows = observed.length;
        int numCols = observed[0].length;

        int grandTotal = 0;
        for (int[] row : observed) {
            for (int cell : row) {
                grandTotal += cell;
            }
        }

        int degreesOfFreedom = calculateDegreesOfFreedom(numRows, numCols);

        // Critical value for 5% significance level
        double criticalValue = chiSquareCriticalValue(degreesOfFreedom, 0.05);

        int countExceeding = 0;

        for (int i = 0; i < numSimulations; i++) {
            int[][] simulated = simulateTable(expected, grandTotal);
            double simulatedChiSquare = calculateChiSquare(simulated, expected);

            if (simulatedChiSquare >= observedChiSquare) {
                countExceeding++;
            }
        }

        double pValue = (double) countExceeding / numSimulations;
        double cramerV = calculateCramersV(observedChiSquare, grandTotal, numRows, numCols);

        // Output results
        System.out.println("Chi-Square Test Statistic: " + observedChiSquare);
        System.out.println("Degrees of Freedom: " + degreesOfFreedom);
        System.out.println("Critical Value (5% significance): " + criticalValue);
        System.out.println("Cramer's V: " + cramerV);
        
        // Interpretation of Cramer's V
        if (observedChiSquare > criticalValue) {
            System.out.print("Association strength: ");
            if (cramerV < 0.1) System.out.println("Weak");
            else if (cramerV < 0.3) System.out.println("Moderate");
            else System.out.println("Strong");
        } else {
            System.out.println("No Association");
        }
        
        
        // Decision on the null hypothesis
        if (observedChiSquare > criticalValue) {
            System.out.println("Decision: Reject the null hypothesis.");
        } else {
            System.out.println("Decision: Failed to reject the null hypothesis.");
        }
        //Simulaiton value
        System.out.println("p-value from simulation: " + pValue);
    }

    // Method to approximate the critical value from Chi-Square table
    public static double chiSquareCriticalValue(int df, double significance) {
        // Approximation for small degrees of freedom, default for higher df
        if (df == 1) return 3.841;
        if (df == 2) return 5.991;
        if (df == 3) return 7.815;
        if (df == 4) return 9.488;
        if (df == 5) return 11.070;
        if (df == 6) return 12.592;  
        if (df == 7) return 14.067; 
        if (df == 8) return 15.507;
        if (df == 9) return 16.919;  
        if (df == 10) return 18.307;  
        if (df == 11) return 19.675; 
        if (df == 12) return 21.026;
        if (df == 13) return 22.362;
        if (df == 14) return 23.685;
        if (df == 15) return 24.996;
        if (df == 16) return 26.296;
        if (df == 17) return 27.587;
        if (df == 18) return 28.869;
        if (df == 19) return 30.144;
        if (df == 20) return 31.410;
        if (df == 21) return 32.671;
        if (df == 22) return 33.924;
        if (df == 23) return 35.172;
        if (df == 24) return 36.415;
        if (df == 25) return 37.652;
        if (df == 26) return 38.885;
        if (df == 27) return 40.113;
        if (df == 28) return 41.337;
        if (df == 29) return 42.557;
        if (df == 30) return 43.773;
        return 10 + 2 * (df - 5); // Linear approximation for df > 30
    }
}
