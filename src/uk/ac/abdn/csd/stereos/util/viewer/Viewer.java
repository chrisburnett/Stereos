package uk.ac.abdn.csd.stereos.util.viewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import processing.core.*;
import processing.pdf.*;
import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.Profile;
import uk.ac.abdn.csd.stereos.trust.sl.Opinion; //import processing.xml.*; 
//
//import java.applet.*; 

//import java.awt.image.*; 
import java.awt.event.*;

/**
 * Alright - what does the viewer need to provide as methods to the experiment
 * class?
 * 
 * 1) Ability to add and remove nodes and relationships
 * 
 * 
 * Requirement - the viewer class must store each action as a 'page' or a new
 * state or something so that the thing can be played through using left or
 * right arrow keys. How to implement this? Well, the order of things needs to
 * be preserved - so something like a stack structure is required.
 * 
 * We need an 'action' class to store events. The viewer will put these in a
 * stack and then when playing them, pop them off.
 * 
 * Subclasses of action will be things like "newnodeaction", "interactionAction"
 * "nodeleaveaction", "teamchangeaction" and a polymorphic (overloaded) method
 * will handle this.
 * 
 * 
 */

@SuppressWarnings( { "serial", "unused" })
public class Viewer extends PApplet
{

	// private static final int AGENT_COLOR = 0xFF2D00E8;
	private static final int AGENT_COLOR = 0xFFFFFFFF;
	private static final int TRUSTOR_COLOR = 0xFFFF8D00;
	//private static final int TRUSTOR_COLOR = 0xFFFFFFFF;
	private static final int TRUSTOR_RING_COLOR = 0xFFFFC062;

	// Window size
	int xsize;
	int ysize;

	// Buttons
	RectButton playButton, arcButton, saveButton;

	// parameters to control the drawing
	// private int vel = 5;
	private float k;
	// private float k2;
	private float t;
	private float tmax = 13;
	// private float tMass;
	// private int curn;
	private int nn;
	// private float curMass;
	// private int im;
	private float accel;

	private static final int CIRCLE_PAD = 15;

	// Current state
	int currentStateId = 0;
	// indicates that the user has changed the state and the nodes should be
	// redrawn
	// update is initially set so that the first state gets loaded
	boolean update = true;
	boolean play = false;
	boolean save = false;
	boolean showInterTeamArcs = true;

	int playSpeed = 20;
	// counters - it is for the cooling function
	int i, it;

	// fonts for text
	PFont font, tiny, medium;

	// the actual data storage
	private List<State> data;
	private Map<Agent, Node> nodes;
	private List<Arc> arcs;
	private Map<Profile, Integer> profileColors;

	/**
	 * Instantiate the viewer with the data
	 * 
	 * @param xsize
	 * @param ysize
	 */
	public Viewer(int xsize, int ysize)
	{
		super();

		this.xsize = xsize;
		this.ysize = ysize;
		// initialise the empty data
		this.data = null;
		this.profileColors = new HashMap<Profile, Integer>();
		playButton = new RectButton("Play", 50, ysize - 60, 50, 25, 0xFFFFFFFF, 0xFFA2CBFF, 0xFF87FA77);
		arcButton = new RectButton("Arcs", 100, ysize - 60, 50, 25, 0xFFFFFFFF, 0xFFA2CBFF, 0xFF87FA77);
		saveButton = new RectButton("Save", 150, ysize - 60, 50, 25, 0xFFFFFFFF, 0xFFA2CBFF, 0xFF87FA77);

		// current increment (for playback)
		i = 0;
		// parameters for the cooling function
		it = 0;
		t = 1;
		// acceleration when correcting the drawing (was 0.93)
		accel = 0.97f;
	}

	/**
	 * Setup the frame
	 */
	public void setup()
	{
		smooth();
		size(xsize, ysize);

		// clear the node lists
		this.arcs = new ArrayList<Arc>(0);
		this.nodes = new HashMap<Agent, Node>(0);

		// initialise fonts
		font = createFont("ArialMT", 20);
		medium = createFont("ArialMT", 10);
		tiny = createFont("ArialMT", 9);
		textAlign(CENTER, TOP);
	}

	// add data to the viewer
	public void setData(List<State> data)
	{
		this.data = data;
	}

	public List<State> getData()
	{
		return data;
	}

	private String getStatusText()
	{
		if (data == null)
			return "Running...";
		return "Current step: " + currentStateId; // this.currentStateId;
	}

	/**
	 * Main drawing loop
	 */
	public void draw()
	{
		// keep going until energy is exhausted
		background(255);
		textFont(medium);
		fill(0);
		stroke(0);
		text(getStatusText(), xsize / 2, ysize - 50);

		// deal with buttons
		playButton.update();
		playButton.display();
		arcButton.update();
		arcButton.display();
		saveButton.update();
		saveButton.display();

		if (data != null) {

			// if save was clicked, save the screen to PDF
			if (save) {
				// saveFrame("experiments/images/output-####.png");
				beginRecord(PDF, "experiments/images/output.pdf");
				saveButton.setPressed(false);
			}

			// if we are playing, shove the thing on
			if (play && i++ % playSpeed == 0 && currentStateId < data.size()) {
				currentStateId++;
				update = true;
			}
			// the currently displayed state
			State currentState = data.get(currentStateId);

			// if the update flag has been set then update the data from the new
			// state
			if (update)
				doUpdate(currentState);

			// repulsive forces
			noStroke();
			for (Node u : nodes.values()) {
				for (Node v : nodes.values()) {
					if (u != v) {
						Vector2D delta = v.pos.sub(u.pos);
						if (delta.norm() != 0)
							v.disp.addSelf(delta.versor().mult(fr(v.mass, u.mass, delta.norm())));
					}
				}
			}

			// attractive
			for (Arc e : arcs) {
				if (e.active) {
					Vector2D delta = e.a.pos.sub(e.b.pos);
					if (delta.norm() != 0) {
						e.a.disp.subSelf(delta.versor().mult(fa(e.a.mass, e.b.mass, delta.norm())));
						e.b.disp.addSelf(delta.versor().mult(fa(e.a.mass, e.b.mass, delta.norm())));
					}
				}
			}

			// cooling and constraining
			for (Node u : nodes.values()) {
				// u.pos =
				// u.pos.add(u.disp.div(u.disp.norm()).mult(min(u.disp.norm(),t)));
				u.update();
				u.costrain(0, width, 0, height);
			}

			// draw team circles under everything else
			List<TeamCircle> tcs = drawTeamCircles(currentState);

			// do collision detection and stop motion if there are no collisions
			if (areCollisions(tcs)) {
				if (t <= tmax)
					t = it++;// t = cool(it++);
				else
					t = tmax;
			} else {
				// decellerate at 80 percent normal speed
				t = t * (accel * 0.8f);
				it = 0;
			}

			// draw arcs
			for (Arc a : arcs)
				a.draw();

			// draw nodes
			ellipseMode(CENTER);
			for (Entry<Agent, Node> e : nodes.entrySet())
				e.getValue().draw();

			if (save) {
				endRecord();
				save = false;
			}
		}
	}

	private boolean areCollisions(List<TeamCircle> tcs)
	{
		// for each pair we need to check
		for (TeamCircle a : tcs) {
			for (TeamCircle b : tcs) {
				if (a != b) {
					// These next variables are for the math work and results
					float distance_squared;
					float radii_squared;

					distance_squared = ((a.x - b.x) * (a.x - b.x)) + ((a.y - b.y) * (a.y - b.y));

					// Multiplication is faster than taking a square root
					radii_squared = (a.rad + b.rad) * (a.rad + b.rad);

					if (radii_squared > distance_squared)
						return true;
				}
			}

			for (Agent ag : a.team) {
				// for each agent in the team, if the agent is further away from
				// the centre of the circle
				// than the radius, then keep cooling
				Node agNode = nodes.get(ag);
				double dist_from_centre = Math.sqrt(Math.pow(agNode.pos.x - a.x, 2) + Math.pow(agNode.pos.y - a.y, 2));
				if (dist_from_centre < (double) a.rad)
					return true;
			}
		}
		// if no collisions were found, return false
		return false;
	}

	private float cool(float time)
	{
		return (xsize) * (time * 2);
		// team cooling:
		// only cool the energy once there's no overlap between teams
		// we need to check each circle against the other
	}

	private List<TeamCircle> drawTeamCircles(State currentState)
	{
		int teamSize = currentState.getTeams().size();

		List<TeamCircle> circles = new ArrayList<TeamCircle>(teamSize);

		// for each team
		// identify the median point
		// find the farthest agent and set radius to this + pad
		for (List<Agent> team : currentState.getTeams()) {
			if (team.size() > 0) {
				float sumx = 0, sumy = 0, maxx = 0, maxy = 0;
				float meanx, meany;
				float distx, disty;
				float size;
				// identify mean point (centroid)
				for (Agent a : team) {
					Node an = nodes.get(a);
					sumx += an.pos.x;
					sumy += an.pos.y;

					// record the outliers
					if (an.pos.x > maxx)
						maxx = an.pos.x;
					if (an.pos.y > maxy)
						maxy = an.pos.y;

				}
				meanx = sumx / team.size();
				meany = sumy / team.size();

				distx = meanx - maxx;
				disty = meany - maxy;

				size = max(distx, disty) - CIRCLE_PAD;

				// now draw the circle
				stroke(0, 130, 255, 255);
				fill(0, 130, 255, 10);
				ellipseMode(RADIUS);
				ellipse(meanx, meany, size, size);

				// add to collision detection table
				circles.add(new TeamCircle(meanx, meany, size, team));
			}
		}
		return circles;
	}

	// motion functions
	// attract
	float fa(float m1, float m2, float z)
	{
		return .0005f * pow(k - m1 - m2 - z, 2); // was .0009
		// return .1f*pow(m1*m2,2)/pow(z,2);
	}

	// repel
	float fr(float m1, float m2, float z)
	{
		return 0.2f * pow(m1 + m2 + k, 2) / pow(z, 2);
		// return 20*(m1*m2)/pow(z,2);
	}

	// backup
	// //repel
	// float fr(float m1, float m2, float z){
	// return 0.3f*pow(m1+m2+k,2)/pow(z,2);
	// //return 20*(m1*m2)/pow(z,2);
	// }

	private int getProfileColor(Profile p)
	{
		// if we already have a colour, return it
		if (profileColors.containsKey(p))
			return profileColors.get(p).intValue();

		int newColor;
		// otherwise make one
		float m = (float) p.getDefaultMeanPerformance();
		float v = (float) p.getDefaultVariance();

		float t, r, g;

		t = (float) Math.round(200 * (1 - v));
		if (m > 0.5) {
			r = 1 - m - v;
			g = 1 - v;
		} else {
			r = 1 - v;
			g = 1 - (0.5f - m) - v;
		}

		newColor = color(r * 200, g * 200, 0, t);
		profileColors.put(p, newColor);
		return newColor;
	}

	public void doUpdate(State currentState)
	{
		Set<Agent> currentAgents = new HashSet<Agent>(nodes.keySet());
		// remove departed agents
		for (Agent a : currentAgents) {
			if (!currentState.getAgents().contains(a) && !currentState.getTrustors().contains(a))
				nodes.remove(a);
			boolean teamed = false;
			for (List<Agent> team : currentState.getTeams())
				if (team.contains(a))
					teamed = true;
			if (!teamed)
				nodes.remove(a);
		}
		// For each agent, add a node
		// for(Agent a : currentState.getAgents())
		// {
		// // add a new node for that agent if not already in
		// if(!nodes.containsKey(a))
		// nodes.put(a, new
		// Node(random(xsize/2-xsize/3.2f,xsize/2+xsize/3.2f),random(ysize/2-ysize/3,ysize/2+ysize/3),5,AGENT_COLOR,getProfileColor(a.getProfile()))
		// );
		// }

		// only add agents in teams
		for (List<Agent> team : currentState.getTeams())
			for (Agent a : team)
				// add a new node for that agent if not already in
				if (!nodes.containsKey(a) && a.getRole() == Agent.TRUSTEE)
					// note: consider replacing the distance from centre
					// parameter (denominators) with a constant
					nodes.put(a, new Node(random(xsize / 2 - xsize / 3, xsize / 2 + xsize / 3), random(ysize / 2 - ysize / 3, ysize / 2 + ysize / 3), 5, AGENT_COLOR, getProfileColor(a.getProfile())));
		// nodes.put(a, new
		// Node(random(xsize/2,xsize/3),random(ysize/2,ysize/3),5,AGENT_COLOR,getProfileColor(a.getProfile()))
		// );
		// nodes.put(a, new
		// Node(xsize/2f,ysize/2f,5,AGENT_COLOR,getProfileColor(a.getProfile())));
		// nodes.put(a, new
		// Node(random(xsize/2-10,ysize/2+10),random(ysize/2+10,ysize/2-10),5,TRUSTOR_COLOR,TRUSTOR_RING_COLOR)
		// );

		updateArcs(currentState);

		reJig(currentState);

		// Reset the update flag until a key has been pressed
		update = false;
	}

	public void updateArcs(State currentState)
	{
		arcs = new ArrayList<Arc>();
		// for each trustor, add the opinion arcs
		for (Agent t : currentState.getTrustors()) {
			// will be null if this trustor didnt interact
			Agent partner = currentState.getInteractors().get(t);
			// add a new node for that trustor if it's not already present
			if (!nodes.containsKey(t))
				// nodes.put(t, new
				// Node(random(xsize/2-xsize/3,xsize/2+xsize/3),random(ysize/2-ysize/3,ysize/2+ysize/3),5,TRUSTOR_COLOR,TRUSTOR_RING_COLOR)
				// );
				// nodes.put(t, new
				// Node(random(xsize/2-xsize/3,xsize/2+xsize/3),random(ysize/2-ysize/3,ysize/2+ysize/3),5,);
				nodes.put(t, new Node(random(xsize / 2 - xsize / 3, xsize / 2 + xsize / 3), random(ysize / 2 - ysize
						/ 3, ysize / 2 + ysize / 3), 5, TRUSTOR_COLOR, TRUSTOR_RING_COLOR));

			// get the opinions of the agents and add the arcs
			Map<Agent, Opinion> tops = currentState.getOpinions().get(t);
			// for each opinion of the current trustor
			for (Entry<Agent, Opinion> e : tops.entrySet()) {
				Opinion op = e.getValue();
				Agent a = e.getKey();
				// check to see if this was a partner in the current state
				// then we'll mark the arc differently
				boolean isPartner = false;
				if (partner != null && partner.equals(a))
					isPartner = true;
				// do we want to see the inter-team arcs?
				if (op.getUncertainty() < 1 && nodes.containsKey(a))
					if ((!showInterTeamArcs && currentState.getAssignments().get(t).contains(a)) || showInterTeamArcs)
						arcs.add(new OpinionArc(nodes.get(t), nodes.get(a), op, true, false, isPartner));
			}
		}

		// now add the arcs for teams
		for (List<Agent> team : currentState.getTeams())
			for (Agent x : team)
				for (Agent y : team)
					if (x != y)
						arcs.add(new Arc(nodes.get(x), nodes.get(y), false, true));
	}

	// reset the motion parameters to 'jiggle' the nodes
	private void reJig(State currentState)
	{
		// (re)set the drawing parameters if things have changed

		if (currentStateId == 0 || data.get(currentStateId - 1).numAgents() != nn) {
			nn = currentState.numAgents();
			k = sqrt(width * height / nn) * 0.5f;
		}

		// if(currentStateId==0 ||
		// !data.get(currentStateId-1).getAssignments().equals(currentState.getAssignments()))
		// {
		// t = xsize/10;
		// t=1;
		t = xsize / 120;

	}

	/**
	 * Handle keypresses
	 */
	public void keyPressed(KeyEvent e)
	{
		if (data != null)
			if (isLeft(e) && currentStateId > 0) {
				currentStateId--;
				update = true;
			} else if (isRight(e) && (currentStateId < data.size() - 1)) {
				currentStateId++;
				update = true;
			}
	}

	public void mouseClicked()
	{
		// flick state
		if (playButton.pressed())
			play = !play;
		if (arcButton.pressed()) {
			showInterTeamArcs = !showInterTeamArcs;
			updateArcs(data.get(currentStateId));
		}
		// this method might seem awkward (setting flags) but it's so that the
		// file gets saved at the end of the drawing cycle.
		if (saveButton.pressed()) {
			save = true;
		}

	}

	boolean isLeft(KeyEvent e)
	{
		return e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_KP_LEFT;
	}

	boolean isRight(KeyEvent e)
	{
		return e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_KP_RIGHT;
	}

	static public void main(String args[])
	{
		PApplet.main(new String[] { "--bgcolor=#FFFFFF", "uk.ac.abdn.csd.stereos.util.viewer.Viewer" });
	}

	/**
	 * This inner class represents a node in the viewer's graph. It needs to be
	 * an inner class to take advantage of Processing's simplifications.....
	 * 
	 * @author cburnett
	 * 
	 */
	class Node
	{
		Vector2D pos;
		Vector2D disp;
		Vector2D[] oldpos;
		float mass;
		float newmass;
		int innerColor;
		int outerColor;
		boolean trail;
		boolean ball;

		Node(float _x, float _y, float _mass, int nodeColor, int ringColor)
		{
			pos = new Vector2D(_x, _y);
			disp = new Vector2D();
			mass = _mass;
			oldpos = new Vector2D[8];
			for (int i = 0; i < oldpos.length; i++)
				oldpos[i] = pos.clone();
			innerColor = nodeColor;
			outerColor = ringColor;
			ball = true;
			trail = false;
		}

		void incrMass(float nm)
		{
			newmass = mass + nm;
		}

		void setBall(boolean ball)
		{
			this.ball = ball;
		}

		void setTrail(boolean trail)
		{
			this.trail = trail;
		}

		void update()
		{
			for (int i = oldpos.length - 1; i > 0; i--)
				oldpos[i] = oldpos[i - 1];
			oldpos[0] = pos.clone();
			pos.addSelf(disp.div(disp.norm()).mult(min(disp.norm(), t)));

			// pos.addSelf(disp);
			disp.clear();
		}

		void draw()
		{
			if (mass < newmass)
				mass += 4;

			if (ball) {
				fill(outerColor);
				ellipse(pos.x, pos.y, mass * (float) 1.5, mass * (float) 1.5);
				// fill(240,240,240);
				// ellipse(pos.x,pos.y,mass*(float)1.5,mass*(float)1.5);
				fill(innerColor);
				ellipse(pos.x, pos.y, mass, mass);
			}
		}

		void costrain(float x0, float x1, float y0, float y1)
		{
			pos.x = min(x1, max(x0, pos.x));
			pos.y = min(y1, max(y0, pos.y));
		}

		public String toString()
		{
			return pos + "";
		}
	}

	/**
	 * This inner class represents an arc in the graph.
	 * 
	 * @author cburnett
	 * 
	 */
	class Arc
	{
		Node a;
		Node b;
		boolean visible;
		boolean active;

		Arc(Node _s, Node _e)
		{
			a = _s;
			b = _e;
			this.visible = true;
			this.active = true;
		}

		Arc(Node _s, Node _e, boolean vis, boolean act)
		{
			a = _s;
			b = _e;
			this.visible = vis;
			this.active = act;
		}

		void draw()
		{
			// don't draw anything
		}
	}

	class OpinionArc extends Arc
	{
		Opinion arcOp;
		boolean marked;

		OpinionArc(Node _s, Node _e, Opinion op, boolean vis, boolean act, boolean marked)
		{
			super(_s, _e, vis, act);
			this.arcOp = op;
			this.marked = marked;
		}

		public void draw()
		{
			double opb = arcOp.getBelief();
			// double opd = arcOp.getDisbelief();
			double opu = arcOp.getUncertainty();

			// Set the colour the arc will be
			float transparency, r, g;
			transparency = (float) Math.round(200 * (1 - opu));
			if (opb > 0.5) {
				r = 1 - (float) opb;
				g = 1;
			} else {
				r = 1;
				g = 1 - (float) (0.5 - opb);
			}

			stroke(color(r * 200, g * 200, 0), transparency);
			if (marked)
				strokeWeight(4);
			else
				strokeWeight(1.3f);

			// stroke(0);
			line(a.pos.x, a.pos.y, b.pos.x, b.pos.y);
			strokeWeight(1);
			noStroke();
		}
	}

	class TeamCircle
	{
		public float x;
		public float y;
		public float rad;

		public List<Agent> team;

		public TeamCircle(float x, float y, float rad, List<Agent> team)
		{
			this.x = x;
			this.y = y;
			this.rad = rad;
			this.team = team;
		}
	}

	// buttons

	class Button
	{
		int x, y;
		int sizex, sizey;
		int basecolor, highlightcolor, oncolor;
		int currentcolor;
		boolean over = false;
		boolean pressed = false;

		void update()
		{
			if (over()) {
				currentcolor = highlightcolor;
			} else {
				if (pressed)
					currentcolor = oncolor;
				else
					currentcolor = basecolor;
			}
		}

		public void setPressed(boolean state)
		{
			pressed = state;
		}

		boolean pressed()
		{
			if (over) {
				pressed = !pressed;
				return true;
			} else {
				return false;
			}
		}

		boolean over()
		{
			return true;
		}

		boolean overRect(int x, int y, int width, int height)
		{
			if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
				return true;
			} else {
				return false;
			}
		}

		boolean overCircle(int x, int y, int diameter)
		{
			float disX = x - mouseX;
			float disY = y - mouseY;
			if (sqrt(sq(disX) + sq(disY)) < diameter / 2) {
				return true;
			} else {
				return false;
			}
		}
	}

	class RectButton extends Button
	{

		String label;

		RectButton(String labelText, int ix, int iy, int isizex, int isizey, int icolor, int ihighlight, int ion)
		{
			x = ix;
			y = iy;
			sizex = isizex;
			sizey = isizey;
			basecolor = icolor;
			highlightcolor = ihighlight;
			oncolor = ion;
			currentcolor = basecolor;
			label = labelText;
		}

		boolean over()
		{
			if (overRect(x, y, sizex, sizey)) {
				over = true;
				return true;
			} else {
				over = false;
				return false;
			}
		}

		void display()
		{
			stroke(10);
			textAlign(CENTER, CENTER);
			textFont(medium);
			fill(currentcolor);
			rect(x, y, sizex, sizey);
			fill(0);
			text(label, x + (sizex / 2), y + sizey - (sizey / 2));

		}
	}

}
