#BlackMamba

##INTRODUCTION
BlackMamba is a Deep Reinforcement Learning agent. It was trained by fighting with participants in the past few years and self-play using the PPO algorithm.  
 
The policy network is a simple 6-layer MLP.

##HOW TO RUN

BlackMamba is a Java AI. To run it, please put `BlackMamba.jar` into the folder `data/ai/` and put the folder `BlackMamba` into `data/aiData/`. Now you can run a game by launching the FightingICE and select BlackMamba and an opponent. 

Our submission is organized as follows:

```
submit/
├── BlackMamba/
│   ├── ZEN/
│   │   ├──normal/
│   │   │  ├──bias_l{1-6}.csv
│   │   │  └──weight_l{1-6}.csv
│   │   └──speed/
│   │      ├──bias_l{1-6}.csv
│   │      └──weight_l{1-6}.csv
│   ├── GARNET/
│   │   ├──normal/
│   │   │  ├──bias_l{1-6}.csv
│   │   │  └──weight_l{1-6}.csv
│   │   └──speed/
│   │      ├──bias_l{1-6}.csv
│   │      └──weight_l{1-6}.csv
│   └── LUD/
│       ├──normal/
│       │  ├──bias_l{1-6}.csv
│       │  └──weight_l{1-6}.csv
│       └──speed/
│          ├──bias_l{1-6}.csv
│          └──weight_l{1-6}.csv
├── code/
├── BlackMamba.jar
└── README.md
```



