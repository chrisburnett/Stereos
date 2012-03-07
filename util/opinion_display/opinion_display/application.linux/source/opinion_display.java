import processing.core.*; 
import processing.xml.*; 

import processing.pdf.*; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class opinion_display extends PApplet {



// geometry constants
int XSIZE = 400;
int YSIZE = 400;
int PAD = 50;
int D_LOC_X = PAD;
int B_LOC_X = XSIZE-PAD;
int X_DIST = B_LOC_X - D_LOC_X;
int Y_DIST = (int)sqrt((X_DIST*X_DIST)-((X_DIST/2)*(X_DIST/2)));
int B_LOC_Y = PAD+Y_DIST;
int D_LOC_Y = PAD+Y_DIST;
int U_LOC_X = PAD+(X_DIST/2);
int U_LOC_Y = PAD;
int OPACITY = 80;
int HOTSPOT_RAD = 4;
int LABEL_PAD_X = 10;
int LABEL_PAD_Y = 10;
int SELECTED_X, SELECTED_Y;

String DATAFILE;
String[] OPINION_LISTS;
String[] CURRENT_OPINIONS;

double[][] B_MAP;
double[][] D_MAP;
double[][] U_MAP;
int[][] AGENTID_MAP;

// an array to map agents to profile codes, for colouring
int TRUSTEEPROFILE_MAP[];
int TRUSTORPROFILE_MAP[];

// define some colours here for the profile colouring code
int[] PROFILE_COLOUR;
// profiles to be 'highlighted' in a B&W compatible way
int circleHighlightProfile;
int crossHighlightProfile;

String SELECTED_OPINION;

// details of the currently selected (mouse over) agent's profile
String SEL_PROFILEID;
String SEL_MEAN;
String SEL_VAR;

// details of the currently viewed agent's profile
String CA_PROFILEID;
String CA_MEAN;
String CA_VAR;
// string for displaying it on screen
String CA_PROFILESTRING;

int currentAgent = 0;
int agentCount;
int trustorCount;

int MODE;
boolean reset;
boolean recording;

// fonts
PFont font,tiny,medium;

public void setup() {
  smooth();
  
  // initial profiles to be highlighted
  circleHighlightProfile=0;
  crossHighlightProfile=1;
  
  //size(XSIZE,YSIZE,PDF,"output.pdf");
  size(XSIZE,YSIZE);
  hint(ENABLE_NATIVE_FONTS);
  font = createFont("Arial",20);
  medium = createFont("Arial",14);
  tiny = createFont("Arial",14);
  textAlign(CENTER,TOP);
  fill(0,0,0);
  
  // open the file selection dialogue
  readData();
  
  // find out how many agents there are
  agentCount = OPINION_LISTS.length-1;
  // and trustors are columns
  trustorCount = (OPINION_LISTS[0].split(",").length)-1;
  SELECTED_OPINION = "";
  SELECTED_X = 40;
  SELECTED_Y = 40;
  // display mode (opinion bases of trustors =1, or opinions about trustees=0) (essentially display columns or rows resp.)
  MODE = 0;
  reset=false;
  recording=false;
  // maps for point locations to data
  B_MAP = new double[XSIZE][YSIZE];
  D_MAP = new double[XSIZE][YSIZE];
  U_MAP = new double[XSIZE][YSIZE];
  AGENTID_MAP = new int[XSIZE][YSIZE];

  CA_PROFILESTRING="";
  TRUSTEEPROFILE_MAP = new int[agentCount];
  TRUSTORPROFILE_MAP = new int[trustorCount];
  
  // colour definitions for profiles
  PROFILE_COLOUR = new int[5];
  PROFILE_COLOUR[0] = 0xffB70000; // red
  PROFILE_COLOUR[3] = 0xff1700B7; // blue
  PROFILE_COLOUR[2] = 0xffB40297; // purple
  PROFILE_COLOUR[1] = 0xff2DB405; // green
  PROFILE_COLOUR[4] = 0xffE38800; // green

}

public void readData()
{
  DATAFILE = selectInput();
  OPINION_LISTS = loadStrings(DATAFILE);
}

public String[] getTrusteeProfileDetails(int agentId)
{
   String profileDetails = OPINION_LISTS[agentId].split(",")[trustorCount];
   String[] parts = profileDetails.substring(1,profileDetails.length()-1).split(":");
   return parts;
}

public String[] getTrustorProfileDetails(int agentId)
{
   String profileDetails = OPINION_LISTS[OPINION_LISTS.length-1].split(",")[agentId];
   String[] parts = profileDetails.substring(1,profileDetails.length()-1).split(":");
   return parts;
}

public void draw() 
{
  reset = false; 
  background(255);
  // instructions
  textFont(tiny);
  fill(0);
  text("Up/Down: Switch mode        Left/Right: Switch agent", XSIZE/2, YSIZE-20);
  textFont(font);
  // get proper text description
  String modeDescription = (MODE == 1) ? "Opinion space for agent " : "Opinions about agent ";
  text(modeDescription+(currentAgent+1),XSIZE/2,B_LOC_Y+(0.5f*PAD));
  textFont(medium);
  text(CA_PROFILESTRING,XSIZE/2,YSIZE-40);
  
   
  if(recording)
  {
    beginRecord(PDF, "frame-####.pdf"); 

  }
  

  //text("a=0.5",XSIZE/2,B_LOC_Y+10);
  fill(255,255,255);
  stroke(0,80);
  strokeWeight(2);
  triangle(D_LOC_X,D_LOC_Y,U_LOC_X,U_LOC_Y,B_LOC_X,B_LOC_Y);
  
  // draw the example opinion projectors
  //line(U_LOC_X,U_LOC_Y,XSIZE/2,B_LOC_Y);
  //line(U_LOC_X,U_LOC_Y,XSIZE/2+(0.15*X_DIST),B_LOC_Y);
  //line(U_LOC_X,U_LOC_Y,XSIZE/2,B_LOC_Y);
  
  fill(0);
  text(SELECTED_OPINION, SELECTED_X, SELECTED_Y);
  // display current trustors's opinion space or opinions about current agent, depending on mode
  //CURRENT_OPINIONS = OPINION_LISTS[currentAgent].split(",");
  CA_PROFILESTRING="";
  String[] ca_profile;
  // if mode == 1, then we want to display the opinion space of a trustor (cols)
  // as such, it should be the length of the number of trustees (agentCount)
  if(MODE == 1)
  {
    String[] opBase = new String[agentCount];
    // for each trustee, get the opinion from the selected trustor
    for(int i=0; i < OPINION_LISTS.length-1; i++) {
        opBase[i] = OPINION_LISTS[i].split(",")[currentAgent];
    }
    CURRENT_OPINIONS = opBase;
    // get profile details for the current trustee
    ca_profile = getTrustorProfileDetails(currentAgent);
    for(int i = 0; i<CURRENT_OPINIONS.length-1; i++)
      TRUSTEEPROFILE_MAP[i] = Integer.parseInt((getTrusteeProfileDetails(i)[0]).substring(1))-1;
  } else {
    // else if mode==0 then we want the opinions ABOUT a single trustee (rows)
    // current opinions is just the currently selected row
    CURRENT_OPINIONS = OPINION_LISTS[currentAgent].split(",");
    
    // get profile details for the current trustee
    ca_profile = getTrusteeProfileDetails(currentAgent);

    for(int i = 0; i<CURRENT_OPINIONS.length-1; i++)
      TRUSTORPROFILE_MAP[i] = Integer.parseInt((getTrustorProfileDetails(i)[0]).substring(1))-1;
  }
  
  // description string for the currently selected agent's profile
  CA_PROFILEID = ca_profile[0];
  CA_MEAN = ca_profile[1];
  CA_VAR = ca_profile[2];
  CA_PROFILESTRING = "Profile id: "+CA_PROFILEID+"     Mean: "+CA_MEAN+"     Variance: "+CA_VAR;
  
  // parse and plot each opinion
  stroke(70);
  strokeWeight(1);
  for(int i = 0; i < CURRENT_OPINIONS.length-1; i++)  // length-1??!!
  {
    if(validAgent(currentAgent, MODE)){
      String[] op_parts = CURRENT_OPINIONS[i].substring(1,CURRENT_OPINIONS[i].length()-1).split(":");
      double b = Double.parseDouble(op_parts[0]);
      double d = Double.parseDouble(op_parts[1]);
      double u = Double.parseDouble(op_parts[2]);

      // triple to double transform
      double x = (b+(u/2));
      double y = u;

      // calculate position in triangle
      double adjustedX = ((x*X_DIST)+PAD);
      double adjustedY = (D_LOC_Y-(y*Y_DIST));
      //ellipse((float)adjustedX,(float)adjustedY,5,5);
      fill(0xff1700B7, OPACITY);
      stroke(0xff1700B7, OPACITY);
      //text("<"+b+","+d+","+u+">", (float)adjustedX,(float)adjustedY);
      //text(Integer.toString(i), (float)adjustedX, (float)adjustedY);

      // we need to store the screen values along with the b,d,u and trustee id
      for(int k = -(HOTSPOT_RAD); k <= HOTSPOT_RAD; k++)
      {
        for(int j = -(HOTSPOT_RAD); j <= HOTSPOT_RAD; j++)
        {
          B_MAP[(int)adjustedX+k][(int)adjustedY+j] = b;
          D_MAP[(int)adjustedX+k][(int)adjustedY+j] = d;
          U_MAP[(int)adjustedX+k][(int)adjustedY+j] = u;
          AGENTID_MAP[(int)adjustedX+k][(int)adjustedY+j] = i+1;
        }
      }
      
      // change colour of i according to profile, if appropriate
      int currentProfile;
      
      if(!reset) {
        currentProfile = getProfileMap()[i];
        // set colors
        fill(PROFILE_COLOUR[currentProfile], OPACITY);
        stroke(PROFILE_COLOUR[currentProfile], OPACITY);
        //ellipse((float)((x*X_DIST)+PAD), (float)(B_LOC_Y-(y*Y_DIST)),5,5);
  
        
        // highlight with marks
        if(currentProfile == circleHighlightProfile)
        {
           ellipse((int)adjustedX, (int)adjustedY,HOTSPOT_RAD+1,HOTSPOT_RAD+1);
        }
        else if(currentProfile == crossHighlightProfile)
        {
          strokeWeight(1.5f);
          line((int)adjustedX - HOTSPOT_RAD/1.5f, (int)adjustedY + HOTSPOT_RAD/1.5f, (int)adjustedX + HOTSPOT_RAD/1.5f, (int)adjustedY - HOTSPOT_RAD/1.5f);
          line((int)adjustedX - HOTSPOT_RAD/1.5f, (int)adjustedY - HOTSPOT_RAD/1.5f, (int)adjustedX + HOTSPOT_RAD/1.5f, (int)adjustedY + HOTSPOT_RAD/1.5f);
        }
      }
      
    }
  }
  
  textFont(tiny);
  fill(0);
  text("d",D_LOC_X-LABEL_PAD_X,D_LOC_Y+LABEL_PAD_Y);
  text("b",B_LOC_X+LABEL_PAD_X,D_LOC_Y+LABEL_PAD_Y);
  text("u",U_LOC_X-LABEL_PAD_X/2,U_LOC_Y-LABEL_PAD_Y);
  
  if(recording)
  {
    recording = false;

    endRecord();
  }
  //exit(); - for pdf?
}

public int[] getProfileMap()
{
  // change colour of i according to profile, if appropriate
  int[] profileMap;
  if(MODE==0)
    return TRUSTORPROFILE_MAP;
  else
    return TRUSTEEPROFILE_MAP;
}

public boolean isLeft(KeyEvent e) { 
  return e.getKeyCode()==KeyEvent.VK_LEFT || e.getKeyCode()==KeyEvent.VK_KP_LEFT; 
}
public boolean isRight(KeyEvent e) { 
  return e.getKeyCode()==KeyEvent.VK_RIGHT || e.getKeyCode()==KeyEvent.VK_KP_RIGHT; 
}
public boolean isUp(KeyEvent e) { 
  return e.getKeyCode()==KeyEvent.VK_UP || e.getKeyCode()==KeyEvent.VK_KP_UP; 
}
public boolean isDown(KeyEvent e) { 
  return e.getKeyCode()==KeyEvent.VK_DOWN || e.getKeyCode()==KeyEvent.VK_KP_DOWN; 
}
//boolean isR(KeyEvent e) {
//  return e.getKeyCode()==KeyEvent.VK_R;
//}

public boolean validAgent(int i, int mode)
{
  if(mode == 1 && i < trustorCount-1) return true;
  // -1 because the last 'trustor' column is actually a profile description
  if(mode == 0 && i < agentCount-1) return true;
  else return false;
}

public void keyPressed(KeyEvent e) {
  if (isLeft(e) && currentAgent > 0)
    currentAgent--; 
  else if (isRight(e) && validAgent(currentAgent, MODE))
    currentAgent++;
  else if (isUp(e) && MODE != 1)
  {
    currentAgent=0;
    MODE = 1;
    reset=true;
  }
  else if (isDown(e) && MODE != 0) 
  {
    currentAgent=0;
    MODE = 0;
    reset=true;
  }
  else if (key == 'r')
  {
    recording = true;
  }
  
  else if (e.getKeyCode()==KeyEvent.VK_L)
  {  
    readData();
  }
   
  // clear the maps
  B_MAP = new double[XSIZE][YSIZE];
  D_MAP = new double[XSIZE][YSIZE];
  U_MAP = new double[XSIZE][YSIZE];
  AGENTID_MAP = new int[XSIZE][YSIZE];
}

public void mouseMoved() {
  //mouseX and mouseY give mouse coordinates. Need to map these
  int id = AGENTID_MAP[mouseX][mouseY];
  if(id != 0)
  {
    double b = B_MAP[mouseX][mouseY];
    double d = D_MAP[mouseX][mouseY];
    double u = U_MAP[mouseX][mouseY];

    double factor = 1e2f; // = 1 * 10^5 = 100000.
    double rb = Math.round(b * factor) / factor;
    double rd = Math.round(d * factor) / factor;
    double ru = Math.round(u * factor) / factor;

    SELECTED_OPINION = "Agent "+id+"\nB:"+rb+"\nD:"+rd+"\nU:"+ru;
  }
}











  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#FFFFFF", "opinion_display" });
  }
}
