import java.util.Random;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

import aiinterface.AIInterface;
import aiinterface.CommandCenter;
import enumerate.Action;
import struct.*;

import network.*;
import structs.InputData;
import util.Matrix2D;



public class BlackMamba implements AIInterface {
    Network network;
    private boolean frameskip;
    private boolean isGameJustStarted;
    private boolean isControl;
    private ScreenData screenData;
    private Action[] myaction;
    String ourCharacter;
    boolean speedmode;


    private Key key;
    private CommandCenter commandCenter;
    private boolean playerNumber;
    private Random policy_r = new Random();


    private FrameData frameData;
//    private CharacterData myCharacter;
//    private CharacterData oppCharacter;

    private double total_time = 0.0;
    private double total_forward_time = 0.0;
    private int total_step = 0;
//    private int timeout_count = 0;


    @Override
    public void close() {

    }

    @Override
    public void getInformation(FrameData frameData, boolean isControl) {
        this.isControl = isControl;
        this.frameData = frameData;
        this.commandCenter.setFrameData(this.frameData, this.playerNumber);
        if(frameData.getEmptyFlag())
            return;
    }

    @Override
    public int initialize(GameData gameData, boolean playerNumber) {
        this.frameskip = false;
        this.playerNumber = playerNumber;
        this.ourCharacter = gameData.getCharacterName(this.playerNumber);
        this.speedmode = (gameData.getAiName(!this.playerNumber).equals("MctsAi") ||
                             gameData.getAiName(!this.playerNumber).equals("SampleMctsAi") ||
                             gameData.getAiName(!this.playerNumber).equals("WeakMctsAi"));
        myaction = new Action[] {
                Action.AIR,
                Action.AIR_A,
                Action.AIR_B,
                Action.AIR_D_DB_BA,
                Action.AIR_D_DB_BB,
                Action.AIR_D_DF_FA,
                Action.AIR_D_DF_FB,
                Action.AIR_DA,
                Action.AIR_DB,
                Action.AIR_F_D_DFA,
                Action.AIR_F_D_DFB,
                Action.AIR_FA,
                Action.AIR_FB,
                Action.AIR_GUARD,
                Action.AIR_GUARD_RECOV,
                Action.AIR_RECOV,
                Action.AIR_UA,
                Action.AIR_UB,
                Action.BACK_JUMP,
                Action.BACK_STEP,
                Action.CHANGE_DOWN,
                Action.CROUCH,
                Action.CROUCH_A,
                Action.CROUCH_B,
                Action.CROUCH_FA,
                Action.CROUCH_FB,
                Action.CROUCH_GUARD,
                Action.CROUCH_GUARD_RECOV,
                Action.CROUCH_RECOV,
                Action.DASH,
                Action.DOWN,
                Action.FOR_JUMP,
                Action.FORWARD_WALK,
                Action.JUMP,
                Action.LANDING,
                Action.NEUTRAL,
                Action.RISE,
                Action.STAND,
                Action.STAND_A,
                Action.STAND_B,
                Action.STAND_D_DB_BA,
                Action.STAND_D_DB_BB,
                Action.STAND_D_DF_FA,
                Action.STAND_D_DF_FB,
                Action.STAND_D_DF_FC,
                Action.STAND_F_D_DFA,
                Action.STAND_F_D_DFB,
                Action.STAND_FA,
                Action.STAND_FB,
                Action.STAND_GUARD,
                Action.STAND_GUARD_RECOV,
                Action.STAND_RECOV,
                Action.THROW_A,
                Action.THROW_B,
                Action.THROW_HIT,
                Action.THROW_SUFFER,
        };

        this.key = new Key();
        this.frameData = new FrameData();
        this.commandCenter = new CommandCenter();

        this.network = new Network();

        Layer l1 = new Layer(143, 800, new LeakyReLU());
        Layer l2 = new Layer(800, 600, new LeakyReLU());
        Layer l3 = new Layer(600, 400, new LeakyReLU());
        Layer l4 = new Layer(400, 200, new LeakyReLU());
        Layer l5 = new Layer(200, 100, new LeakyReLU());
        Layer l6 = new Layer(100, 56, new None());
        String root = "data/aiData/BlackMamba/" + ourCharacter + "/";
        if(this.speedmode){
            root = root + "/speed/";
            System.out.println("running with speed mode...");
        }
        else{
            root = root + "/normal/";
            System.out.println("running with normal mode...");
        }

        l1.loadWeight(root + "weight_l1.csv");
        l1.loadBias(root + "bias_l1.csv");
        l2.loadWeight(root + "weight_l2.csv");
        l2.loadBias(root + "bias_l2.csv");
        l3.loadWeight(root + "weight_l3.csv");
        l3.loadBias(root + "bias_l3.csv");
        l4.loadWeight(root + "weight_l4.csv");
        l4.loadBias(root + "bias_l4.csv");
        l5.loadWeight(root + "weight_l5.csv");
        l5.loadBias(root + "bias_l5.csv");
        l6.loadWeight(root + "weight_l6.csv");
        l6.loadBias(root + "bias_l6.csv");

        network.addLayer(l1);
        network.addLayer(l2);
        network.addLayer(l3);
        network.addLayer(l4);
        network.addLayer(l5);
        network.addLayer(l6);

        this.isGameJustStarted = true;

        return 0;
    }

    @Override
    public Key input() {
        return key;
    }

    public void gameEnd(){

    }

    public void getScreenData(ScreenData sd){
        this.screenData = sd;
    }

    @Override
    public void processing() {
//        System.out.println("Processing!");
        double time = System.currentTimeMillis();

        if(this.frameData.getEmptyFlag() || this.frameData.getRemainingTime() <= 0) {
            this.isGameJustStarted = true;
            return;
        }

        if(this.frameskip) {
            if(this.commandCenter.getSkillFlag())
            {
                this.key = this.commandCenter.getSkillKey();
                return;
            }
            if(!this.isControl)
            {
                return;
            }
            this.key.empty();
            this.commandCenter.skillCancel();
        }

        InputData temp = new InputData(frameData, playerNumber);
        double[][] inputs = new Matrix2D(temp.Input).getArrays();
//        System.out.println(new Matrix2D(temp.Input));
//        double ss = System.currentTimeMillis();
        double[][] outputs = this.network.forward(inputs);
//        double f_time = System.currentTimeMillis() - ss;
//        System.out.println(new Matrix2D(outputs));
        int action_n = 0;
        if(this.speedmode){
            if(ourCharacter.equals("ZEN")){
                action_n = choose_action(outputs);
            }
            if(ourCharacter.equals("GARNET")){
                action_n = choose_action(outputs);
            }
            if(ourCharacter.equals("LUD")){
                double max_value = -99999.0;
                for(int i = 0; i < outputs.length; i++){
                    if(max_value < outputs[i][0]){
                        max_value = outputs[i][0];
                        action_n = i;
                    }
                }
            }

        }else{
            if(ourCharacter.equals("GARNET") || ourCharacter.equals("LUD")){
                double max_value = -99999.0;
                for(int i = 0; i < outputs.length; i++){
                    if(max_value < outputs[i][0]){
                        max_value = outputs[i][0];
                        action_n = i;
                    }
                }
            }else {
                action_n = choose_action(outputs);
            }
        }


        this.commandCenter.commandCall(myaction[action_n].name());
        if(!this.frameskip)
            this.key = this.commandCenter.getSkillKey();
        double processing_time = System.currentTimeMillis() - time;
        this.total_step += 1;
        this.total_time += processing_time;
//        this.total_forward_time += f_time;

    }

    @Override
    public void roundEnd(int p1Hp, int p2Hp, int frames) {
//        System.out.println("num act:");
//        System.out.println(num_act);
        System.out.println("total step: " + this.total_step);
        System.out.println("mean processing time:" + this.total_time / this.total_step);
        System.out.println("mean forward time:" + this.total_forward_time / this.total_step);

        this.total_step = 0;
//        this.timeout_count = 0;
        this.total_time = 0.0;
        this.total_forward_time = 0.0;
        System.out.println(p1Hp);
        System.out.println(p2Hp);
        System.out.println(frames);
    }

    private int choose_action(double[][] policy_output) {
        int action_n = 0;
        int count = 0;
        double max_a = -100;
        int action_max = 0;
        double d = policy_r.nextDouble();
        double sum_p = 0;
        double sum_temp = 0;
        for (int i = 0; i < policy_output.length; i++) {
            sum_temp += Math.exp(policy_output[i][0]-10);
            if(max_a < policy_output[i][0]) {
                action_max = i;
                max_a = policy_output[i][0];
            }
        }
        for (int i = 0; i < policy_output.length; i++) {
            if(sum_p < d){
                action_n=i;
                sum_p += Math.exp(policy_output[i][0]-10)/sum_temp;
            }
            else if(count<3) {
                if(sum_p < 0.5) {
                    count=3;
                    action_n = action_max;
                    break;
                }
            }
        }
//		for (int i = 0; i < policy_output.length; i++) {
//			if(max_a < policy_output[i][0]) {
//				max_a = policy_output[i][0];
//				action_n = i;
//			}
//		}
        return action_n;
    }

}
