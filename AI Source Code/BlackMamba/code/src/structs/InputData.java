package structs;

import java.util.Iterator;
import java.util.List;

import struct.AttackData;
import struct.CharacterData;
import struct.FrameData;
import java.util.ArrayList;


public class InputData {
	public double[] Input;

	public InputData(FrameData frameData, boolean player) {
		Input = convert(frameData, player);
	}


	double[] convert(FrameData frameData, boolean player) {

		ArrayList<Double> input_list = new ArrayList<>();
		CharacterData my = frameData.getCharacter(player);
		CharacterData opp = frameData.getCharacter(!player);

		// my info
		double myHp = Math.abs((double)my.getHp() / 400);
		double myEnergy = (double)my.getEnergy() / 300;
		double myX = (((double)my.getLeft() + (double)my.getRight()) / 2) / 960;
		double myY = (((double)my.getBottom() + (double)my.getTop()) / 2) / 640;
		double mySpeedX = ((double)my.getSpeedX()) / 15;
		double mySpeedY = ((double)my.getSpeedY()) / 28;
		int myState = my.getAction().ordinal();
		double myRemainingFrame = (double)my.getRemainingFrame() / 70;

		// opp info
		double oppHp = Math.abs((double)opp.getHp() / 400);
		double oppEnergy = (double)opp.getEnergy() / 300;
		double oppX = (((double)opp.getLeft() + (double)opp.getRight()) / 2) / 960;
		double oppY = (((double)opp.getBottom() + (double)opp.getTop()) / 2) / 640;
		double oppSpeedX = ((double)opp.getSpeedX()) / 15;
		double oppSpeedY = ((double)opp.getSpeedY()) / 28;
		int oppState = opp.getAction().ordinal();
		double oppRemainingFrame = (double)opp.getRemainingFrame() / 70;

		// time info
		double game_frame_num = (double)frameData.getFramesNumber() / 3600;

		input_list.add(myHp);
		input_list.add(myEnergy);
		input_list.add(myX);
		input_list.add(myY);
		if(mySpeedX < 0)
			input_list.add(0.0);
		else
			input_list.add(1.0);
		input_list.add(Math.abs(mySpeedX));
		if(mySpeedY < 0)
			input_list.add(0.0);
		else
			input_list.add(1.0);
		input_list.add(Math.abs(mySpeedY));
		for(int i = 0; i < 56; i++){
			if(i == myState)
				input_list.add(1.0);
			else
				input_list.add(0.0);
		}
		input_list.add(myRemainingFrame);

		input_list.add(oppHp);
		input_list.add(oppEnergy);
		input_list.add(oppX);
		input_list.add(oppY);
		if(oppSpeedX < 0)
			input_list.add(0.0);
		else
			input_list.add(1.0);
		input_list.add(Math.abs(oppSpeedX));
		if(oppSpeedY < 0)
			input_list.add(0.0);
		else
			input_list.add(1.0);
		input_list.add(Math.abs(oppSpeedY));
		for(int i = 0; i < 56; i++){
			if(i == oppState)
				input_list.add(1.0);
			else
				input_list.add(0.0);
		}
		input_list.add(oppRemainingFrame);

		input_list.add(game_frame_num);

		List<AttackData> myAttack =
				new ArrayList<>(player ? frameData.getProjectilesByP1() : frameData.getProjectilesByP2());
		List<AttackData> oppAttack =
				new ArrayList<>(player ? frameData.getProjectilesByP2() : frameData.getProjectilesByP1());

		for(int i = 0; i < 2; i++){
			if (myAttack.size() > i){
				AttackData tmp = myAttack.get(i);
				input_list.add((double)tmp.getHitDamage() / 200.0);
				input_list.add(
						(((double)tmp.getCurrentHitArea().getLeft() + (double)tmp.getCurrentHitArea().getRight()) / 2) / 960.0
				);
				input_list.add(
						(((double)tmp.getCurrentHitArea().getTop() + (double)tmp.getCurrentHitArea().getBottom()) / 2) / 640.0
				);
			}
			else{
				input_list.add(0.0);
				input_list.add(0.0);
				input_list.add(0.0);
			}
		}

		for(int i = 0; i < 2; i++){
			if (oppAttack.size() > i){
				AttackData tmp = oppAttack.get(i);
				input_list.add((double)tmp.getHitDamage() / 200.0);
				input_list.add(
						(((double)tmp.getCurrentHitArea().getLeft() + (double)tmp.getCurrentHitArea().getRight()) / 2) / 960.0
				);
				input_list.add(
						(((double)tmp.getCurrentHitArea().getTop() + (double)tmp.getCurrentHitArea().getBottom()) / 2) / 640.0
				);
			}
			else{
				input_list.add(0.0);
				input_list.add(0.0);
				input_list.add(0.0);
			}
		}

		for(int i = 0; i < input_list.size(); i++){
			if(input_list.get(i) > 1.0)
				input_list.set(i, 1.0);
			if(input_list.get(i) < 0.0)
				input_list.set(i, 0.0);
		}



		Input = toArr(input_list);
		return Input;

	}

	public static double[] toArr(List<Double> list) {
		int l = list.size();
		double[] arr = new double[l];
		Iterator<Double> iter = list.iterator();
		for (int i = 0; i < l; i++)
			arr[i] = iter.next();
		return arr;
	}

}
