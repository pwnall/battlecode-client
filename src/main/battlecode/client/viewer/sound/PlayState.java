package battlecode.client.viewer.sound;

import battlecode.common.*;
import battlecode.serial.*;
import battlecode.client.viewer.*;
import battlecode.world.signal.*;

import java.util.*;

public class PlayState extends GameState {


    private static class Factory implements GameStateFactory<PlayState> {

        public PlayState createState(MatchHeader header) {
            return new PlayState(header);
        }

        public PlayState cloneState(PlayState state) {
            return new PlayState(state);
        }

        public void copyState(PlayState src, PlayState dst) {
            dst.copyStateFrom(src);
        }
    }
    public static final GameStateFactory<PlayState> FACTORY = new Factory();

    private static class EMPInfo {

        public double energon = 100;
        public int evolveTenure = 0;
    }
    private Set<GameSoundBank.ClipGroup> activeClips;
    private Set<GameSoundBank.ClipGroup> futureClips;
    private Map<Integer, RobotType> robotTypes;
    private int intensityLevel = 0;
    

    public PlayState() {
        activeClips = new HashSet<GameSoundBank.ClipGroup>();
        futureClips = new HashSet<GameSoundBank.ClipGroup>();
        robotTypes = new HashMap<Integer, RobotType>();
      
    }

    public PlayState(MatchHeader header) {
        this();
    }

    private PlayState(PlayState clone) {
        this();
        copyStateFrom(clone);
    }

    private synchronized void copyStateFrom(PlayState src) {
    	this.intensityLevel = src.intensityLevel;
    	//System.out.println("Copied");
    }

    private void scheduleClip(GameSoundBank.ClipGroup clip) {
        futureClips.add(clip);
    }

    private void scheduleClip(GameSoundBank.ClipGroup clip, float gain) {
        scheduleClip(clip);
        clip.setGain(gain);
    }

    private void scheduleClip(GameSoundBank.ClipGroup clip, float minGain, float maxGain) {
        scheduleClip(clip, ((float) Math.random()) * (maxGain - minGain) + minGain);
    }

    public Void visitAttackSignal(AttackSignal s) {
        try {
            switch (robotTypes.get(s.getRobotID())) {
                /*case WOUT2XXX:
                    scheduleClip(GameSoundBank.ATTACK2, -10, 0);
                    break;*/
                case TURRET:
                    scheduleClip(GameSoundBank.MORTAR, -2, 0);
                    break;
                default:
                    scheduleClip(GameSoundBank.ATTACK, -5, 0);
            }
        } catch (NullPointerException e) {
        }
        return null;
    }

    public Void visitBroadcastSignal(BroadcastSignal s) {
        return null;
    }

    public Void visitDeathSignal(DeathSignal s) {
        scheduleClip(GameSoundBank.DEATH);
    	//System.out.println("Death: " + robotTypes.get(s.getObjectID()) + " " + RobotType.ARCHON);
    
    	if(robotTypes.get(s.getObjectID()) == RobotType.ARCHON){
    		intensityLevel += 100;
    	}
        return null;
    }

    public Void visitEnergonChangeSignal(EnergonChangeSignal s) {
        return null;
    }

    public Void visitEnergonTransferSignal(EnergonTransferSignal s) {
        return null;
    }

    public Void visitEvolutionSignal(EvolutionSignal s) {
        robotTypes.put(s.getRobotID(), s.getType());
        return null;
    }

    public Void visitIndicatorStringSignal(IndicatorStringSignal s) {
        return null;
    }

    public Void visitMatchObservationSignal(MatchObservationSignal s) {
        return null;
    }

    public Void visitControlBitsSignal(ControlBitsSignal s) {
        return null;
    }

    public Void visitMovementOverrideSignal(MovementOverrideSignal s) {
        return null;
    }

    public Void visitMovementSignal(MovementSignal s) {
        return null;
    }

    public Void visitSetDirectionSignal(SetDirectionSignal s) {
        return null;
    }

    public Void visitAwesomenessSignal(AwesomenessSignal s) {
        return null;
    }

    public Void visitSpawnSignal(SpawnSignal s) {
        scheduleClip(GameSoundBank.SNIPE, -10.f);
    	//System.out.println( s.getType());
        robotTypes.put(s.getRobotID(), s.getType());
        return null;
    }

    public Void visitBytecodesUsedSignal(BytecodesUsedSignal s) {
        return null;
    }

    protected void updateRound() {
        activeClips.clear();
        Set<GameSoundBank.ClipGroup> tmp = activeClips;
        activeClips = futureClips;
        futureClips = tmp;
    }

    public void play() {
    	if(intensityLevel > 100){
    		
    		if(intensityLevel > 400){
    			AmbientPlayer.playAmbient(3);
    		}
    		else{
    			AmbientPlayer.playAmbient(2);
    		}
    	}
    	else{
    		AmbientPlayer.playAmbient(1);
    	}
    	
    	
        for (GameSoundBank.ClipGroup clipGroup : activeClips) {
            clipGroup.play();
        }
        activeClips.clear();
    }


    public Void visitMapOriginSignal(MapOriginSignal s) {
        return null;
    }

    public Void visitDeploySignal(DeploySignal s) {
        return null;//FIXME I AM NOT IMPLEMENTED!
    }

    public Void visitLightningShieldSignal(LightningShieldSignal s) {
        return null;//FIXME I AM NOT IMPLEMENTED!
    }
}