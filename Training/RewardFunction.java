import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Handles the "Deep IRL" Reward Function.
 * - Supports 14 Inputs (Physics, Orientation, Vulnerability)
 * - Supports 3 Model Types (Linear, Fast NN, Deep NN)
 */
public class RewardFunction {

    // Configuration: 0=Linear, 1=FastNN, 2=DeepNN
    private static int activeModelIndex = 1; 
    
    // State flags
    private static boolean isLoaded = false;
    private static boolean useFallback = false;

    // Scaling Parameters
    private static double[] scaleMean = new double[0];
    private static double[] scaleStd = new double[0];

    // Linear Model Weights
    private static double[] linearWeights = new double[0];
    private static double linearBias = 0.0;

    // Fast NN (2 Layers)
    private static double[][] fastFc1W = new double[0][0];
    private static double[] fastFc1B = new double[0];
    private static double[][] fastFc2W = new double[0][0];
    private static double[] fastFc2B = new double[0];
    private static double[][] fastOutW = new double[0][0];
    private static double[] fastOutB = new double[0];

    // Deep NN (4 Layers)
    private static double[][] deepFc1W = new double[0][0];
    private static double[] deepFc1B = new double[0];
    private static double[][] deepFc2W = new double[0][0];
    private static double[] deepFc2B = new double[0];
    private static double[][] deepFc3W = new double[0][0];
    private static double[] deepFc3B = new double[0];
    private static double[][] deepFc4W = new double[0][0];
    private static double[] deepFc4B = new double[0];
    private static double[][] deepOutW = new double[0][0];
    private static double[] deepOutB = new double[0];

    /**
     * Set which model to use (0=Linear, 1=Fast, 2=Deep).
     * Must be called inside agent initialize().
     */
    public static void setModel(int modelIndex) {
        activeModelIndex = modelIndex;
    }

    /**
     * Tries to load weights from multiple possible locations.
     */
    public static void loadWeights() {
        if (isLoaded) return;

        String cwd = Paths.get("").toAbsolutePath().toString();
        // System.out.println("RewardFunction: Current Working Directory is: " + cwd);

        String[] possiblePaths = {
            "data/ai/nn_weights.json",       
            "./nn_weights.json",             
            "../data/ai/nn_weights.json",    
            cwd + "/data/ai/nn_weights.json" 
        };

        for (String path : possiblePaths) {
            File f = new File(path);
            if (f.exists() && !f.isDirectory()) {
                System.out.println("RewardFunction: Loading brain from " + f.getAbsolutePath());
                try {
                    String jsonContent = readFile(f.getAbsolutePath());
                    parseJson(jsonContent);
                    
                    if (scaleStd != null && scaleStd.length > 0) {
                        isLoaded = true;
                        useFallback = false;
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("RewardFunction: Failed to parse " + path);
                    e.printStackTrace();
                }
            }
        }

        System.err.println("RewardFunction: CRITICAL - Brain missing! Using Fallback.");
        useFallback = true;
        isLoaded = true;
    }

    /**
     * Main Evaluation Function (14 Inputs)
     */
    public static double evaluate(double[] rawInputs) {
        if (!isLoaded) loadWeights();

        if (useFallback || scaleStd.length == 0) {
            // Simple Fallback: HP Diff
            return rawInputs[0]; 
        }

        try {
            // 1. Normalize Inputs
            double[] norm = new double[rawInputs.length];
            for (int i = 0; i < rawInputs.length; i++) {
                if (i < scaleStd.length && scaleStd[i] != 0) {
                    norm[i] = (rawInputs[i] - scaleMean[i]) / scaleStd[i];
                } else if (i < scaleMean.length) {
                    norm[i] = rawInputs[i] - scaleMean[i];
                } else {
                    norm[i] = rawInputs[i];
                }
            }

            // 2. Run Selected Model
            if (activeModelIndex == 0) return predictLinear(norm);
            if (activeModelIndex == 1) return predictFastNN(norm);
            if (activeModelIndex == 2) return predictDeepNN(norm);
            
            return 0.0;

        } catch (Exception e) {
            return 0.0;
        }
    }

    // --- MODEL IMPLEMENTATIONS ---

    private static double predictLinear(double[] x) {
        if (linearWeights.length == 0) return 0.0;
        double sum = linearBias;
        for (int i = 0; i < Math.min(x.length, linearWeights.length); i++) {
            sum += x[i] * linearWeights[i];
        }
        return sigmoid(sum);
    }

    private static double predictFastNN(double[] x) {
        if (fastFc1W.length == 0) return 0.0;
        double[] h1 = dense(x, fastFc1W, fastFc1B, true);
        double[] h2 = dense(h1, fastFc2W, fastFc2B, true);
        double[] out = dense(h2, fastOutW, fastOutB, false);
        return sigmoid(out[0]);
    }

    private static double predictDeepNN(double[] x) {
        if (deepFc1W.length == 0) return 0.0;
        double[] h1 = dense(x, deepFc1W, deepFc1B, true);
        double[] h2 = dense(h1, deepFc2W, deepFc2B, true);
        double[] h3 = dense(h2, deepFc3W, deepFc3B, true);
        double[] h4 = dense(h3, deepFc4W, deepFc4B, true);
        double[] out = dense(h4, deepOutW, deepOutB, false);
        return sigmoid(out[0]);
    }

    // --- MATH HELPERS ---

    private static double[] dense(double[] input, double[][] weights, double[] bias, boolean relu) {
        if (weights == null || bias == null || weights.length == 0) return new double[0];
        int neurons = bias.length;
        double[] output = new double[neurons];
        for (int i = 0; i < neurons; i++) {
            double sum = bias[i];
            int connections = Math.min(input.length, weights[i].length);
            for (int j = 0; j < connections; j++) sum += input[j] * weights[i][j];
            output[i] = relu ? Math.max(0, sum) : sum;
        }
        return output;
    }

    private static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    // --- JSON PARSER ---
    private static void parseJson(String json) {
        try {
            scaleMean = parseArray(json, "\"mean\":");
            scaleStd  = parseArray(json, "\"scale\":");
            
            // Linear
            linearWeights = parseArray(json, "\"linear\":", "\"weights\":");
            linearBias    = parseDouble(json, "\"linear\":", "\"bias\":");

            // Fast NN
            fastFc1W = parseMatrix(json, "\"fast_nn\":", "\"fc1_w\":");
            fastFc1B = parseArray(json, "\"fast_nn\":", "\"fc1_b\":");
            fastFc2W = parseMatrix(json, "\"fast_nn\":", "\"fc2_w\":");
            fastFc2B = parseArray(json, "\"fast_nn\":", "\"fc2_b\":");
            fastOutW = parseMatrix(json, "\"fast_nn\":", "\"out_w\":");
            fastOutB = parseArray(json, "\"fast_nn\":", "\"out_b\":");

            // Deep NN
            deepFc1W = parseMatrix(json, "\"deep_nn\":", "\"fc1_w\":");
            deepFc1B = parseArray(json, "\"deep_nn\":", "\"fc1_b\":");
            deepFc2W = parseMatrix(json, "\"deep_nn\":", "\"fc2_w\":");
            deepFc2B = parseArray(json, "\"deep_nn\":", "\"fc2_b\":");
            deepFc3W = parseMatrix(json, "\"deep_nn\":", "\"fc3_w\":");
            deepFc3B = parseArray(json, "\"deep_nn\":", "\"fc3_b\":");
            deepFc4W = parseMatrix(json, "\"deep_nn\":", "\"fc4_w\":");
            deepFc4B = parseArray(json, "\"deep_nn\":", "\"fc4_b\":");
            deepOutW = parseMatrix(json, "\"deep_nn\":", "\"out_w\":");
            deepOutB = parseArray(json, "\"deep_nn\":", "\"out_b\":");

        } catch (Exception e) {
            System.err.println("RewardFunction: JSON Parsing Error");
            e.printStackTrace();
        }
    }

    // (Helper parsing methods same as before - parseArray, parseMatrix, parseDouble, getSection, readFile)
    // ... [Include the same parsing helpers from previous version here for brevity]
    
    private static double[] parseArray(String json, String... keys) {
        String section = getSection(json, keys);
        if (section == null) return new double[0];
        int start = section.indexOf("[");
        int end = section.indexOf("]");
        if (start == -1 || end == -1) return new double[0];
        String[] parts = section.substring(start + 1, end).split(",");
        double[] res = new double[parts.length];
        for (int i=0; i<parts.length; i++) {
            try { res[i] = Double.parseDouble(parts[i].trim()); } catch (Exception e) { res[i] = 0.0; }
        }
        return res;
    }

    private static double[][] parseMatrix(String json, String... keys) {
        String section = getSection(json, keys);
        if (section == null) return new double[0][0];
        int start = section.indexOf("[[");
        int end = section.indexOf("]]");
        if (start == -1 || end == -1) return new double[0][0];
        String content = section.substring(start + 2, end);
        String[] rows = content.split("\\],\\s*\\[");
        double[][] matrix = new double[rows.length][];
        for(int i=0; i<rows.length; i++) {
            String[] nums = rows[i].split(",");
            matrix[i] = new double[nums.length];
            for(int j=0; j<nums.length; j++) {
                try { matrix[i][j] = Double.parseDouble(nums[j].trim()); } catch (Exception e) { matrix[i][j] = 0.0; }
            }
        }
        return matrix;
    }
    
    private static double parseDouble(String json, String... keys) {
        String section = getSection(json, keys);
        if (section == null) return 0.0;
        String[] parts = section.split("[:,}]");
        try { return Double.parseDouble(parts[0].trim()); } catch (Exception e) { return 0.0; }
    }

    private static String getSection(String json, String[] keys) {
        int idx = 0;
        for(String key : keys) {
            idx = json.indexOf(key, idx);
            if(idx == -1) return null;
            idx += key.length();
        }
        return json.substring(idx);
    }

    private static String readFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}