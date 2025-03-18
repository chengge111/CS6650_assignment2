package cs6650;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Log {

    private static final String fileName = "logs.csv";

    public static void createCSV() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            System.out.println("CSV file created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateCSV(double start, double end, int statusCode) {
        try (FileWriter writer = new FileWriter(fileName, true)) { // 'true' enables appending
            writer.append(Double.toString(start)).append(",").append(Double.toString(end)).append(",").append(Double.toString(end - start)).append(",").append(Integer.toString(statusCode)).append(",").append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void statistics() {
        List<Double> values = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false; // Skip header
                    continue;
                }
                String[] data = line.split(",");
                try {
                    values.add(Double.parseDouble(data[2].trim()));
                } catch (NumberFormatException e) {
                    System.out.println("Skipping invalid value: " + data[0]);
                }
            }
            if (values.isEmpty()) {
                System.out.println("No valid data found.");
                return;
            }
            double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);

            // Compute Median
            Collections.sort(values);
            double median = computeMedian(values);

            // Compute 99th Percentile
            double percentile99 = computePercentile(values, 99);


            System.out.println("Mean Value of Response Times: " + mean + " ms");
            System.out.println("Median Value of Response Times: " + median + " ms");
            System.out.println("99th Percentile Value of Response Times: " + percentile99 + " ms");
            System.out.println("Minimum Value of Response Times: " + values.get(0) + " ms");
            System.out.println("Maximum Value of Response Times: " + values.get(values.size() - 1) + " ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double computeMedian(List<Double> values) {
        int size = values.size();
        if (size % 2 == 0) {
            return (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        } else {
            return values.get(size / 2);
        }
    }

    private static double computePercentile(List<Double> values, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * values.size()) - 1;
        return values.get(Math.min(index, values.size() - 1));
    }
}
