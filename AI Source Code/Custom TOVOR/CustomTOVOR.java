import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;

import aiinterface.AIInterface;
import aiinterface.CommandCenter;
import struct.FrameData;
import struct.GameData;
import struct.Key;
import enumerate.Action;
import simulator.Simulator;

public class CustomTOVOR implements AIInterface {
	
	Key key;
	CommandCenter cc;
	boolean cPlayer;
	FrameData fd;
	Simulator sim;
	Random rand;

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void getInformation(FrameData arg0, boolean arg1) {
		// TODO Auto-generated method stub
		fd = arg0;
		cc.setFrameData(fd, cPlayer);
	}

	@Override
	public int initialize(GameData arg0, boolean arg1) {
		// TODO Auto-generated method stub
		key = new Key();
		cc = new CommandCenter();
		cPlayer = arg1;
		fd = new FrameData();
		sim = new Simulator(arg0);
		rand = new Random();
		return 0;
	}

	@Override
	public Key input() {
		// TODO Auto-generated method stub
		return key;
	}

	@Override
	public void processing() {
		// TODO Auto-generated method stub
		if(!fd.getEmptyFlag() && fd.getRemainingFramesNumber()>0) 
		{
			// Custom code: Idk what this does but it was in BlackMamba
			// This is commented out so that CustomTOVOR is still like TOVOR besides the heuristic
			// if(this.cc.getSkillFlag())
            // {
            //     this.key = this.cc.getSkillKey();
            //     return;
            // }
			// this.key.empty();
			// this.cc.skillCancel();
			// Custom code ends here
			
			McTree mcTree = new McTree();
			mcTree.getStartNode().setFrameData(fd);
			long startTime = System.currentTimeMillis();
			long elapsedTime = 0L;
			while (elapsedTime < 16)
			{
				treeSearch(mcTree,fd);
				elapsedTime = (new Date()).getTime() - startTime;
			}
			Action newAction = mcTree.getStartNode().ucb1Select().getAction();
			cc.commandCall(newAction.toString());
			key = cc.getSkillKey();
		}

	}
	
	public Action randomAction()
	{
		return Action.values()[rand.nextInt(Action.values().length - 1)];
		// Instead of predicting that the opponent will use a random action, we can predict a specific action
		// Predicting that the opponent will stand still could be useful (though not perfect)
		// return Action.STAND_GUARD;
	}
	
	public void treeSearch(McTree tree, FrameData fd)
	{
		McNode currNode = tree.getStartNode();
		
		//Node selection
		while (!currNode.isLeafNode())
		{
			currNode = currNode.ucb1Select();
		}
		if(currNode.getVisits() != 0)
		{
			currNode.expand();
			currNode = currNode.getChildren().get(0);  
		}
		
		//Simulation
		LinkedList<Action> myAct = new LinkedList<Action>();
		myAct.add(currNode.getAction());
		LinkedList<Action> oppAct = new LinkedList<Action>();
		oppAct.add(randomAction());
		FrameData cfd = currNode.parent.getFrameData();
		FrameData sfd = sim.simulate(cfd, cPlayer, myAct, oppAct, 35);
		// Current modification: Original heuristic * 1
		// You can change the 1 to something like -1 to change the behavior of CustomTOVOR
		// TODO: Change this heuristic to the learned (optimal) heuristic
		int score = 1 * (cfd.getCharacter(cPlayer).getHp() - sfd.getCharacter(cPlayer).getHp()
				- (cfd.getCharacter(!cPlayer).getHp() - sfd.getCharacter(!cPlayer).getHp()));
		// Another modification: If the current action is sweep or solar plexus, weight it heavily and positively
		// Update: This worked!
		// if (currNode.getAction() == Action.CROUCH_FB || currNode.getAction() == Action.STAND_FA) {
		// 	score = Math.abs(score) * 1000;
		// }
		currNode.setResult(score);
		currNode.setFrameData(sfd);
		currNode.visit();
		
		//Back-propagation
		while(currNode.getParent() != null)
		{
			McNode parent = currNode.getParent();
			parent.visit();
			parent.setResult(parent.getResult() + currNode.getResult());
			currNode = parent;
		}
	}

	@Override
	public void roundEnd(int arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	private class McNode
	{
		private McNode parent; 
		private ArrayList<McNode> children;
		private Action mcAction;
		private int t;
		private int n;
		private FrameData fd;
		
		public McNode(McNode par, Action act)
		{
			parent = par;
			mcAction = act;
			t = 0;
			n = 0;
			children = new ArrayList<McNode>();
			fd = null;
		}
		
		public McNode()
		{
			parent = null;
			mcAction = null;
			t = 0;
			n = 0;
			children = new ArrayList<McNode>();
			fd = null;
		}
		
		public McNode getParent() 
		{
			return parent;
		}
		
		public ArrayList<McNode> getChildren()
		{
			return children;
		}
		
		public void addChild(Action chAct)
		{
			children.add(new McNode(this,chAct));
		}
		
		public Action getAction()
		{
			return mcAction;
		}
		
		public int getResult()
		{
			return t;
		}
		
		public void setResult(int result)
		{
			t = result;
		}
		
		public FrameData getFrameData()
		{
			return fd;
		}
		
		public void setFrameData(FrameData ifd)
		{
			fd = ifd;
		}
		
		public boolean isLeafNode()
		{
			return (children.size() == 0);
		}
		
		public void expand()
		{
			for (Action act : Action.values())
			{
				addChild(act);
			}
		}
		
		public McNode ucb1Select()
		{
			Double max = Double.NEGATIVE_INFINITY;
			int maxInd = -1;
			for (int i=0;i<children.size();i++)
			{
				Double ucb1 = 0d;
				McNode child = children.get(i);
				if(child.n == 0)
				{
					ucb1 = Double.MAX_VALUE;
				}
				else 
				{
					ucb1 = child.getResult()/child.getVisits() + 2 * Math.sqrt(Math.log(n)/child.getVisits());
				}
				if(ucb1 > max)
				{
					max = ucb1;
					maxInd = i;
				}
			}
			if(maxInd > -1) 
			{
				return children.get(maxInd);
			}
			else
			{
				return null;
			}
		}
		
		public void visit()
		{
			n++;
		}
		
		public int getVisits() 
		{
			return n;
		}
	}
	
	private class McTree
	{
		private McNode mcStart;
		
		public McTree() 
		{
			mcStart = new McNode();
			mcStart.expand();
		}
		
		public McNode getStartNode()
		{
			return mcStart;
		}
	}
}
