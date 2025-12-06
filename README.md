# DareFightingICE Operation and Custom AI Modification

## Project Goals: Learning Strategic Rewards for Fighting Game AI

Fighting games present a significant challenge for reinforcement learning due to complex state spaces and real-time constraints. Standard computer-controlled players (CPUs) often rely on hand-crafted heuristics that fail to capture high-level strategies such as zoning or cornering.

The primary goal of this project was to improve the **TOVOR** agent, a standard Monte Carlo Tree Search (MCTS) agent in the DareFightingICE competition environment. TOVOR originally relied on a simple heuristic that evaluated future states strictly based on Health Point (HP) difference.

We implemented a **Deep Inverse Reinforcement Learning (IRL) approach** to learn a non-linear reward function from expert gameplay data (specifically, the expert agent BlackMamba). This aimed to teach TOVOR to value states based on implicit strategic values (like spacing, energy, and safety), not just immediate HP changes.

We systematically investigated two primary factors to determine the optimal agent performance:
1.  **Reward Shaping:** Sparse Outcome-based rewards (long-term match outcomes) versus Dense Physics-based rewards (short-term HP change).
2.  **Model Architecture:** Fast Shallow Networks versus Deep Networks (to explore the trade-off between search speed and evaluation accuracy).

The success criteria were defined as achieving a win rate statistically significantly higher than the original TOVOR agent's baseline of 26% against the standard opponent, MctsAi.

## Key Results

Our experiments demonstrated that a learned, non-linear reward function can significantly outperform standard hand-crafted heuristics.

| Agent (P1) | Opponent (P2) | P1 Win Rate (%) | Notes |
| :--- | :--- | :--- | :--- |
| **TOVOR (Original Baseline)** | MctsAi | 26 | Hand-crafted HP-difference heuristic. |
| **SparseDeep (Our Best Agent)** | MctsAi | **93** | Deep architecture trained on Sparse, long-horizon outcome labels. |

### Strategic Insights

*   **Learned Rewards Efficacy (H1 Supported):** The agent **SparseDeep** achieved a **93% win rate** against MctsAi, representing a massive improvement over the original TOVOR’s 26%. This validated the use of Inverse Reinforcement Learning to capture complex, implicit strategies.
*   **Reward Shaping (H3 Rejected):** We found that **Sparse rewards were critical** for high performance. Agents trained on dense rewards converged to suboptimal, "greedy" policies that prioritized maximizing damage over setting up strategic win conditions. The sparse signal allowed SparseDeep to prioritize low-damage, strategic actions (like the crouching kick) that enabled its win condition.
*   **Architecture Depth (H2 Partially Supported):** While the deep architecture was necessary for the best performance against the baseline (SparseDeep), deep networks showed failure modes against novel opponents, suggesting a need for less biased training data for generalized strategy learning.

## Operation and Running the Project

The core project uses batch files (assuming a Windows OS) to run games and collect logs.

### Running Game Simulations

The following batch files are used in the main directory:

*   `training.bat`: Runs the game in training mode without logging. This is helpful for understanding gameplay mechanics.
*   `log.bat`: Runs one full game (3 rounds) and logs the results. Useful for quick testing of changes.
*   `log-100-rounds.bat`: This file will **delete all existing log files** in `log/` before running 100 rounds between two defined AIs and logging the results. You must **edit this file** to change which two AIs are competing.

### Modifying the Custom AI

To modify the custom MCTS agent:

1.  Navigate to `AI Source Code/Custom TOVOR/`.
2.  Edit the core logic in `CustomTOVOR.java`.
3.  Instructions for compiling `CustomTOVOR` into a usable JAR file, including where to place the output file, are contained in `AI Source Code/Custom TOVOR/make.txt`. The trained neural network weights (serialized into a single JSON file) are loaded by the Java-based TOVOR agent at runtime via a helper class .
```